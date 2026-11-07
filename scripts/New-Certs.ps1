#Requires -Version 5.1
param(
    [string]$OutDir
)

$ErrorActionPreference = 'Stop'

if (-not $OutDir) {
    $OutDir = Join-Path (Split-Path -Parent $PSScriptRoot) 'config\certs'
}

function Write-Step { param($m) Write-Host "==> $m" -ForegroundColor Cyan }
function Write-Ok   { param($m) Write-Host "    $m"  -ForegroundColor Green }
function Write-Note { param($m) Write-Host "    $m"  -ForegroundColor Yellow }

function Invoke-External {
    param([scriptblock]$Command)

    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $output = & $Command 2>&1 | Out-String
        $code = $LASTEXITCODE
        return [pscustomobject]@{ ExitCode = $code; Output = $output }
    }
    finally {
        $ErrorActionPreference = $previous
    }
}

function Find-OpenSsl {
    $candidates = @()
    $cmd = Get-Command openssl -ErrorAction SilentlyContinue
    if ($cmd) { $candidates += $cmd.Source }
    $candidates += 'C:\Program Files\Git\usr\bin\openssl.exe'
    $candidates += 'C:\Program Files\OpenSSL-Win64\bin\openssl.exe'
    foreach ($c in $candidates) {
        if ($c -and (Test-Path $c)) { return $c }
    }
    return $null
}

function Find-KeyTool {
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += (Join-Path $env:JAVA_HOME 'bin\keytool.exe') }
    if (Test-Path 'C:\Program Files\Java') {
        $candidates += Get-ChildItem -Path 'C:\Program Files\Java' -Filter 'keytool.exe' -Recurse -Depth 2 -ErrorAction SilentlyContinue |
            Where-Object { $_.Directory.Parent.Name -match '^jdk' } |
            Sort-Object { $_.Directory.Parent.Name } -Descending |
            ForEach-Object { $_.FullName }
    }
    foreach ($c in $candidates) {
        if ($c -and (Test-Path $c)) { return $c }
    }
    return $null
}

$openssl = Find-OpenSsl
if (-not $openssl) {
    Write-Note 'Не найден openssl. Установите Git for Windows или OpenSSL.'
    exit 1
}

$keytool = Find-KeyTool
if (-not $keytool) {
    Write-Note 'Не найден keytool. Установите JDK 21+ или задайте JAVA_HOME.'
    exit 1
}

if (-not (Test-Path $OutDir)) { New-Item -ItemType Directory -Path $OutDir -Force | Out-Null }
$OutDir = (Resolve-Path $OutDir).Path

$cert = Join-Path $OutDir 'server.crt'
$key = Join-Path $OutDir 'server.key'
$trust = Join-Path $OutDir 'truststore.p12'
$pass = 'changeit'

Write-Step 'Сертификат для localhost'
if ((Test-Path $cert) -and (Test-Path $key)) {
    Write-Ok 'уже есть, пропуск'
}
else {
    $r = Invoke-External { & $openssl req -x509 -newkey rsa:2048 -sha256 -days 825 -nodes -keyout $key -out $cert -subj '/CN=localhost' -addext 'subjectAltName=DNS:localhost,IP:127.0.0.1' }
    if ($r.ExitCode -ne 0 -or -not (Test-Path $cert)) {
        Write-Note 'openssl req завершился с ошибкой'
        $r.Output.Trim() | ForEach-Object { Write-Note $_ }
        exit 1
    }
    Write-Ok $cert
    Write-Ok $key
}

Write-Step 'Truststore для JVM'
if (Test-Path $trust) {
    Write-Ok 'уже есть, пропуск'
}
else {
    $javaHome = Split-Path -Parent (Split-Path -Parent $keytool)
    $cacerts = Join-Path $javaHome 'lib\security\cacerts'
    if (Test-Path $cacerts) {
        Copy-Item $cacerts $trust -Force
        $r = Invoke-External { & $keytool -importcert -noprompt -alias 'recipe-local' -file $cert -keystore $trust -storetype PKCS12 -storepass $pass }
    }
    else {
        $r = Invoke-External { & $keytool -importcert -noprompt -alias 'recipe-local' -file $cert -keystore $trust -storetype PKCS12 -storepass $pass }
    }
    if ($r.ExitCode -ne 0 -or -not (Test-Path $trust)) {
        Write-Note 'не удалось создать truststore'
        $r.Output.Trim() | ForEach-Object { Write-Note $_ }
        exit 1
    }
    Write-Ok $trust
}

Write-Host ''
Write-Ok ('каталог: ' + $OutDir)
