<#
.SYNOPSIS
    Bootstraps a Windows development machine for the Cue Android project.

.DESCRIPTION
    Installs the project toolchain for the current Windows user, provisions the
    Android SDK packages required by this repository, creates local.properties,
    and optionally validates the checkout. Run this script from the Cue clone;
    it intentionally does not copy source code, Git credentials, Android Studio
    settings, or any other machine-specific data.

.EXAMPLE
    Set-ExecutionPolicy -Scope Process Bypass
    .\docs\setup-android-dev.ps1

.EXAMPLE
    .\docs\setup-android-dev.ps1 -ProjectRoot C:\Users\Student\Studio\Android\Cue
#>

[CmdletBinding()]
param(
    [string]$ProjectRoot = (Join-Path $PSScriptRoot ".."),
    [string]$RepositoryUrl = "https://github.com/intNation/Cue.git",
    [switch]$CloneIfMissing,
    [switch]$SkipVerification
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

# These are derived from the committed Gradle configuration.
$RequiredJavaMajor = 17
$RequiredPlatforms = @("platforms;android-36", "platforms;android-36.1")
$RequiredBuildTools = @("build-tools;36.0.0", "build-tools;36.1.0")
$RequiredSdkPackages = @(
    "platform-tools",
    "emulator",
    "system-images;android-36;google_apis;x86_64",
    "system-images;android-36.1;google_apis;x86_64"
) + $RequiredPlatforms + $RequiredBuildTools

function Write-Step([string]$Message) {
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Install-WingetPackage([string]$Id, [string]$Name) {
    Write-Step "Ensuring $Name is installed"
    & winget install --id $Id --exact --silent --disable-interactivity --accept-package-agreements --accept-source-agreements
    if ($LASTEXITCODE -ne 0) {
        throw "winget could not install $Name (package: $Id). Exit code: $LASTEXITCODE"
    }
}

function Get-Temurin17Home {
    $javaCandidates = @()

    $command = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($command) {
        $candidate = Split-Path (Split-Path $command.Source -Parent) -Parent
        if ($candidate -match "jdk-17") {
            $javaCandidates += $candidate
        }
    }

    $adoptiumRoot = Join-Path $env:ProgramFiles "Eclipse Adoptium"
    if (Test-Path $adoptiumRoot) {
        $javaCandidates += Get-ChildItem $adoptiumRoot -Directory -Filter "jdk-17*" |
            Sort-Object Name -Descending |
            ForEach-Object FullName
    }

    $javaHome = $javaCandidates |
        Where-Object { Test-Path (Join-Path $_ "bin\javac.exe") } |
        Select-Object -First 1

    if (-not $javaHome) {
        throw "A JDK $RequiredJavaMajor installation could not be found after installation."
    }

    return $javaHome
}

function Get-SdkManager([string]$SdkRoot) {
    $sdkManager = Join-Path $SdkRoot "cmdline-tools\latest\bin\sdkmanager.bat"
    if (Test-Path $sdkManager) {
        return $sdkManager
    }

    Write-Step "Installing Android SDK command-line tools"
    $toolsVersion = "13114758"
    $archive = Join-Path $env:TEMP "cue-commandlinetools-$toolsVersion.zip"
    $extractRoot = Join-Path $env:TEMP "cue-commandlinetools-$toolsVersion"
    $latestRoot = Join-Path $SdkRoot "cmdline-tools\latest"

    New-Item -ItemType Directory -Force -Path $extractRoot, $latestRoot | Out-Null
    Invoke-WebRequest -Uri "https://dl.google.com/android/repository/commandlinetools-win_${toolsVersion}_latest.zip" -OutFile $archive
    Expand-Archive -LiteralPath $archive -DestinationPath $extractRoot -Force
    Move-Item -Path (Join-Path $extractRoot "cmdline-tools\*") -Destination $latestRoot -Force

    if (-not (Test-Path $sdkManager)) {
        throw "Android SDK command-line tools were downloaded but sdkmanager.bat was not found."
    }

    return $sdkManager
}

function Set-UserEnvironmentVariable([string]$Name, [string]$Value) {
    [Environment]::SetEnvironmentVariable($Name, $Value, [EnvironmentVariableTarget]::User)
    Set-Item -Path "Env:$Name" -Value $Value
}

if (-not (Get-Command winget.exe -ErrorAction SilentlyContinue)) {
    throw "winget is required. Install/update App Installer from the Microsoft Store, then run this script again."
}

Install-WingetPackage "Git.Git" "Git"
Install-WingetPackage "EclipseAdoptium.Temurin.17.JDK" "Eclipse Temurin JDK 17"
Install-WingetPackage "Google.AndroidStudio" "Android Studio"

$ProjectRoot = [IO.Path]::GetFullPath($ProjectRoot)
if (-not (Test-Path (Join-Path $ProjectRoot "settings.gradle.kts"))) {
    if (-not $CloneIfMissing) {
        throw "ProjectRoot must be the Cue repository root. Could not find settings.gradle.kts in: $ProjectRoot"
    }

    if (Test-Path $ProjectRoot) {
        throw "Cannot clone into an existing directory: $ProjectRoot"
    }

    Write-Step "Cloning Cue"
    & git clone $RepositoryUrl $ProjectRoot
    if ($LASTEXITCODE -ne 0) {
        throw "Could not clone Cue from $RepositoryUrl. Exit code: $LASTEXITCODE"
    }
}

Write-Step "Configuring JDK $RequiredJavaMajor"
$javaHome = Get-Temurin17Home
Set-UserEnvironmentVariable "JAVA_HOME" $javaHome
$userPath = [Environment]::GetEnvironmentVariable("Path", [EnvironmentVariableTarget]::User)
$javaBin = Join-Path $javaHome "bin"
if ($userPath -notlike "*$javaBin*") {
    [Environment]::SetEnvironmentVariable("Path", "$javaBin;$userPath", [EnvironmentVariableTarget]::User)
}
$env:Path = "$javaBin;$env:Path"

Write-Step "Configuring Android SDK"
$sdkRoot = if ($env:ANDROID_SDK_ROOT) {
    $env:ANDROID_SDK_ROOT
} elseif ($env:ANDROID_HOME) {
    $env:ANDROID_HOME
} else {
    Join-Path $env:LOCALAPPDATA "Android\Sdk"
}
New-Item -ItemType Directory -Force -Path $sdkRoot | Out-Null
Set-UserEnvironmentVariable "ANDROID_SDK_ROOT" $sdkRoot
Set-UserEnvironmentVariable "ANDROID_HOME" $sdkRoot

$sdkManager = Get-SdkManager $sdkRoot
Write-Step "Accepting Android SDK licences"
$yesAnswers = ((1..100) | ForEach-Object { "y" }) -join [Environment]::NewLine
$yesAnswers | & $sdkManager "--sdk_root=$sdkRoot" --licenses | Out-Host
if ($LASTEXITCODE -ne 0) {
    throw "Android SDK licence acceptance failed. Exit code: $LASTEXITCODE"
}

Write-Step "Installing Cue's Android SDK packages"
& $sdkManager "--sdk_root=$sdkRoot" @RequiredSdkPackages | Out-Host
if ($LASTEXITCODE -ne 0) {
    throw "Android SDK package installation failed. Exit code: $LASTEXITCODE"
}

Write-Step "Writing project-local Android SDK path"
$escapedSdkRoot = $sdkRoot.Replace("\", "\\").Replace(":", "\:")
Set-Content -LiteralPath (Join-Path $ProjectRoot "local.properties") -Value "sdk.dir=$escapedSdkRoot" -NoNewline

if (-not $SkipVerification) {
    Write-Step "Downloading Gradle dependencies and validating the project"
    Push-Location $ProjectRoot
    try {
        & .\gradlew.bat testDebugUnitTest assembleDebug --stacktrace
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle validation failed. The development environment is installed; inspect the Gradle output for project issues."
        }
    } finally {
        Pop-Location
    }
}

Write-Host "`nCue development environment is ready." -ForegroundColor Green
Write-Host "Project: $ProjectRoot"
Write-Host "JAVA_HOME: $javaHome"
Write-Host "ANDROID_SDK_ROOT: $sdkRoot"
Write-Host "Open the project root in Android Studio."
