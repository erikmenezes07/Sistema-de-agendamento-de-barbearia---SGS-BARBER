$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$envFile = Join-Path $root ".env"
if (!(Test-Path $envFile)) { throw "Copie .env.example para .env e configure o banco." }
foreach ($line in Get-Content $envFile -Encoding UTF8) {
    $line = $line.Trim()
    if (!$line -or $line.StartsWith("#")) { continue }
    $parts = $line -split "=", 2
    if ($parts.Count -ne 2 -or $parts[0].Trim() -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') {
        throw "Linha inválida no .env. Use NOME=valor sem comandos PowerShell."
    }
    $value = $parts[1].Trim()
    if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
        $value = $value.Substring(1, $value.Length - 2)
    }
    [Environment]::SetEnvironmentVariable($parts[0].Trim(), $value, "Process")
}
Push-Location (Join-Path $root "backend")
try {
    & .\mvnw.cmd spring-boot:run
    if ($LASTEXITCODE -ne 0) { throw "O backend terminou com erro ($LASTEXITCODE)." }
} finally { Pop-Location }
