#Requires -Version 5.1
param(
    [string]$OutDir
)

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
if (-not $OutDir) {
    $OutDir = Join-Path $root 'config\nginx'
}

function Write-Step { param($m) Write-Host "==> $m" -ForegroundColor Cyan }
function Write-Ok   { param($m) Write-Host "    $m"  -ForegroundColor Green }
function Write-Note { param($m) Write-Host "    $m"  -ForegroundColor Yellow }

function Read-DotEnv {
    param([string]$Path)

    $values = @{}
    if (-not (Test-Path $Path)) { return $values }

    foreach ($line in Get-Content $Path -Encoding UTF8) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith('#')) { continue }
        $eq = $trimmed.IndexOf('=')
        if ($eq -lt 1) { continue }
        $name = $trimmed.Substring(0, $eq).Trim()
        $value = $trimmed.Substring($eq + 1).Trim()
        if ($value.Length -ge 2 -and $value.StartsWith('"') -and $value.EndsWith('"')) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        $values[$name] = $value
    }
    return $values
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

$envValues = Read-DotEnv (Join-Path $root '.env')
$user = $envValues['SWAGGER_USER']
$password = $envValues['SWAGGER_PASSWORD']

if (-not $user) { $user = 'swagger' }
if (-not $password) { $password = 'SwaggerManager2026!' }

if (-not (Test-Path $OutDir)) { New-Item -ItemType Directory -Path $OutDir -Force | Out-Null }
$target = Join-Path (Resolve-Path $OutDir).Path 'swagger.htpasswd'

Write-Step 'Пароль для Swagger UI'

if ((Test-Path $target) -and -not $envValues['SWAGGER_PASSWORD']) {
    Write-Ok 'уже есть, пропуск'
    exit 0
}

$openssl = Find-OpenSsl
if (-not $openssl) {
    Write-Note 'Не найден openssl. Установите Git for Windows или OpenSSL.'
    exit 1
}

$hash = & $openssl passwd -apr1 $password 2>&1
if ($LASTEXITCODE -ne 0 -or -not $hash) {
    Write-Note 'openssl passwd завершился с ошибкой'
    $hash | ForEach-Object { Write-Note $_ }
    exit 1
}

$line = $user + ':' + $hash.Trim() + "`n"
[System.IO.File]::WriteAllText($target, $line, (New-Object System.Text.UTF8Encoding($false)))
Write-Ok $target
