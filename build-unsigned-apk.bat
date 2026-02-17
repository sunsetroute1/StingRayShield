@echo off
echo Building StingrayShield unsigned debug APK...

set ANDROID_HOME=C:\Users\sunse\AppData\Local\Android\Sdk
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\tools;%ANDROID_HOME%\tools\bin;%ANDROID_HOME%\platform-tools;%PATH%

echo Using Android SDK at: %ANDROID_HOME%
echo Using Java at: %JAVA_HOME%

echo Running Gradle clean...
call gradlew.bat clean --no-daemon --stacktrace

echo Running Gradle assembleDebug...
call gradlew.bat assembleDebug --no-daemon --stacktrace

if %ERRORLEVEL% == 0 (
    echo Build successful! Debug APK should be available at:
    echo app\build\outputs\apk\debug\app-debug.apk
) else (
    echo Build failed with error code: %ERRORLEVEL%
)

echo Build script completed.
