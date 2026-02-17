@echo off
echo Creating Simplified StingrayShield APK Build...

REM Setup Environment Variables
set ANDROID_HOME=C:\Users\sunse\AppData\Local\Android\Sdk
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\tools;%ANDROID_HOME%\tools\bin;%ANDROID_HOME%\platform-tools;%PATH%
set BUILD_TOOLS=%ANDROID_HOME%\build-tools\34.0.0
set PLATFORM=%ANDROID_HOME%\platforms\android-35

REM Create output directories
set OUTPUT_DIR=apk-output
if not exist %OUTPUT_DIR% mkdir %OUTPUT_DIR%

echo Using Android SDK: %ANDROID_HOME%
echo Using Java: %JAVA_HOME%
echo Using Build Tools: %BUILD_TOOLS%

REM Create a simple Java-based app as a fallback
echo Creating a simplified Java version of StingrayShield...
set TEMP_DIR=temp-java-project
if not exist %TEMP_DIR% mkdir %TEMP_DIR%
if not exist %TEMP_DIR%\src\main\java\com\stingrayshield mkdir %TEMP_DIR%\src\main\java\com\stingrayshield
if not exist %TEMP_DIR%\src\main\res\layout mkdir %TEMP_DIR%\src\main\res\layout
if not exist %TEMP_DIR%\src\main\res\values mkdir %TEMP_DIR%\src\main\res\values
if not exist %TEMP_DIR%\src\main\res\drawable mkdir %TEMP_DIR%\src\main\res\drawable

REM Copy the existing resources
echo Copying existing resources...
xcopy app\src\main\res\drawable %TEMP_DIR%\src\main\res\drawable /E /I /Y
xcopy app\src\main\res\values %TEMP_DIR%\src\main\res\values /E /I /Y

REM Create a simple AndroidManifest.xml
echo Creating simplified AndroidManifest.xml...
echo ^<?xml version="1.0" encoding="utf-8"?^> > %TEMP_DIR%\src\main\AndroidManifest.xml
echo ^<manifest xmlns:android="http://schemas.android.com/apk/res/android"^> >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo     ^<application >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo         android:allowBackup="true" >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo         android:icon="@drawable/ic_app_icon" >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo         android:label="StingrayShield" >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo         android:theme="@android:style/Theme.DeviceDefault"^> >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo         ^<activity >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo             android:name="com.stingrayshield.MainActivity" >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo             android:exported="true"^> >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo             ^<intent-filter^> >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo                 ^<action android:name="android.intent.action.MAIN" /^> >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo                 ^<category android:name="android.intent.category.LAUNCHER" /^> >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo             ^</intent-filter^> >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo         ^</activity^> >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo     ^</application^> >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo ^</manifest^> >> %TEMP_DIR%\src\main\AndroidManifest.xml

REM Create simple Java MainActivity
echo Creating simple MainActivity.java...
echo package com.stingrayshield; > %TEMP_DIR%\src\main\java\com\stingrayshield\MainActivity.java
echo. >> %TEMP_DIR%\src\main\java\com\stingrayshield\MainActivity.java
echo import android.app.Activity; >> %TEMP_DIR%\src\main\java\com\stingrayshield\MainActivity.java
echo import android.os.Bundle; >> %TEMP_DIR%\src\main\java\com\stingrayshield\MainActivity.java
echo import android.widget.TextView; >> %TEMP_DIR%\src\main\java\com\stingrayshield\MainActivity.java
echo. >> %TEMP_DIR%\src\main\java\com\stingrayshield\MainActivity.java
echo public class MainActivity extends Activity { >> %TEMP_DIR%\src\main\java\com\stingrayshield\MainActivity.java
echo     @Override >> %TEMP_DIR%\src\main\java\com\stingrayshield\MainActivity.java
echo     protected void onCreate(Bundle savedInstanceState) { >> %TEMP_DIR%\src\main\java\com\stingrayshield\MainActivity.java
echo         super.onCreate(savedInstanceState); >> %TEMP_DIR%\src\main\java\com\stingrayshield\MainActivity.java
echo. >> %TEMP_DIR%\src\main\java\com\stingrayshield\MainActivity.java
echo         // Create a simple text view with version info >> %TEMP_DIR%\src\main\java\com\stingrayshield\MainActivity.java
echo         TextView textView = new TextView(this); >> %TEMP_DIR%\src\main\java\com\stingrayshield\MainActivity.java
echo         textView.setText("StingrayShield\nVersion 1.0\nCompatible with Android 15"); >> %TEMP_DIR%\src\main\java\com\stingrayshield\MainActivity.java
echo         textView.setPadding(50, 50, 50, 50); >> %TEMP_DIR%\src\main\java\com\stingrayshield\MainActivity.java
echo         setContentView(textView); >> %TEMP_DIR%\src\main\java\com\stingrayshield\MainActivity.java
echo     } >> %TEMP_DIR%\src\main\java\com\stingrayshield\MainActivity.java
echo } >> %TEMP_DIR%\src\main\java\com\stingrayshield\MainActivity.java

REM Compile Java files
echo Compiling Java files...
javac -d %TEMP_DIR%\classes -classpath %PLATFORM%\android.jar %TEMP_DIR%\src\main\java\com\stingrayshield\MainActivity.java

REM Convert class files to dex
echo Converting classes to DEX format...
mkdir %TEMP_DIR%\dex
call %BUILD_TOOLS%\d8.bat --output %TEMP_DIR%\dex %TEMP_DIR%\classes\com\stingrayshield\MainActivity.class

REM Package resources
echo Packaging resources...
call %BUILD_TOOLS%\aapt.exe package -f -M %TEMP_DIR%\src\main\AndroidManifest.xml -S app\src\main\res -I %PLATFORM%\android.jar -F %TEMP_DIR%\stingrayshield-resources.ap_

REM Create APK
echo Creating APK...
mkdir %TEMP_DIR%\final
cd %TEMP_DIR%
call %BUILD_TOOLS%\aapt.exe add stingrayshield-resources.ap_ dex\classes.dex
move /y stingrayshield-resources.ap_ final\stingrayshield-unsigned.apk
cd ..

echo APK created at: %CD%\%TEMP_DIR%\final\stingrayshield-unsigned.apk

REM Copy to output directory
copy %TEMP_DIR%\final\stingrayshield-unsigned.apk %OUTPUT_DIR%\stingrayshield-unsigned.apk

echo APK saved to: %CD%\%OUTPUT_DIR%\stingrayshield-unsigned.apk
echo.
echo =====================================
echo APK SIGNING INSTRUCTIONS:
echo =====================================
echo To sign the APK for testing:
echo 1. %BUILD_TOOLS%\apksigner sign --ks debug.keystore --ks-pass pass:android --out %OUTPUT_DIR%\stingrayshield-signed.apk %OUTPUT_DIR%\stingrayshield-unsigned.apk
echo.
echo If you don't have a debug keystore, create one with:
echo %JAVA_HOME%\bin\keytool -genkey -v -keystore debug.keystore -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android
echo.
echo Done.
pause
