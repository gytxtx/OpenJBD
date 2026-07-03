$ErrorActionPreference = "Stop"

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$scriptPath = Join-Path $repositoryRoot "deploy-debug.ps1"
$script = Get-Content -Raw -LiteralPath $scriptPath

if ($script -notmatch '\$gradle\s*=\s*Join-Path\s+\$PSScriptRoot\s+"gradlew\.bat"') {
    throw "deploy-debug.ps1 must invoke gradlew.bat from PSScriptRoot"
}

if ($script -match '\.gradle[\\/]wrapper[\\/]dists') {
    throw "deploy-debug.ps1 must not depend on the user Gradle distribution cache"
}

if ($script -notmatch 'deployDebug') {
    throw "deploy-debug.ps1 must invoke the deployDebug task"
}

if ($script -notmatch '\$LASTEXITCODE') {
    throw "deploy-debug.ps1 must propagate a Gradle failure"
}
