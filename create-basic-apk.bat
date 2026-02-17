@echo off
echo Creating basic StingrayShield APK with Android 15/16 compatibility...

REM Setup Environment Variables
set ANDROID_HOME=C:\Users\sunse\AppData\Local\Android\Sdk
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\tools;%ANDROID_HOME%\tools\bin;%ANDROID_HOME%\platform-tools;%PATH%
set BUILD_TOOLS=%ANDROID_HOME%\build-tools\34.0.0
set PLATFORM=%ANDROID_HOME%\platforms\android-35
set OUTPUT_DIR=apk-output
set PROJECT_DIR=basic-stingrayshield

if not exist %OUTPUT_DIR% mkdir %OUTPUT_DIR%
if exist %PROJECT_DIR% rmdir /s /q %PROJECT_DIR%
mkdir %PROJECT_DIR%

echo Creating project structure...
mkdir %PROJECT_DIR%\res\layout
mkdir %PROJECT_DIR%\res\values
mkdir %PROJECT_DIR%\res\drawable

echo Creating basic resources...

REM Create strings.xml
echo ^<?xml version="1.0" encoding="utf-8"?^> > %PROJECT_DIR%\res\values\strings.xml
echo ^<resources^> >> %PROJECT_DIR%\res\values\strings.xml
echo     ^<string name="app_name"^>StingrayShield^</string^> >> %PROJECT_DIR%\res\values\strings.xml
echo     ^<string name="app_description"^>Android 15/16 Compatible IMSI-Catcher Detection^</string^> >> %PROJECT_DIR%\res\values\strings.xml
echo ^</resources^> >> %PROJECT_DIR%\res\values\strings.xml

REM Create main.xml layout
echo ^<?xml version="1.0" encoding="utf-8"?^> > %PROJECT_DIR%\res\layout\main.xml
echo ^<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" >> %PROJECT_DIR%\res\layout\main.xml
echo     android:orientation="vertical" >> %PROJECT_DIR%\res\layout\main.xml
echo     android:layout_width="match_parent" >> %PROJECT_DIR%\res\layout\main.xml
echo     android:layout_height="match_parent" >> %PROJECT_DIR%\res\layout\main.xml
echo     android:gravity="center"^> >> %PROJECT_DIR%\res\layout\main.xml
echo     ^<TextView >> %PROJECT_DIR%\res\layout\main.xml
echo         android:layout_width="wrap_content" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:layout_height="wrap_content" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:text="@string/app_name" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:textSize="24sp" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:textStyle="bold" /^> >> %PROJECT_DIR%\res\layout\main.xml
echo     ^<TextView >> %PROJECT_DIR%\res\layout\main.xml
echo         android:layout_width="wrap_content" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:layout_height="wrap_content" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:text="@string/app_description" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:layout_marginTop="8dp" /^> >> %PROJECT_DIR%\res\layout\main.xml
echo ^</LinearLayout^> >> %PROJECT_DIR%\res\layout\main.xml

REM Create app icon
echo ^<?xml version="1.0" encoding="utf-8"?^> > %PROJECT_DIR%\res\drawable\icon.xml
echo ^<vector xmlns:android="http://schemas.android.com/apk/res/android" >> %PROJECT_DIR%\res\drawable\icon.xml
echo     android:width="24dp" >> %PROJECT_DIR%\res\drawable\icon.xml
echo     android:height="24dp" >> %PROJECT_DIR%\res\drawable\icon.xml
echo     android:viewportWidth="24" >> %PROJECT_DIR%\res\drawable\icon.xml
echo     android:viewportHeight="24"^> >> %PROJECT_DIR%\res\drawable\icon.xml
echo     ^<path >> %PROJECT_DIR%\res\drawable\icon.xml
echo         android:fillColor="#0066CC" >> %PROJECT_DIR%\res\drawable\icon.xml
echo         android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,20c-4.42,0 -8,-3.58 -8,-8s3.58,-8 8,-8 8,3.58 8,8 -3.58,8 -8,8z"/^> >> %PROJECT_DIR%\res\drawable\icon.xml
echo     ^<path >> %PROJECT_DIR%\res\drawable\icon.xml
echo         android:fillColor="#0066CC" >> %PROJECT_DIR%\res\drawable\icon.xml
echo         android:pathData="M7.5,12c0,-0.83 0.67,-1.5 1.5,-1.5s1.5,0.67 1.5,1.5 -0.67,1.5 -1.5,1.5 -1.5,-0.67 -1.5,-1.5zM12,12c0,-0.83 0.67,-1.5 1.5,-1.5s1.5,0.67 1.5,1.5 -0.67,1.5 -1.5,1.5 -1.5,-0.67 -1.5,-1.5z"/^> >> %PROJECT_DIR%\res\drawable\icon.xml
echo ^</vector^> >> %PROJECT_DIR%\res\drawable\icon.xml

REM Create AndroidManifest.xml
echo ^<?xml version="1.0" encoding="utf-8"?^> > %PROJECT_DIR%\AndroidManifest.xml
echo ^<manifest xmlns:android="http://schemas.android.com/apk/res/android" >> %PROJECT_DIR%\AndroidManifest.xml
echo     package="com.stingrayshield" >> %PROJECT_DIR%\AndroidManifest.xml
echo     android:versionCode="1" >> %PROJECT_DIR%\AndroidManifest.xml
echo     android:versionName="1.0"^> >> %PROJECT_DIR%\AndroidManifest.xml
echo. >> %PROJECT_DIR%\AndroidManifest.xml
echo     ^<uses-sdk android:minSdkVersion="24" android:targetSdkVersion="35" /^> >> %PROJECT_DIR%\AndroidManifest.xml
echo. >> %PROJECT_DIR%\AndroidManifest.xml
echo     ^<application android:label="@string/app_name" >> %PROJECT_DIR%\AndroidManifest.xml
echo                  android:icon="@drawable/icon"^> >> %PROJECT_DIR%\AndroidManifest.xml
echo         ^<activity android:name=".MainActivity" >> %PROJECT_DIR%\AndroidManifest.xml
echo                   android:exported="true"^> >> %PROJECT_DIR%\AndroidManifest.xml
echo             ^<intent-filter^> >> %PROJECT_DIR%\AndroidManifest.xml
echo                 ^<action android:name="android.intent.action.MAIN" /^> >> %PROJECT_DIR%\AndroidManifest.xml
echo                 ^<category android:name="android.intent.category.LAUNCHER" /^> >> %PROJECT_DIR%\AndroidManifest.xml
echo             ^</intent-filter^> >> %PROJECT_DIR%\AndroidManifest.xml
echo         ^</activity^> >> %PROJECT_DIR%\AndroidManifest.xml
echo     ^</application^> >> %PROJECT_DIR%\AndroidManifest.xml
echo ^</manifest^> >> %PROJECT_DIR%\AndroidManifest.xml

REM Create empty classes.dex file
echo Creating empty classes.dex placeholder (for packaging only)...
type nul > %PROJECT_DIR%\classes.dex

REM Package resources into APK
echo Building APK...
cd %PROJECT_DIR%
call %BUILD_TOOLS%\aapt.exe package -f -M AndroidManifest.xml -S res -I %PLATFORM%\android.jar -F stingrayshield-unsigned.apk .

REM Check if APK was created
if exist stingrayshield-unsigned.apk (
    echo Successfully created basic APK
    copy stingrayshield-unsigned.apk ..\%OUTPUT_DIR%\stingrayshield-unsigned.apk
    echo APK copied to %OUTPUT_DIR%\stingrayshield-unsigned.apk
) else (
    echo Failed to create APK
)

cd ..

echo.
echo ==========================================================
echo   STINGRAYSHIELD BASIC APK CREATED FOR ANDROID 15/16
echo ==========================================================
echo.
echo This is a basic APK that can be used for testing Android 15/16 
echo compatibility. It contains the essential app structure but 
echo without full functionality.
echo.
echo To sign the APK for testing:
echo.
echo 1. Generate a debug keystore (if you don't already have one):
echo    %JAVA_HOME%\bin\keytool -genkey -v -keystore debug.keystore -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android
echo.
echo 2. Sign the APK:
echo    %BUILD_TOOLS%\apksigner sign --ks debug.keystore --ks-pass pass:android --key-pass pass:android --out %OUTPUT_DIR%\stingrayshield-signed.apk %OUTPUT_DIR%\stingrayshield-unsigned.apk
echo.
echo 3. Install on device:
echo    %ANDROID_HOME%\platform-tools\adb install -r %OUTPUT_DIR%\stingrayshield-signed.apk
echo.
pause
