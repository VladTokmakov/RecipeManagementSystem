#Requires -Version 5.1
<#
    Запуск стенда RecipeManagementSystem.

    Поднимает всё, что нужно для работы сайта:
      1. Keycloak + две базы данных (docker compose)
      2. catalogue-service  (порт 8081) — источник данных
      3. manager-app        (порт 8080) — кабинет менеджера
      4. customer-app       (порт 8083) — кабинет покупателя

    Приложения запускаются как обычные фоновые процессы java -jar.
    Они НЕ привязаны к этому окну и продолжают работать после его закрытия.
    Остановить их: .\stop.ps1

    Примеры:
      .\start.ps1                 # обычный запуск
      .\start.ps1 -Rebuild        # пересобрать jar перед запуском
      .\start.ps1 -SkipDocker     # если контейнеры уже подняты
#>
param(
    [switch]$Rebuild,
    [switch]$SkipDocker
)

$ErrorActionPreference = 'Stop'

$root   = Split-Path -Parent $MyInvocation.MyCommand.Path
$logDir = Join-Path $root 'logs'
if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir | Out-Null }

function Write-Step { param($m) Write-Host "==> $m" -ForegroundColor Cyan }
function Write-Ok   { param($m) Write-Host "    $m"  -ForegroundColor Green }
function Write-Note { param($m) Write-Host "    $m"  -ForegroundColor Yellow }

# Внешние программы (docker, mvnw.cmd) пишут обычный прогресс в stderr.
# При $ErrorActionPreference = 'Stop' PowerShell считает любую такую строку
# фатальной ошибкой и обрывает скрипт. Поэтому на время вызова внешней
# программы переключаемся на 'Continue' и смотрим только на код возврата.
function Invoke-External {
    param(
        [scriptblock]$Command,
        [string]$WorkDir
    )

    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $pushed = $false
    try {
        if ($WorkDir) { Push-Location $WorkDir; $pushed = $true }
        $output = & $Command 2>&1 | Out-String
        $code = $LASTEXITCODE
        return [pscustomobject]@{ ExitCode = $code; Output = $output }
    }
    finally {
        if ($pushed) { Pop-Location }
        $ErrorActionPreference = $previous
    }
}

# --------------------------------------------------------------------------
# 0. Проверка .env
# --------------------------------------------------------------------------
Write-Step 'Проверка .env'
$envFile = Join-Path $root '.env'
if (-not (Test-Path $envFile)) {
    Copy-Item (Join-Path $root '.env.example') $envFile
    Write-Note 'Файла .env не было — создан из .env.example.'
    Write-Note 'Впишите в него реальные секреты Keycloak и запустите скрипт снова.'
    exit 1
}
Write-Ok '.env найден'

# --------------------------------------------------------------------------
# 0.1. Поиск настоящего java.exe
# --------------------------------------------------------------------------
# На Windows в PATH обычно лежит javapath\java.exe — это лаунчер-заглушка,
# который запускает второй процесс JVM. Из-за него на каждое приложение
# получается по два процесса, а PID в pids.txt не совпадает со слушающим.
# Поэтому запускаем настоящий java.exe из JDK напрямую.
Write-Step 'Поиск java.exe'

$javaCandidates = @()
if ($env:JAVA_HOME) { $javaCandidates += (Join-Path $env:JAVA_HOME 'bin\java.exe') }

$jdkRoot = 'C:\Program Files\Java'
if (Test-Path $jdkRoot) {
    $javaCandidates += Get-ChildItem -Path $jdkRoot -Filter 'java.exe' -Recurse -Depth 2 -ErrorAction SilentlyContinue |
        Where-Object { $_.Directory.Name -eq 'bin' -and $_.Directory.Parent.Name -match '^jdk' } |
        Sort-Object { $_.Directory.Parent.Name } -Descending |
        ForEach-Object { $_.FullName }
}

$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if ($javaCmd) { $javaCandidates += $javaCmd.Source }

$javaExe = $null
foreach ($candidate in $javaCandidates) {
    if ($candidate -and (Test-Path $candidate) -and ($candidate -notmatch '\\javapath\\')) {
        $javaExe = $candidate
        break
    }
}
if (-not $javaExe) {
    # Крайний случай: только лаунчер в PATH. Хуже, но лучше, чем ничего.
    if ($javaCmd) { $javaExe = $javaCmd.Source }
}
if (-not $javaExe -or -not (Test-Path $javaExe)) {
    Write-Note 'Не найден java.exe. Установите JDK 21+ или задайте переменную JAVA_HOME.'
    exit 1
}
Write-Ok $javaExe

# --------------------------------------------------------------------------
# 1. Docker: Keycloak + PostgreSQL
# --------------------------------------------------------------------------
if (-not $SkipDocker) {
    Write-Step 'Запуск Keycloak и баз данных (docker compose up -d)'
    $docker = Invoke-External -WorkDir $root -Command { docker compose up -d }
    if ($docker.ExitCode -ne 0) {
        Write-Note 'docker compose завершился с ошибкой. Проверьте, запущен ли Docker Desktop.'
        $docker.Output.Trim() | ForEach-Object { Write-Note $_ }
        exit 1
    }
    Write-Ok 'контейнеры запущены'
}

# --------------------------------------------------------------------------
# 2. Ожидание готовности Keycloak
# --------------------------------------------------------------------------
Write-Step 'Ожидание Keycloak (realm RecipeManagementSystem)'
$realmUrl = 'http://localhost:8082/realms/RecipeManagementSystem/.well-known/openid-configuration'
$ready = $false
for ($i = 0; $i -lt 90; $i++) {
    try {
        $resp = Invoke-WebRequest -Uri $realmUrl -UseBasicParsing -TimeoutSec 3
        if ($resp.StatusCode -eq 200) { $ready = $true; break }
    }
    catch { }
    Start-Sleep -Seconds 2
}
if (-not $ready) {
    Write-Note 'Keycloak не ответил за 3 минуты. Логи: docker logs RecipeManagementSystem-keycloak'
    exit 1
}
Write-Ok 'Keycloak готов'

# --------------------------------------------------------------------------
# 3. Сборка jar (если нужно)
# --------------------------------------------------------------------------
$apps = @(
    [pscustomobject]@{ Name = 'catalogue-service'; Dir = 'catalogue-service'; Profile = 'standalone'; Port = 8081 },
    [pscustomobject]@{ Name = 'manager-app';       Dir = 'manager-app';       Profile = 'standalone'; Port = 8080 },
    [pscustomobject]@{ Name = 'customer-app';      Dir = 'customer-app';      Profile = '';           Port = 8083 }
)

foreach ($app in $apps) {
    $dir = Join-Path $root $app.Dir
    $jar = Join-Path $dir ('target\' + $app.Name + '-0.0.1-SNAPSHOT.jar')
    $app | Add-Member -NotePropertyName Jar -NotePropertyValue $jar -Force

    # jar считается устаревшим, если исходники или ресурсы новее него.
    # Без этого легко запустить старую сборку и получить необъяснимые ошибки
    # входа (например, jar без spring.config.import не читает .env).
    $stale = $false
    if (Test-Path $jar) {
        $jarTime = (Get-Item $jar).LastWriteTimeUtc
        $newer = Get-ChildItem -Path (Join-Path $dir 'src') -Recurse -File -ErrorAction SilentlyContinue |
            Where-Object { $_.LastWriteTimeUtc -gt $jarTime } |
            Select-Object -First 1
        if ($newer) {
            $stale = $true
            Write-Note ($app.Name + ': исходники новее jar (' + $newer.Name + ')')
        }
    }

    if ($Rebuild -or (-not (Test-Path $jar)) -or $stale) {
        Write-Step ('Сборка ' + $app.Name)
        $build = Invoke-External -WorkDir $dir -Command { & .\mvnw.cmd -o -q package -DskipTests }
        $code = $build.ExitCode

        if ($code -ne 0 -or (-not (Test-Path $jar))) {
            Write-Note ('Не удалось собрать ' + $app.Name + '. Запустите вручную: cd ' + $app.Dir + '; .\mvnw.cmd -o package -DskipTests')
            ($build.Output -split "`r?`n" | Select-Object -Last 15) | ForEach-Object { Write-Note $_ }
            exit 1
        }
        Write-Ok 'собран'
    }
    else {
        Write-Ok ($app.Name + ': jar актуален')
    }
}

# --------------------------------------------------------------------------
# 4. Запуск приложений
# --------------------------------------------------------------------------
# catalogue-service поднимаем первым: веб-приложения ходят в него за данными.
$pidFile = Join-Path $logDir 'pids.txt'
if (Test-Path $pidFile) { Remove-Item $pidFile -Force }
$started = @()

foreach ($app in $apps) {
    Write-Step ('Запуск ' + $app.Name + ' (порт ' + $app.Port + ')')

    $busy = Get-NetTCPConnection -LocalPort $app.Port -State Listen -ErrorAction SilentlyContinue
    if ($busy) {
        Write-Note ('порт ' + $app.Port + ' уже занят — ' + $app.Name + ' пропущен')
        continue
    }

    # Рабочая директория = папка модуля: конфиги ищут .env по путям ./.env и ./../.env
    $appDir    = Join-Path $root $app.Dir
    $outLog    = Join-Path $logDir ($app.Name + '.log')
    $errLog    = Join-Path $logDir ($app.Name + '.err.log')

    $argList = @('-jar', $app.Jar)
    if ($app.Profile) { $argList += ('--spring.profiles.active=' + $app.Profile) }

    $proc = Start-Process -FilePath $javaExe `
                          -ArgumentList $argList `
                          -WorkingDirectory $appDir `
                          -RedirectStandardOutput $outLog `
                          -RedirectStandardError  $errLog `
                          -WindowStyle Hidden `
                          -PassThru

    $started += $app
    Write-Ok ('PID ' + $proc.Id + ', лог: logs\' + $app.Name + '.log')

    # Ждём, пока приложение займёт свой порт, и сохраняем PID того процесса,
    # который реально слушает порт (а не промежуточного лаунчера).
    $listenPid = $null
    for ($i = 0; $i -lt 120; $i++) {
        $conn = Get-NetTCPConnection -LocalPort $app.Port -State Listen -ErrorAction SilentlyContinue
        if ($conn) { $listenPid = ($conn | Select-Object -First 1).OwningProcess; break }
        if ($proc.HasExited) { break }
        Start-Sleep -Seconds 1
    }

    if ($listenPid) {
        Add-Content -Path $pidFile -Value ($listenPid.ToString() + '|' + $app.Name + '|' + $app.Port)
        if ($listenPid -ne $proc.Id) {
            # На случай цепочки лаунчеров: помним и родителя, чтобы stop.ps1 убрал всё.
            Add-Content -Path $pidFile -Value ($proc.Id.ToString() + '|' + $app.Name + '-launcher|' + $app.Port)
        }
        Write-Ok ('порт ' + $app.Port + ' слушает PID ' + $listenPid)
    }
    else {
        Add-Content -Path $pidFile -Value ($proc.Id.ToString() + '|' + $app.Name + '|' + $app.Port)
        Write-Note ($app.Name + ' не занял порт ' + $app.Port + '. Смотрите logs\' + $app.Name + '.err.log')
    }
}

# --------------------------------------------------------------------------
# 5. Итог
# --------------------------------------------------------------------------
Write-Host ''
Write-Step 'Готово'
foreach ($app in $apps) {
    $listening = Get-NetTCPConnection -LocalPort $app.Port -State Listen -ErrorAction SilentlyContinue
    $state = if ($listening) { 'работает' } else { 'не отвечает' }
    $color = if ($listening) { 'Green' } else { 'Red' }
    Write-Host ('    {0,-18} порт {1}  {2}' -f $app.Name, $app.Port, $state) -ForegroundColor $color
}

Write-Host ''
Write-Host '    Менеджер:  http://localhost:8080' -ForegroundColor White
Write-Host '    Покупатель: http://localhost:8083' -ForegroundColor White
Write-Host '    Swagger:   http://localhost:8081/swagger-ui.html' -ForegroundColor White
Write-Host '    Keycloak:  http://localhost:8082' -ForegroundColor White
Write-Host ''
Write-Host '    Остановить: .\stop.ps1' -ForegroundColor DarkGray
Write-Host '    Логи:       logs\' -ForegroundColor DarkGray
