$ErrorActionPreference = "Stop"

$gradle = Join-Path $PSScriptRoot "gradlew.bat"

if (-not (Test-Path -LiteralPath $gradle -PathType Leaf)) {
    throw "Gradle wrapper runtime not found: $gradle"
}

Push-Location $PSScriptRoot
try {
    & $gradle deployDebug --offline
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle deployDebug failed with exit code $LASTEXITCODE"
    }
}
finally {
    Pop-Location
}
