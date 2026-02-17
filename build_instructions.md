# StingrayShield APK Build Instructions

## Build Instructions for Android 15 and Samsung OneUI 7.0 Compatibility

We've successfully integrated the advanced detection algorithms with the UI components in your StingrayShield app. The app is now ready to be built with compatibility for Android 15 and Samsung OneUI 7.0.

### Building the APK using Android Studio

1. **Generate the keystore for APK signing**
   - Navigate to `C:\Users\sunse\CascadeProjects\StingrayShield\keystore`
   - Run the `generate_keystore.bat` script by double-clicking it
   - This will create a file named `stingrayshield.keystore` in the keystore directory
   - Note: For production use, use a secure password and store the keystore file safely

2. **Open the project in Android Studio**
   - Launch Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to `C:\Users\sunse\CascadeProjects\StingrayShield` and click "OK"

3. **Allow Gradle sync to complete**
   - Android Studio will automatically detect the project's configuration
   - Wait for the Gradle sync to complete (this may take a few minutes)

4. **Build the signed APK**
   - From the menu bar, select Build → Generate Signed Bundle / APK
   - Select APK
   - In the signing configuration:
     - Select the keystore file at `C:\Users\sunse\CascadeProjects\StingrayShield\keystore\stingrayshield.keystore`
     - Enter the keystore password: `stingrayshield`
     - Enter the key alias: `stingrayshield`
     - Enter the key password: `stingrayshield`
   - Choose release build variant
   - Enable both V1 and V2 signature versions
   - Click Finish to build the APK

5. **Locate the APK**
   - Android Studio will show a notification when the build is finished
   - The signed APK will be located at `C:\Users\sunse\CascadeProjects\StingrayShield\app\release\app-release.apk`

### Project Configuration Summary

The project is already configured for Android 15 and Samsung OneUI 7.0 compatibility:

- **Compile SDK and Target SDK**: Set to 35 (Android 15)
- **Minimum SDK**: Set to 24 for broader device support
- **Java Compatibility**: Set to Java 17
- **Dependencies**: All necessary libraries are up to date

### Features Implemented

1. **Advanced Detection Algorithms**
   - 4G LTE-specific detection methods
   - 5G NR-specific detection methods
   - Signal strength anomaly detection
   - Tower information inconsistency detection
   - Neighbor cell anomaly detection
   - Silent SMS detection
   - Femtocell detection
   - Encryption downgrade detection

2. **UI Integration**
   - Real-time updates to UI components
   - Status broadcasts from the detector service
   - Enhanced notifications with threat levels
   - Settings configuration for different detection methods

### Testing the APK

After building, you can install and test the APK on:
- Android 15 devices
- Samsung devices running OneUI 7.0
- Any Android device with API level 24 or higher (though advanced detection features may be limited on older versions)

The app will automatically adapt to the capabilities of the device it's running on, with full features available on Android 15 and OneUI 7.0.
