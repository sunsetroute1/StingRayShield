@echo off
echo Building StingrayShield Debug APK with Updated Configuration...

REM Setup Environment Variables
set ANDROID_HOME=C:\Users\sunse\AppData\Local\Android\Sdk
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set PATH=%JAVA_HOME%\bin;%PATH%

echo Using Android SDK: %ANDROID_HOME%
echo Using Java: %JAVA_HOME%

REM Clean the project first
echo Cleaning project...
call gradlew clean --no-daemon --stacktrace 

REM Build only the debug variant
echo Building debug APK...
call gradlew assembleDebug --no-daemon --stacktrace --debug > build-log.txt
echo Detailed build log saved to build-log.txt

REM Check if build succeeded
if %ERRORLEVEL% == 0 (
    echo Build successful! 
    echo Debug APK location: %CD%\app\build\outputs\apk\debug\app-debug.apk
    
    REM Create output directory
    if not exist apk-output mkdir apk-output
    
    REM Copy the APK to output directory with a proper name
    copy app\build\outputs\apk\debug\app-debug.apk apk-output\stingrayshield-debug-unsigned.apk
    
    echo APK saved to: %CD%\apk-output\stingrayshield-debug-unsigned.apk
) else (
    echo Build failed with error code %ERRORLEVEL%
    echo Please check the build logs for more details.
)

echo Done.
pause
