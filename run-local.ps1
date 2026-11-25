$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$envPath = Join-Path $PSScriptRoot '.env'

if (-not (Test-Path $envPath)) {
  throw 'Arquivo backend/java/.env não encontrado. Copie .env.example para .env e configure as variáveis.'
}

Get-Content $envPath | ForEach-Object {
  $line = $_.Trim()
  if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
    $parts = $line.Split('=', 2)
    $name = $parts[0].Trim()
    $value = $parts[1].Trim()
    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
  }
}

mvn spring-boot:run
