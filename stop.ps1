#Requires -Version 5.1
<#
    Остановка стенда RecipeManagementSystem.

    Останавливает приложения, запущенные через .\start.ps1 (по сохранённым PID),
    а также освобождает порты 8080/8081/8083, если их занял старый процесс.

    Параметры:
      -Docker   также остановить Keycloak, базы данных и HTTPS-прокси (docker compose stop)
                Данные в томах сохраняются.
#>
param(
    [switch]$Docker
)

$ErrorActionPreference = 'Continue'

$root   = Split-Path -Parent $MyInvocation.MyCommand.Path
$logDir = Join-Path $root 'logs'
$pidFile = Join-Path $logDir 'pids.txt'

function Write-Step { param($m) Write-Host "==> $m" -ForegroundColor Cyan }
function Write-Ok   { param($m) Write-Host "    $m"  -ForegroundColor Green }
function Write-Note { param($m) Write-Host "    $m"  -ForegroundColor Yellow }

# --------------------------------------------------------------------------
# 1. Приложения из pid-файла
# --------------------------------------------------------------------------
$killed = @{}

function Stop-Tree {
    param([int]$ProcessId)

    # Дочерние процессы останавливаем первыми, иначе родитель может оставить
    # их висящими (например, Maven порождает отдельную JVM).
    $children = Get-CimInstance -ClassName Win32_Process -Filter "ParentProcessId = $ProcessId" -ErrorAction SilentlyContinue
    foreach ($child in $children) {
        Stop-Tree -ProcessId ([int]$child.ProcessId)
    }

    $proc = Get-Process -Id $ProcessId -ErrorAction SilentlyContinue
    if ($proc) {
        Stop-Process -Id $ProcessId -Force -ErrorAction SilentlyContinue
        return $true
    }
    return $false
}

if (Test-Path $pidFile) {
    Write-Step 'Остановка приложений из logs\pids.txt'
    foreach ($line in Get-Content $pidFile) {
        $parts = $line.Split('|')
        if ($parts.Count -lt 3) { continue }
        $procId = [int]$parts[0]
        $name   = $parts[1]

        if ($killed.ContainsKey($procId)) { continue }

        if (Stop-Tree -ProcessId $procId) {
            Write-Ok ('остановлен ' + $name + ' (PID ' + $procId + ')')
        }
        else {
            Write-Note ($name + ' уже не работал (PID ' + $procId + ')')
        }
        $killed[$procId] = $true
    }
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
}
else {
    Write-Note 'logs\pids.txt не найден — приложения запускались не через start.ps1'
}

# --------------------------------------------------------------------------
# 2. Подстраховка: освободить порты, если их держит наш jar
# --------------------------------------------------------------------------
# Трогаем только процессы, которые запущены именно из этого проекта
# (в командной строке есть путь к target\<модуль>-...jar). Запуск из IDE
# или чужой java на этих портах остаётся нетронутым.
Write-Step 'Освобождение портов 8080 / 8081 / 8083'
$rootPattern = [regex]::Escape($root)
foreach ($port in 8080, 8081, 8083) {
    $conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if (-not $conns) { Write-Ok ('порт ' + $port + ' свободен'); continue }

    $freed = $false
    foreach ($procId in ($conns | Select-Object -ExpandProperty OwningProcess -Unique)) {
        if ($killed.ContainsKey($procId)) { $freed = $true; continue }

        $info = Get-CimInstance -ClassName Win32_Process -Filter "ProcessId = $procId" -ErrorAction SilentlyContinue
        $cmdLine = if ($info -and $info.CommandLine) { $info.CommandLine } else { '' }

        if ($info -and $info.Name -eq 'java.exe' -and $cmdLine -match $rootPattern) {
            if (Stop-Tree -ProcessId $procId) {
                Write-Ok ('порт ' + $port + ': остановлен ' + $info.Name + ' из проекта (PID ' + $procId + ')')
            }
            $freed = $true
        }
        else {
            Write-Note ('порт ' + $port + ' занят процессом ' + $info.Name + ' (PID ' + $procId + ') — не трогаю')
        }
    }

    if (-not $freed) { Write-Note ('порт ' + $port + ' остался занят') }
}

# --------------------------------------------------------------------------
# 3. Docker (по запросу)
# --------------------------------------------------------------------------
if ($Docker) {
    Write-Step 'Остановка контейнеров (данные сохраняются)'
    Push-Location $root
    try {
        docker compose stop 2>&1 | Out-Null
        Write-Ok 'Keycloak, базы данных и HTTPS-прокси остановлены'
        Write-Note 'Поднять снова: docker compose up -d'
    }
    finally { Pop-Location }
}

Write-Host ''
Write-Step 'Готово'
