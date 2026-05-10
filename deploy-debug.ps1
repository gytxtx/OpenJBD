$ErrorActionPreference = "Stop"

$gradle = "$env:USERPROFILE\.gradle\wrapper\dists\gradle-9.2.1-bin\2t0n5ozlw9xmuyvbp7dnzaxug\gradle-9.2.1\bin\gradle.bat"

if (-not (Test-Path $gradle)) {
    throw "Gradle wrapper runtime not found: $gradle"
}

& $gradle deployDebug --offline
