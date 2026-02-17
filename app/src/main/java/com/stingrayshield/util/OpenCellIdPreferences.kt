package com.stingrayshield.util

/**
 * Single source for OpenCellID API key storage.
 *
 * The OpenCellID API key is stored in app Settings and used everywhere OpenCellID is called:
 * - [com.stingrayshield.data.api.CellTowerLocationService]: single-cell API (cell/get),
 *   area API (cell/getInArea), and CSV download URLs (ocid/downloads?token=...).
 *
 * When the user changes the API key in Settings, all of these use the new key on the next
 * request (no in-memory cache of the key). Keep [PREFERENCES_NAME] and [KEY_OPENCELLID_API_KEY]
 * in sync with [com.stingrayshield.ui.screens.settings.SettingsViewModel].
 */
object OpenCellIdPreferences {
    /** SharedPreferences name used for app settings (must match SettingsViewModel). */
    const val PREFERENCES_NAME = "stingray_shield_settings"

    /** Key for the OpenCellID API token (same token used for API and CSV downloads). */
    const val KEY_OPENCELLID_API_KEY = "opencellid_api_key"
    /** Key for CSV refresh interval in days (default 30 = monthly). Used for tower map data. */
    const val KEY_CSV_REFRESH_INTERVAL_DAYS = "csv_refresh_interval_days"
}
