@echo off
echo StingrayShield APK Generator

set ANDROID_HOME=C:\Users\sunse\AppData\Local\Android\Sdk
set BUILD_TOOLS=%ANDROID_HOME%\build-tools\34.0.0
set OUTPUT_DIR=app\build\outputs\apk\debug
set APK_OUTPUT_DIR=apk-output
set DEBUG_APK=app-debug.apk
set UNSIGNED_APK=stingrayshield-unsigned.apk
set SIGNED_APK=stingrayshield-debug.apk

echo Creating output directory...
if not exist %APK_OUTPUT_DIR% mkdir %APK_OUTPUT_DIR%

echo Checking if a previous build exists...
if exist "%OUTPUT_DIR%\%DEBUG_APK%" (
    echo Found existing debug APK, copying to output directory...
    copy "%OUTPUT_DIR%\%DEBUG_APK%" "%APK_OUTPUT_DIR%\%UNSIGNED_APK%"
    echo Copied existing debug APK to %APK_OUTPUT_DIR%\%UNSIGNED_APK%
) else (
    echo No existing debug APK found.
    echo Please build the project first using Android Studio or Gradle.
    echo Once built, the APK should be in %OUTPUT_DIR%\%DEBUG_APK%
)

echo.
echo =========================================================
echo INSTRUCTIONS FOR MANUAL APK SIGNING:
echo =========================================================
echo.
echo To sign the APK manually using Android SDK tools:
echo.
echo 1. Generate a debug keystore (if you don't have one):
echo    %BUILD_TOOLS%\keytool -genkey -v -keystore debug.keystore -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android
echo.
echo 2. Sign the APK:
echo    %BUILD_TOOLS%\apksigner sign --ks debug.keystore --ks-pass pass:android --key-pass pass:android --out %APK_OUTPUT_DIR%\%SIGNED_APK% %APK_OUTPUT_DIR%\%UNSIGNED_APK%
echo.
echo 3. Verify the signed APK:
echo    %BUILD_TOOLS%\apksigner verify %APK_OUTPUT_DIR%\%SIGNED_APK%
echo.
echo 4. Install the APK on a connected device:
echo    %ANDROID_HOME%\platform-tools\adb install -r %APK_OUTPUT_DIR%\%SIGNED_APK%
echo.
echo =========================================================
echo.

echo Script completed.
