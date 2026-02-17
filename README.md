# StingrayShield Android App

StingrayShield is an advanced Android application designed to detect IMSI-catchers (also known as "stingrays") through sophisticated analysis of cellular network parameters. The app provides real-time monitoring and alerts when suspicious cellular activity is detected.

## Features

- **Advanced 4G/5G Stingray Detection**: Uses multiple detection methods to identify cellular anomalies specific to LTE and 5G NR networks
- **Real-time Monitoring**: Continuous background service monitors cellular network parameters
- **Comprehensive UI**: Modern Material 3 design with Jetpack Compose
- **Detailed Event History**: Records and displays all detection events with time, location, and details
- **Cell Tower Database**: Maintains history of observed cell towers for enhanced detection accuracy
- **Configurable Settings**: Customize detection sensitivity, notification preferences, and app behavior
- **Android 15 Ready**: Built with compatibility for the latest Android 15 and Samsung OneUI 7.0

## Detection Methods

StingrayShield employs multiple detection strategies:

1. **Signal Strength Anomalies**: Identifies sudden or unusual changes in signal strength
2. **Cell Tower Information Analysis**: Detects inconsistencies in cell tower identifiers (MCC, MNC, LAC, CID)
3. **Neighbor Cell Monitoring**: Tracks unusual changes in neighboring cell information
4. **Timing Advance Analysis**: Identifies discrepancies in cell tower distances
5. **Silent SMS Detection**: Monitors for hidden SMS messages used for tracking
6. **Femtocell Detection**: Identifies suspicious small cells that may be used for interception
7. **5G-to-4G Downgrade Analysis**: Detects suspicious network downgrade attacks
8. **Encryption Monitoring**: Identifies encryption downgrades that may indicate MITM attacks

## Technical Requirements

- Android 7.0+ (API level 24)
- Location permissions
- Telephony state permissions
- Notification permissions
- Foreground service permissions

## Build Requirements

See the included `build_instructions.md` file for detailed steps on building and signing the APK with Android Studio.

## Privacy

All detection and analysis happens locally on your device. No sensitive cellular data is transmitted to external servers. Detection events can be optionally saved to your device's storage.

## Important Note

While StingrayShield implements advanced detection algorithms based on known IMSI-catcher behaviors, it cannot guarantee detection of all surveillance devices. Government and law enforcement stingrays may use sophisticated techniques to avoid detection.
