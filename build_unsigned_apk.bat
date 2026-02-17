@echo off
echo Building StingrayShield Unsigned Debug APK...
echo.

cd %~dp0
call gradlew.bat clean assembleDebug

if %errorlevel% equ 0 (
    echo.
    echo Build successful!
    echo APK location: %~dp0app\build\outputs\apk\debug\app-debug.apk
) else (
    echo.
    echo Build failed with error code: %errorlevel%
)

pause
