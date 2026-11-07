#Requires -Version 5.1
<#
    Доверие самоподписанному сертификату стенда RecipeManagementSystem.

    Добавляет config\certs\server.crt в хранилище «Доверенные корневые центры
    сертификации» текущего пользователя. После этого Edge, Chrome и встроенный
    браузер перестают ругаться на https://localhost.

    Почему CurrentUser, а не LocalMachine:
      - не требуются права администратора и UAC;
      - действует только для вашей учётной записи;
      - Edge/Chrome и встроенный браузер используют системное хранилище Windows.
    Firefox хранилище Windows не использует — ему нужен отдельный импорт.

    Windows покажет системное окно «Вы собираетесь установить сертификат...».
    Это защита ОС, её нельзя обойти скриптом: подтвердите один раз кнопкой «Да».

    Примеры:
      .\Trust-Certs.ps1                 # добавить сертификат
      .\Trust-Certs.ps1 -Remove         # убрать сертификат
      .\Trust-Certs.ps1 -Thumbprint     # только показать отпечаток и статус
#>
param(
    [switch]$Remove,
    [switch]$Thumbprint,
    [string]$CertPath,
    [ValidateSet('CurrentUser', 'LocalMachine')]
    [string]$StoreScope = 'CurrentUser'
)

$ErrorActionPreference = 'Stop'

function Write-Step { param($m) Write-Host "==> $m" -ForegroundColor Cyan }
function Write-Ok   { param($m) Write-Host "    $m"  -ForegroundColor Green }
function Write-Note { param($m) Write-Host "    $m"  -ForegroundColor Yellow }

# --------------------------------------------------------------------------
# 1. Путь к сертификату
# --------------------------------------------------------------------------
# По умолчанию берём сертификат стенда рядом со скриптом (scripts\..\config\certs).
if (-not $CertPath) {
    $CertPath = Join-Path (Split-Path -Parent $PSScriptRoot) 'config\certs\server.crt'
}

Write-Step 'Поиск сертификата'
if (-not (Test-Path $CertPath)) {
    Write-Note "Файл не найден: $CertPath"
    Write-Note 'Создайте сертификаты: powershell -NoProfile -File .\scripts\New-Certs.ps1'
    exit 1
}
$CertPath = (Resolve-Path $CertPath).Path
Write-Ok $CertPath

# --------------------------------------------------------------------------
# 2. Разбор сертификата и проверка пригодности
# --------------------------------------------------------------------------
$cert = New-Object System.Security.Cryptography.X509Certificates.X509Certificate2($CertPath)

Write-Step 'Параметры сертификата'
Write-Ok ("Subject  : " + $cert.Subject)
Write-Ok ("Issuer   : " + $cert.Issuer)
Write-Ok ("Действует: до " + $cert.NotAfter.ToString('dd.MM.yyyy'))
Write-Ok ("Отпечаток: " + $cert.Thumbprint)
Write-Ok ("SelfSigned: " + ($cert.Subject -eq $cert.Issuer))

if ($cert.NotAfter -lt (Get-Date)) {
    Write-Note 'Срок действия сертификата истёк — сначала пересоздайте его.'
    exit 1
}

# Доверять имеет смысл только самоподписанному сертификату: корневой ЦС
# подписывает сам себя. Для сертификата от настоящего УЦ это было бы вредно.
if ($cert.Subject -ne $cert.Issuer) {
    Write-Note 'Сертификат подписан другим центром сертификации — доверять ему как корневому не нужно.'
    exit 1
}

# Проверяем, что в SAN есть localhost: иначе браузер всё равно отклонит
# соединение, даже при доверенном сертификате.
$sanOk = $false
foreach ($ext in $cert.Extensions) {
    if ($ext.Oid.Value -eq '2.5.29.17') {
        $text = $ext.Format($false)
        if ($text -match 'localhost') { $sanOk = $true }
    }
}
if ($sanOk) { Write-Ok 'SAN      : localhost найден' }
else        { Write-Note 'SAN      : localhost НЕ найден — браузер отклонит адрес https://localhost' }

if ($Thumbprint) {
    Write-Host ''
    Write-Ok 'Режим -Thumbprint: изменения не вносятся.'
    exit 0
}

# --------------------------------------------------------------------------
# 3. Работа с хранилищем
# --------------------------------------------------------------------------
$storeName = [System.Security.Cryptography.X509Certificates.StoreName]::Root
$storeLoc  = [System.Security.Cryptography.X509Certificates.StoreLocation]::$StoreScope
$store = New-Object System.Security.Cryptography.X509Certificates.X509Store($storeName, $storeLoc)

try {
    # ReadWrite нужен и для удаления; для чтения хватило бы ReadOnly,
    # но лишняя проверка прав здесь не оправдана.
    $store.Open([System.Security.Cryptography.X509Certificates.OpenFlags]::ReadWrite)
}
catch {
    Write-Step 'Ошибка доступа к хранилищу'
    Write-Note $_.Exception.Message
    if ($StoreScope -eq 'LocalMachine') {
        Write-Note 'Для хранилища LocalMachine нужны права администратора. Запустите PowerShell от имени администратора'
        Write-Note 'или используйте значение по умолчанию: -StoreScope CurrentUser.'
    }
    exit 1
}

try {
    # Сравниваем по отпечатку, а не по файлу: X509Store.Remove ищет именно его.
    $existing = @($store.Certificates | Where-Object { $_.Thumbprint -eq $cert.Thumbprint })

    if ($Remove) {
        Write-Step 'Удаление сертификата из доверенных'
        if ($existing.Count -eq 0) {
            Write-Ok 'Сертификата в хранилище не было — изменений нет'
        }
        else {
            foreach ($item in $existing) { $store.Remove($item) }
            Write-Ok "Удалён: $($cert.Thumbprint)"
        }
    }
    else {
        Write-Step 'Добавление сертификата в доверенные'
        if ($existing.Count -gt 0) {
            Write-Ok 'Сертификат уже доверенный — пропуск'
        }
        else {
            Write-Note 'Сейчас Windows покажет окно подтверждения. Нажмите «Да».'
            $store.Add($cert)
            Write-Ok "Добавлен: $($cert.Thumbprint)"
        }
    }
}
finally {
    $store.Close()
}

# --------------------------------------------------------------------------
# 4. Проверка результата
# --------------------------------------------------------------------------
Write-Step 'Проверка'
$check = New-Object System.Security.Cryptography.X509Certificates.X509Store($storeName, $storeLoc)
$check.Open([System.Security.Cryptography.X509Certificates.OpenFlags]::ReadOnly)
try {
    $found = @($check.Certificates | Where-Object { $_.Thumbprint -eq $cert.Thumbprint })
}
finally {
    $check.Close()
}

if ($Remove) {
    if ($found.Count -eq 0) { Write-Ok 'Сертификата в хранилище нет — как и ожидалось' }
    else                    { Write-Note 'Сертификат всё ещё в хранилище' }
    exit 0
}

if ($found.Count -gt 0) {
    Write-Ok ("Доверенный сертификат на месте (" + $StoreScope + "\Root)")
    Write-Host ''
    Write-Host '    Проверьте в браузере: https://localhost/' -ForegroundColor White
    Write-Host '    Firefox использует своё хранилище — ему нужен отдельный импорт.' -ForegroundColor DarkGray
    exit 0
}
else {
    Write-Note 'Сертификат не появился в хранилище. Возможно, окно подтверждения было закрыто.'
    exit 1
}
