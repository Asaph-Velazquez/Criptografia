# Requires only PowerShell and Docker Desktop with Compose v2 (Linux containers).
[CmdletBinding()]
param(
    [ValidateSet('up', 'down', 'logs', 'status', 'test')]
    [string]$Action = 'up',
    [ValidateRange(1, 65535)]
    [int]$Port = 8080
)

$ErrorActionPreference = 'Stop'

function Invoke-Docker {
    param([string[]]$DockerArgs)
    & docker @DockerArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Docker termino con codigo $LASTEXITCODE. Revisa la salida anterior."
    }
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Instala Docker Desktop con Docker Compose v2 y activa los contenedores Linux. No necesitas Java, Maven ni Node.js.'
}

$previousPort = $env:NEXO_PORT
Push-Location $PSScriptRoot
try {
    if ($PSBoundParameters.ContainsKey('Port') -or -not $env:NEXO_PORT) {
        $env:NEXO_PORT = "$Port"
    }
    Invoke-Docker -DockerArgs @('compose', 'version')
    $osType = Invoke-Docker -DockerArgs @('info', '--format', '{{.OSType}}')
    if (($osType -join '').Trim() -ne 'linux') {
        throw 'Activa el motor de contenedores Linux en Docker Desktop y vuelve a intentar.'
    }
    Invoke-Docker -DockerArgs @('compose', '--profile', 'test', 'config', '--quiet')
    switch ($Action) {
        'up' {
            Invoke-Docker -DockerArgs @('compose', 'up', '--build', '--detach', '--wait', '--wait-timeout', '180', 'nexo')
            Write-Host "Nexo listo en http://localhost:$env:NEXO_PORT"
        }
        'down' {
            Invoke-Docker -DockerArgs @('compose', '--profile', 'test', 'down', '--remove-orphans')
        }
        'logs' {
            Invoke-Docker -DockerArgs @('compose', 'logs', '--follow', '--tail', '100', 'nexo')
        }
        'status' {
            Invoke-Docker -DockerArgs @('compose', 'ps', '--all')
        }
        'test' {
            Invoke-Docker -DockerArgs @('compose', '--profile', 'test', 'run', '--build', '--rm', '--no-deps', 'frontend-tests')
            Invoke-Docker -DockerArgs @('compose', '--profile', 'test', 'run', '--build', '--rm', '--no-deps', 'backend-tests')
            Invoke-Docker -DockerArgs @('compose', 'up', '--build', '--detach', '--wait', '--wait-timeout', '180', 'nexo')
            Invoke-Docker -DockerArgs @('compose', '--profile', 'test', 'run', '--build', '--rm', '--no-deps', 'integration-tests')
            Write-Host 'Pruebas completadas. Nexo permanece activo; usa .\run.ps1 down para detenerlo.'
        }
    }
} finally {
    $env:NEXO_PORT = $previousPort
    Pop-Location
}
