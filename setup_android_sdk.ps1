# Android SDK Setup Script for StingrayShield
# This script downloads and installs the necessary Android SDK components for building the app

Write-Host "========================================="
Write-Host "Android SDK Setup for StingrayShield"
Write-Host "========================================="

# Constants
$ANDROID_SDK_ROOT = "C:\Users\sunse\AppData\Local\Android\Sdk"
$CMDLINE_TOOLS_URL = "https://dl.google.com/android/repository/commandlinetools-win-9477386_latest.zip"
$CMDLINE_TOOLS_ZIP = "commandlinetools.zip"
$CMDLINE_TOOLS_DIR = "$ANDROID_SDK_ROOT\cmdline-tools"
$JAVA_HOME = "$env:PROGRAMFILES\Java\jdk-17" # Update this if your Java location is different

# Step 1: Create directories
Write-Host "[1/7] Creating directories..."
New-Item -Path $ANDROID_SDK_ROOT -ItemType Directory -Force | Out-Null
New-Item -Path "$ANDROID_SDK_ROOT\cmdline-tools" -ItemType Directory -Force | Out-Null

# Step 2: Download Command Line Tools
Write-Host "[2/7] Downloading Android SDK Command Line Tools..."
try {
    Invoke-WebRequest -Uri $CMDLINE_TOOLS_URL -OutFile $CMDLINE_TOOLS_ZIP
    Write-Host "Download successful!"
} catch {
    Write-Host "Error downloading Command Line Tools: $_"
    exit 1
}

# Step 3: Extract to correct location
Write-Host "[3/7] Extracting Android SDK Command Line Tools..."
try {
    Expand-Archive -Path $CMDLINE_TOOLS_ZIP -DestinationPath "$ANDROID_SDK_ROOT\cmdline-tools" -Force
    # Android SDK expects cmdline-tools/latest/ structure
    Move-Item -Path "$ANDROID_SDK_ROOT\cmdline-tools\cmdline-tools\*" -Destination "$ANDROID_SDK_ROOT\cmdline-tools\latest" -Force
    Write-Host "Extraction successful!"
} catch {
    Write-Host "Error extracting Command Line Tools: $_"
    exit 1
}

# Step 4: Set environment variables
Write-Host "[4/7] Setting environment variables..."
[Environment]::SetEnvironmentVariable("ANDROID_HOME", $ANDROID_SDK_ROOT, "User")
[Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", $ANDROID_SDK_ROOT, "User")
$env:ANDROID_HOME = $ANDROID_SDK_ROOT
$env:ANDROID_SDK_ROOT = $ANDROID_SDK_ROOT
$env:Path = "$env:Path;$ANDROID_SDK_ROOT\cmdline-tools\latest\bin;$ANDROID_SDK_ROOT\platform-tools;$ANDROID_SDK_ROOT\build-tools\34.0.0"
Write-Host "Environment variables set!"

# Step 5: Accept licenses
Write-Host "[5/7] Accepting Android SDK licenses..."
$licenseDir = "$ANDROID_SDK_ROOT\licenses"
New-Item -Path $licenseDir -ItemType Directory -Force | Out-Null

# Create license files
@(
    @{File="android-sdk-license"; Content="24333f8a63b6825ea9c5514f83c2829b004d1fee"},
    @{File="android-sdk-preview-license"; Content="84831b9409646a918e30573bab4c9c91346d8abd"},
    @{File="android-googletv-license"; Content="601085b94cd77f0b54ff86406957099ebe79c4d6"},
    @{File="google-gdk-license"; Content="33b6a2b64607f11b759f320ef9dff4ae5c47d97a"},
    @{File="mips-android-sysimage-license"; Content="e9acab5b5fbb560a72cfaecce8946896ff6aab9d"}
) | ForEach-Object {
    Set-Content -Path "$licenseDir\$($_.File)" -Value $_.Content -Force
}
Write-Host "Licenses accepted!"

# Step 6: Install required SDK components
Write-Host "[6/7] Installing Android SDK components (this may take several minutes)..."
try {
    # Run sdkmanager to install components
    $env:JAVA_HOME = $JAVA_HOME
    $sdkmanager = "$ANDROID_SDK_ROOT\cmdline-tools\latest\bin\sdkmanager.bat"
    
    # Install platform tools
    & $sdkmanager "platform-tools" "--sdk_root=$ANDROID_SDK_ROOT"
    
    # Install build tools (we need version 34.0.0)
    & $sdkmanager "build-tools;34.0.0" "--sdk_root=$ANDROID_SDK_ROOT"
    
    # Install platform 35 (Android 15)
    & $sdkmanager "platforms;android-35" "--sdk_root=$ANDROID_SDK_ROOT"
    
    # Install other necessary components
    & $sdkmanager "extras;android;m2repository" "--sdk_root=$ANDROID_SDK_ROOT"
    & $sdkmanager "extras;google;m2repository" "--sdk_root=$ANDROID_SDK_ROOT"
    
    Write-Host "SDK components installed!"
} catch {
    Write-Host "Error installing SDK components: $_"
    Write-Host "You may need to run this part manually with administrator privileges or ensure Java is installed"
}

# Step 7: Update local.properties
Write-Host "[7/7] Updating local.properties..."
$localProperties = @"
## This file must *NOT* be checked into Version Control Systems,
# as it contains information specific to your local configuration.
#
# Location of the SDK. This is only used by Gradle.
# For customization when using a Version Control System, please read the
# header note.
sdk.dir=$($ANDROID_SDK_ROOT.Replace('\', '\\'))

# Also set the ANDROID_HOME environment variable
android.useAndroidX=true
android.enableJetifier=true
android.suppressUnsupportedCompileSdk=35
"@
Set-Content -Path "C:\Users\sunse\CascadeProjects\StingrayShield\local.properties" -Value $localProperties -Force
Write-Host "local.properties updated!"

Write-Host "========================================="
Write-Host "Android SDK Setup Complete!"
Write-Host "========================================="
Write-Host "You can now build the StingrayShield APK with:"
Write-Host "cd C:\Users\sunse\CascadeProjects\StingrayShield"
Write-Host ".\gradlew assembleRelease"
Write-Host ""
Write-Host "Note: If you get any errors about missing Java,"
Write-Host "make sure Java is installed and JAVA_HOME is set correctly."
Write-Host "Current JAVA_HOME path used: $JAVA_HOME"
Write-Host "========================================="

# Clean up
Remove-Item -Path $CMDLINE_TOOLS_ZIP -Force
Write-Host "Temporary files cleaned up."
