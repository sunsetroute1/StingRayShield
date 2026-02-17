@echo off
echo Creating StingrayShield APK for Android 15/16 Testing...

REM Setup Environment Variables
set ANDROID_HOME=C:\Users\sunse\AppData\Local\Android\Sdk
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\tools;%ANDROID_HOME%\tools\bin;%ANDROID_HOME%\platform-tools;%PATH%
set BUILD_TOOLS=%ANDROID_HOME%\build-tools\34.0.0
set PLATFORM=%ANDROID_HOME%\platforms\android-35
set OUTPUT_DIR=apk-output
set PROJECT_DIR=final-stingrayshield

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
echo     ^<string name="app_version"^>Android 15/16 Compatible^</string^> >> %PROJECT_DIR%\res\values\strings.xml
echo     ^<string name="app_description"^>Advanced 4G/5G IMSI-Catcher Detection^</string^> >> %PROJECT_DIR%\res\values\strings.xml
echo     ^<string name="features"^>• 4G/5G Stingray Detection\n• Cell Tower Monitoring\n• Location Based Analysis\n• Enhanced Privacy Protection^</string^> >> %PROJECT_DIR%\res\values\strings.xml
echo ^</resources^> >> %PROJECT_DIR%\res\values\strings.xml

REM Create colors.xml
echo ^<?xml version="1.0" encoding="utf-8"?^> > %PROJECT_DIR%\res\values\colors.xml
echo ^<resources^> >> %PROJECT_DIR%\res\values\colors.xml
echo     ^<color name="colorPrimary"^>#0066CC^</color^> >> %PROJECT_DIR%\res\values\colors.xml
echo     ^<color name="colorAccent"^>#FF4081^</color^> >> %PROJECT_DIR%\res\values\colors.xml
echo     ^<color name="textLight"^>#FFFFFF^</color^> >> %PROJECT_DIR%\res\values\colors.xml
echo     ^<color name="textDark"^>#333333^</color^> >> %PROJECT_DIR%\res\values\colors.xml
echo     ^<color name="appBackground"^>#F5F5F5^</color^> >> %PROJECT_DIR%\res\values\colors.xml
echo ^</resources^> >> %PROJECT_DIR%\res\values\colors.xml

REM Create main.xml layout
echo ^<?xml version="1.0" encoding="utf-8"?^> > %PROJECT_DIR%\res\layout\main.xml
echo ^<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" >> %PROJECT_DIR%\res\layout\main.xml
echo     android:orientation="vertical" >> %PROJECT_DIR%\res\layout\main.xml
echo     android:layout_width="match_parent" >> %PROJECT_DIR%\res\layout\main.xml
echo     android:layout_height="match_parent" >> %PROJECT_DIR%\res\layout\main.xml
echo     android:background="@color/appBackground" >> %PROJECT_DIR%\res\layout\main.xml
echo     android:gravity="center" >> %PROJECT_DIR%\res\layout\main.xml
echo     android:padding="16dp"^> >> %PROJECT_DIR%\res\layout\main.xml
echo     >> %PROJECT_DIR%\res\layout\main.xml
echo     ^<ImageView >> %PROJECT_DIR%\res\layout\main.xml
echo         android:layout_width="96dp" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:layout_height="96dp" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:src="@drawable/icon" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:layout_marginBottom="24dp" /^> >> %PROJECT_DIR%\res\layout\main.xml
echo     >> %PROJECT_DIR%\res\layout\main.xml
echo     ^<TextView >> %PROJECT_DIR%\res\layout\main.xml
echo         android:layout_width="wrap_content" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:layout_height="wrap_content" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:text="@string/app_name" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:textColor="@color/colorPrimary" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:textSize="28sp" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:textStyle="bold" /^> >> %PROJECT_DIR%\res\layout\main.xml
echo     >> %PROJECT_DIR%\res\layout\main.xml
echo     ^<TextView >> %PROJECT_DIR%\res\layout\main.xml
echo         android:layout_width="wrap_content" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:layout_height="wrap_content" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:text="@string/app_version" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:textColor="@color/textDark" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:textSize="16sp" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:layout_marginTop="8dp" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:layout_marginBottom="16dp" /^> >> %PROJECT_DIR%\res\layout\main.xml
echo     >> %PROJECT_DIR%\res\layout\main.xml
echo     ^<TextView >> %PROJECT_DIR%\res\layout\main.xml
echo         android:layout_width="wrap_content" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:layout_height="wrap_content" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:text="@string/app_description" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:textColor="@color/textDark" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:textSize="18sp" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:layout_marginBottom="24dp" /^> >> %PROJECT_DIR%\res\layout\main.xml
echo     >> %PROJECT_DIR%\res\layout\main.xml
echo     ^<TextView >> %PROJECT_DIR%\res\layout\main.xml
echo         android:layout_width="wrap_content" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:layout_height="wrap_content" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:text="@string/features" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:textColor="@color/textDark" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:textSize="16sp" >> %PROJECT_DIR%\res\layout\main.xml
echo         android:lineSpacingExtra="4dp" /^> >> %PROJECT_DIR%\res\layout\main.xml
echo     >> %PROJECT_DIR%\res\layout\main.xml
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
echo         android:pathData="M12,12m-5,0a5,5 0,1 1,10 0a5,5 0,1 1,-10 0"/^> >> %PROJECT_DIR%\res\drawable\icon.xml
echo     ^<path >> %PROJECT_DIR%\res\drawable\icon.xml
echo         android:fillColor="#FFFFFF" >> %PROJECT_DIR%\res\drawable\icon.xml
echo         android:pathData="M12,8L12,16M8,12L16,12"/^> >> %PROJECT_DIR%\res\drawable\icon.xml
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
echo     ^<!-- Permissions needed for cell tower detection --^> >> %PROJECT_DIR%\AndroidManifest.xml
echo     ^<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" /^> >> %PROJECT_DIR%\AndroidManifest.xml
echo     ^<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" /^> >> %PROJECT_DIR%\AndroidManifest.xml
echo     ^<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" /^> >> %PROJECT_DIR%\AndroidManifest.xml
echo     ^<uses-permission android:name="android.permission.READ_PHONE_STATE" /^> >> %PROJECT_DIR%\AndroidManifest.xml
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

REM First step: Package the resources into APK
echo Packaging resources into APK...
call %BUILD_TOOLS%\aapt.exe package -f -M %PROJECT_DIR%\AndroidManifest.xml -S %PROJECT_DIR%\res -I %PLATFORM%\android.jar -F %OUTPUT_DIR%\stingrayshield-unsigned.apk

REM Check if APK was created
if exist %OUTPUT_DIR%\stingrayshield-unsigned.apk (
    echo Successfully created StingrayShield APK!
) else (
    echo Failed to create APK.
    goto end
)

echo.
echo ==========================================================
echo   STINGRAYSHIELD APK FOR ANDROID 15/16 CREATED
echo ==========================================================
echo.
echo The APK has been created successfully at:
echo %CD%\%OUTPUT_DIR%\stingrayshield-unsigned.apk
echo.
echo This APK provides a basic structure that demonstrates
echo Android 15/16 compatibility. While it doesn't include the
echo full functionality of StingrayShield, it serves as a 
echo placeholder for testing purposes.
echo.
echo To sign the APK for installation on a device:
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

:end
pause
