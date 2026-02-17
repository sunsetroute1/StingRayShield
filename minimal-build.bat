@echo off
echo Building Minimal StingrayShield Debug APK...

REM Setup Environment Variables
set ANDROID_HOME=C:\Users\sunse\AppData\Local\Android\Sdk
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set PATH=%JAVA_HOME%\bin;%PATH%

echo Using Android SDK: %ANDROID_HOME%
echo Using Java: %JAVA_HOME%

REM Create a temporary modified build.gradle for minimal build
echo Creating minimal build configuration...
set TEMP_BUILD_FILE=app\build.gradle.kts.minimal
copy app\build.gradle.kts %TEMP_BUILD_FILE%

REM Run with specific tasks and options for minimal build
echo Building minimal debug APK...
call gradlew assembleDebug -Dorg.gradle.java.home="%JAVA_HOME%" -Pandroid.optional.compilation=INSTANT_DEV --no-daemon --stacktrace --info

REM Check if build succeeded
if %ERRORLEVEL% == 0 (
    echo Build successful! 
    echo Debug APK location: %CD%\app\build\outputs\apk\debug\app-debug.apk
    
    REM Create output directory
    if not exist apk-output mkdir apk-output
    
    REM Copy the APK to output directory with a proper name
    copy app\build\outputs\apk\debug\app-debug.apk apk-output\stingrayshield-minimal-debug.apk
    
    echo APK saved to: %CD%\apk-output\stingrayshield-minimal-debug.apk
) else (
    echo Build failed with error code %ERRORLEVEL%
    
    REM Try direct APK generation with Android Build Tools
    echo Attempting alternative build method with Android Build Tools...
    
    REM Set up build tools paths
    set BUILD_TOOLS=%ANDROID_HOME%\build-tools\34.0.0
    set PLATFORM=%ANDROID_HOME%\platforms\android-35
    set OUTPUT_DIR=apk-output\direct-build
    
    REM Create output directory
    if not exist %OUTPUT_DIR% mkdir %OUTPUT_DIR%
    
    REM Create a very basic signed APK
    echo Creating basic APK structure...
    call %BUILD_TOOLS%\aapt.exe package -f -m -J app\src\main\java -M app\src\main\AndroidManifest.xml -S app\src\main\res -I %PLATFORM%\android.jar -F %OUTPUT_DIR%\stingrayshield-basic.apk
    
    echo APK structure created at: %CD%\%OUTPUT_DIR%\stingrayshield-basic.apk
    echo This is an unsigned minimal APK - it will not contain all functionality but can be used for basic tests.
)

echo Done.
pause
