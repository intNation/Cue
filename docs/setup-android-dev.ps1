Write-Host "Installing Git..."
winget install --id Git.Git -e --silent

Write-Host "Installing JDK 17..."
winget install --id EclipseAdoptium.Temurin.17.JDK -e --silent

Write-Host "Installing Android Studio..."
winget install --id Google.AndroidStudio -e --silent

Write-Host "Setting JAVA_HOME..."
$javaPath = (Get-Command java).Source | Split-Path | Split-Path
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", $javaPath, [System.EnvironmentVariableTarget]::Machine)

Write-Host "Done. Restart your PC before launching Android Studio."