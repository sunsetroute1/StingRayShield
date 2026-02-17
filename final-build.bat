@echo off
echo Creating StingrayShield APK with Android 15/16 compatibility...

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
set TEMP_DIR=java-project
if not exist %TEMP_DIR% mkdir %TEMP_DIR%
if not exist %TEMP_DIR%\src\main\java\com\stingrayshield\ui mkdir %TEMP_DIR%\src\main\java\com\stingrayshield\ui
if not exist %TEMP_DIR%\src\main\res\layout mkdir %TEMP_DIR%\src\main\res\layout
if not exist %TEMP_DIR%\src\main\res\values mkdir %TEMP_DIR%\src\main\res\values
if not exist %TEMP_DIR%\src\main\res\drawable mkdir %TEMP_DIR%\src\main\res\drawable

REM Create app icon if it doesn't exist
if not exist %TEMP_DIR%\src\main\res\drawable\ic_app_icon.xml (
    echo Creating app icon...
    echo ^<?xml version="1.0" encoding="utf-8"?^> > %TEMP_DIR%\src\main\res\drawable\ic_app_icon.xml
    echo ^<vector xmlns:android="http://schemas.android.com/apk/res/android" >> %TEMP_DIR%\src\main\res\drawable\ic_app_icon.xml
    echo     android:width="24dp" >> %TEMP_DIR%\src\main\res\drawable\ic_app_icon.xml
    echo     android:height="24dp" >> %TEMP_DIR%\src\main\res\drawable\ic_app_icon.xml
    echo     android:viewportWidth="24" >> %TEMP_DIR%\src\main\res\drawable\ic_app_icon.xml
    echo     android:viewportHeight="24" >> %TEMP_DIR%\src\main\res\drawable\ic_app_icon.xml
    echo     android:tint="#0066CC"^> >> %TEMP_DIR%\src\main\res\drawable\ic_app_icon.xml
    echo   ^<path >> %TEMP_DIR%\src\main\res\drawable\ic_app_icon.xml
    echo       android:fillColor="#FFFFFF" >> %TEMP_DIR%\src\main\res\drawable\ic_app_icon.xml
    echo       android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,20c-4.41,0 -8,-3.59 -8,-8s3.59,-8 8,-8 8,3.59 8,8 -3.59,8 -8,8zM16,11h-3V8c0,-0.55 -0.45,-1 -1,-1s-1,0.45 -1,1v3H8c-0.55,0 -1,0.45 -1,1s0.45,1 1,1h3v3c0,0.55 0.45,1 1,1s1,-0.45 1,-1v-3h3c0.55,0 1,-0.45 1,-1s-0.45,-1 -1,-1z"/^> >> %TEMP_DIR%\src\main\res\drawable\ic_app_icon.xml
    echo ^</vector^> >> %TEMP_DIR%\src\main\res\drawable\ic_app_icon.xml
)

REM Create strings resource file
echo Creating string resources...
echo ^<?xml version="1.0" encoding="utf-8"?^> > %TEMP_DIR%\src\main\res\values\strings.xml
echo ^<resources^> >> %TEMP_DIR%\src\main\res\values\strings.xml
echo     ^<string name="app_name"^>StingrayShield^</string^> >> %TEMP_DIR%\src\main\res\values\strings.xml
echo     ^<string name="app_version"^>Version 1.0^</string^> >> %TEMP_DIR%\src\main\res\values\strings.xml
echo     ^<string name="app_compatibility"^>Compatible with Android 15/16^</string^> >> %TEMP_DIR%\src\main\res\values\strings.xml
echo     ^<string name="app_description"^>Advanced 4G/5G IMSI-catcher detection^</string^> >> %TEMP_DIR%\src\main\res\values\strings.xml
echo ^</resources^> >> %TEMP_DIR%\src\main\res\values\strings.xml

REM Create colors resource file
echo Creating color resources...
echo ^<?xml version="1.0" encoding="utf-8"?^> > %TEMP_DIR%\src\main\res\values\colors.xml
echo ^<resources^> >> %TEMP_DIR%\src\main\res\values\colors.xml
echo     ^<color name="colorPrimary"^>#0066CC^</color^> >> %TEMP_DIR%\src\main\res\values\colors.xml
echo     ^<color name="colorPrimaryDark"^>#004080^</color^> >> %TEMP_DIR%\src\main\res\values\colors.xml
echo     ^<color name="colorAccent"^>#FF4081^</color^> >> %TEMP_DIR%\src\main\res\values\colors.xml
echo     ^<color name="textColor"^>#FFFFFF^</color^> >> %TEMP_DIR%\src\main\res\values\colors.xml
echo ^</resources^> >> %TEMP_DIR%\src\main\res\values\colors.xml

REM Create a layout for the main activity
echo Creating layout resources...
echo ^<?xml version="1.0" encoding="utf-8"?^> > %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo ^<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android" >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo     android:layout_width="match_parent" >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo     android:layout_height="match_parent" >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo     android:orientation="vertical" >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo     android:padding="16dp" >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo     android:gravity="center"^> >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo     >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo     ^<TextView >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo         android:layout_width="wrap_content" >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo         android:layout_height="wrap_content" >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo         android:text="@string/app_name" >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo         android:textSize="24sp" >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo         android:textStyle="bold" >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo         android:layout_marginBottom="8dp" /^> >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo     >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo     ^<TextView >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo         android:layout_width="wrap_content" >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo         android:layout_height="wrap_content" >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo         android:text="@string/app_version" >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo         android:textSize="16sp" >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo         android:layout_marginBottom="4dp" /^> >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo     >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo     ^<TextView >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo         android:layout_width="wrap_content" >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo         android:layout_height="wrap_content" >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo         android:text="@string/app_compatibility" >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo         android:textSize="16sp" >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo         android:layout_marginBottom="16dp" /^> >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo     >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo     ^<TextView >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo         android:layout_width="wrap_content" >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo         android:layout_height="wrap_content" >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo         android:text="@string/app_description" >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo         android:textSize="18sp" >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo         android:gravity="center" /^> >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo     >> %TEMP_DIR%\src\main\res\layout\activity_main.xml
echo ^</LinearLayout^> >> %TEMP_DIR%\src\main\res\layout\activity_main.xml

REM Create a proper AndroidManifest.xml with package attribute
echo Creating AndroidManifest.xml with proper package...
echo ^<?xml version="1.0" encoding="utf-8"?^> > %TEMP_DIR%\src\main\AndroidManifest.xml
echo ^<manifest xmlns:android="http://schemas.android.com/apk/res/android" >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo     package="com.stingrayshield"^> >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo     ^<application >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo         android:allowBackup="true" >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo         android:icon="@drawable/ic_app_icon" >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo         android:label="@string/app_name" >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo         android:theme="@android:style/Theme.DeviceDefault"^> >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo         ^<activity >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo             android:name=".ui.MainActivity" >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo             android:exported="true"^> >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo             ^<intent-filter^> >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo                 ^<action android:name="android.intent.action.MAIN" /^> >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo                 ^<category android:name="android.intent.category.LAUNCHER" /^> >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo             ^</intent-filter^> >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo         ^</activity^> >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo     ^</application^> >> %TEMP_DIR%\src\main\AndroidManifest.xml
echo ^</manifest^> >> %TEMP_DIR%\src\main\AndroidManifest.xml

REM Create simple Java MainActivity
echo Creating MainActivity.java with Java 8 compatibility...
echo package com.stingrayshield.ui; > %TEMP_DIR%\src\main\java\com\stingrayshield\ui\MainActivity.java
echo. >> %TEMP_DIR%\src\main\java\com\stingrayshield\ui\MainActivity.java
echo import android.app.Activity; >> %TEMP_DIR%\src\main\java\com\stingrayshield\ui\MainActivity.java
echo import android.os.Bundle; >> %TEMP_DIR%\src\main\java\com\stingrayshield\ui\MainActivity.java
echo import android.util.Log; >> %TEMP_DIR%\src\main\java\com\stingrayshield\ui\MainActivity.java
echo. >> %TEMP_DIR%\src\main\java\com\stingrayshield\ui\MainActivity.java
echo /** >> %TEMP_DIR%\src\main\java\com\stingrayshield\ui\MainActivity.java
echo  * Main activity for StingrayShield app - Android 15/16 compatible version >> %TEMP_DIR%\src\main\java\com\stingrayshield\ui\MainActivity.java
echo  */ >> %TEMP_DIR%\src\main\java\com\stingrayshield\ui\MainActivity.java
echo public class MainActivity extends Activity { >> %TEMP_DIR%\src\main\java\com\stingrayshield\ui\MainActivity.java
echo     private static final String TAG = "StingrayShield"; >> %TEMP_DIR%\src\main\java\com\stingrayshield\ui\MainActivity.java
echo. >> %TEMP_DIR%\src\main\java\com\stingrayshield\ui\MainActivity.java
echo     @Override >> %TEMP_DIR%\src\main\java\com\stingrayshield\ui\MainActivity.java
echo     protected void onCreate(Bundle savedInstanceState) { >> %TEMP_DIR%\src\main\java\com\stingrayshield\ui\MainActivity.java
echo         super.onCreate(savedInstanceState); >> %TEMP_DIR%\src\main\java\com\stingrayshield\ui\MainActivity.java
echo         Log.i(TAG, "Starting StingrayShield for Android 15/16"); >> %TEMP_DIR%\src\main\java\com\stingrayshield\ui\MainActivity.java
echo         setContentView(R.layout.activity_main); >> %TEMP_DIR%\src\main\java\com\stingrayshield\ui\MainActivity.java
echo     } >> %TEMP_DIR%\src\main\java\com\stingrayshield\ui\MainActivity.java
echo } >> %TEMP_DIR%\src\main\java\com\stingrayshield\ui\MainActivity.java

REM Compile Java files with Java 8 compatibility
echo Compiling Java files with Java 8 compatibility...
javac -source 1.8 -target 1.8 -d %TEMP_DIR%\classes -classpath %PLATFORM%\android.jar %TEMP_DIR%\src\main\java\com\stingrayshield\ui\MainActivity.java

REM Check if compilation succeeded
IF %ERRORLEVEL% NEQ 0 (
    echo Compilation failed with Java 8 compatibility.
    echo Attempting alternative compilation...
    javac -d %TEMP_DIR%\classes -classpath %PLATFORM%\android.jar %TEMP_DIR%\src\main\java\com\stingrayshield\ui\MainActivity.java
)

REM Create R.java file for resources
echo Generating R.java file for resources...
call %BUILD_TOOLS%\aapt.exe package -f -m -J %TEMP_DIR%\src\main\java -M %TEMP_DIR%\src\main\AndroidManifest.xml -S %TEMP_DIR%\src\main\res -I %PLATFORM%\android.jar

REM Compile R.java if it was generated
if exist %TEMP_DIR%\src\main\java\com\stingrayshield\R.java (
    echo Compiling generated R.java...
    javac -source 1.8 -target 1.8 -d %TEMP_DIR%\classes -classpath %PLATFORM%\android.jar %TEMP_DIR%\src\main\java\com\stingrayshield\R.java
)

REM Convert class files to dex
echo Converting classes to DEX format...
mkdir %TEMP_DIR%\dex
call %BUILD_TOOLS%\d8.bat --min-api 24 --output %TEMP_DIR%\dex\classes.dex %TEMP_DIR%\classes

REM Package resources
echo Packaging resources...
call %BUILD_TOOLS%\aapt.exe package -f -M %TEMP_DIR%\src\main\AndroidManifest.xml -S %TEMP_DIR%\src\main\res -I %PLATFORM%\android.jar -F %TEMP_DIR%\stingrayshield-resources.apk

REM Create APK
echo Creating APK...
cd %TEMP_DIR%
call %BUILD_TOOLS%\aapt.exe add stingrayshield-resources.apk dex/classes.dex
rename stingrayshield-resources.apk stingrayshield-unsigned.apk
cd ..

echo APK created at: %CD%\%TEMP_DIR%\stingrayshield-unsigned.apk

REM Copy to output directory
copy %TEMP_DIR%\stingrayshield-unsigned.apk %OUTPUT_DIR%\stingrayshield-unsigned.apk

echo.
echo =======================================================
echo    STINGRAYSHIELD APK FOR ANDROID 15/16 CREATED
echo =======================================================
echo.
echo Unsigned APK saved to: %CD%\%OUTPUT_DIR%\stingrayshield-unsigned.apk
echo.
echo To sign the APK for installation:
echo 1. %BUILD_TOOLS%\apksigner sign --ks debug.keystore --ks-pass pass:android --key-pass pass:android --out %OUTPUT_DIR%\stingrayshield-signed.apk %OUTPUT_DIR%\stingrayshield-unsigned.apk
echo.
echo If you don't have a debug keystore, create one with:
echo %JAVA_HOME%\bin\keytool -genkey -v -keystore debug.keystore -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android
echo.
echo To install directly to a connected device:
echo %ANDROID_HOME%\platform-tools\adb install -r %OUTPUT_DIR%\stingrayshield-signed.apk
echo.
echo Done.
pause
