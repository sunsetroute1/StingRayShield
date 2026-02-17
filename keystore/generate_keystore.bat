@echo off
echo Generating keystore for StingrayShield...

set KEYSTORE_FILE=stingrayshield.keystore
set KEY_ALIAS=stingrayshield
set STORE_PASSWORD=stingrayshield
set KEY_PASSWORD=stingrayshield

REM Change these values for production use
set NAME="StingrayShield"
set ORGANIZATIONAL_UNIT="Development"
set ORGANIZATION="StingrayShield"
set CITY="San Francisco"
set STATE="California"
set COUNTRY="US"

REM Generate the keystore
keytool -genkeypair -v ^
  -keystore %KEYSTORE_FILE% ^
  -alias %KEY_ALIAS% ^
  -keyalg RSA ^
  -keysize 2048 ^
  -validity 10000 ^
  -storepass %STORE_PASSWORD% ^
  -keypass %KEY_PASSWORD% ^
  -dname "CN=%NAME%, OU=%ORGANIZATIONAL_UNIT%, O=%ORGANIZATION%, L=%CITY%, ST=%STATE%, C=%COUNTRY%"

echo Keystore generated: %KEYSTORE_FILE%
echo.
echo Note: For production apps, use a secure password and keep your keystore file safe!
