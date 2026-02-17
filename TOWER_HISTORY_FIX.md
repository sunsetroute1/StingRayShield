# Cell Tower History Loading Fix

## Problem
When navigating the map, only the currently connected cell towers (2 towers) were being displayed. Previously observed towers were not being shown, even though they were stored in the database.

## Root Causes

### Issue 1: No Historical Tower Loading
The `MapViewModel` had no mechanism to load previously observed towers from the database. It only displayed towers from the current real-time scan.

### Issue 2: Towers Not Being Saved
Even when towers were scanned, they were not being persisted to the database, so there was no history to retrieve.

### Issue 3: Real-time Scan Replaced Everything
Each location scan would replace the entire tower list instead of merging with existing data.

## Solution Implemented

### 1. Added Historical Tower Loading Function
```kotlin
private fun loadHistoricalTowers() {
    viewModelScope.launch {
        try {
            // Subscribe to all cell towers from database
            cellTowerRepository.getAllCellTowers().collect { towers ->
                // Get current state
                val currentState = _uiState.value
                
                // If we have real-time scan results, combine with historical
                // Otherwise, just show all historical towers
                val displayTowers = if (currentState.cellTowers.isEmpty()) {
                    towers
                } else {
                    // Merge: keep real-time towers as primary, add unique historical ones
                    val realtimeIds = currentState.cellTowers.map { it.cellId }.toSet()
                    currentState.cellTowers + towers.filter { it.cellId !in realtimeIds }
                }
                
                if (displayTowers.isNotEmpty()) {
                    android.util.Log.d("MapViewModel", "Loaded ${towers.size} historical towers, displaying ${displayTowers.size} total")
                    _uiState.value = currentState.copy(cellTowers = displayTowers)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MapViewModel", "Error loading historical towers: ${e.message}")
        }
    }
}
```

**Key features:**
- Uses Flow collection to listen for database updates
- Dynamically loads all previously observed towers
- Merges real-time and historical data intelligently
- Shows only unique towers (no duplicates by cell ID)
- Logs the count of loaded and displayed towers

### 2. Added Tower Persistence on Scan
Modified `scanCellTowers()` to save towers to the database:

```kotlin
// Save scanned towers to database
if (towers.isNotEmpty()) {
    viewModelScope.launch {
        try {
            cellTowerRepository.addCellTowers(towers)
            android.util.Log.d("MapViewModel", "Saved ${towers.size} towers to database")
        } catch (e: Exception) {
            android.util.Log.e("MapViewModel", "Error saving towers: ${e.message}")
        }
    }
}
```

### 3. Updated Initialization Order
```kotlin
init {
    // Load saved stingrays from database
    loadStingrays()
    // Load historical towers from database
    loadHistoricalTowers()      // <- NEW
    // Get initial location and scan
    getLocationAndScan()
}
```

Now the app:
1. Loads previously detected stingrays
2. **Loads all previously observed towers**
3. Does a fresh real-time scan at the current location

### 4. Added Required Import
```kotlin
import kotlinx.coroutines.flow.collect
```

## How It Works

### Initialization Sequence
```
App Launch
    ↓
loadStingrays()
    ↓ (loads from database)
loadHistoricalTowers()
    ↓ (subscribes to database flow)
getLocationAndScan()
    ↓ (gets GPS location)
scanCellTowers()
    ↓ (scans current towers)
saveToDatabase()
    ↓ (persists to database)
Flow notifies historical loader
    ↓ (merges real-time + historical)
Map displays all towers
```

### Data Flow for Tower Display
1. **Historical towers** loaded as a Flow subscription
2. **Real-time scan** finds current towers and saves them
3. **Database notifies** the Flow subscriber of new towers
4. **Towers are merged** (real-time + historical unique ones)
5. **Map UI updates** with all available towers

### Deduplication Logic
```kotlin
val realtimeIds = currentState.cellTowers.map { it.cellId }.toSet()
currentState.cellTowers + towers.filter { it.cellId !in realtimeIds }
```

- Collects all cell IDs from real-time scans
- Adds historical towers that aren't in the real-time set
- Prevents duplicate towers on the map

## Expected Behavior After Fix

✅ **On first launch:**
- Shows historical towers from database (if any exist)
- Performs fresh scan at current location
- Adds current towers to display
- Saves new towers to database

✅ **When navigating:**
- Real-time scan happens if moved >100m
- New towers are saved to database
- All historical towers remain visible
- New towers are added to the map

✅ **When refreshing:**
- Re-scans current location
- Saves fresh data to database
- Merges with all historical observations

✅ **Over time:**
- Map becomes more complete as you explore
- All previously seen towers remain on the map
- Tower positions are re-estimated from your current location using signal strength

## Benefits

### Comprehensive Coverage
- See all towers ever observed, not just current ones
- Build a history of tower locations as you travel

### Better Analysis
- Visualize tower distribution across areas
- Identify towers that appear in multiple locations
- Detect anomalies by comparing with historical data

### Improved IMSI-Catcher Detection
- More data points for analysis
- Can correlate tower patterns with threat detection events
- Historical context for threat assessment

## Database Queries Used

The fix uses these repository methods:
- `cellTowerRepository.getAllCellTowers()` - Returns Flow of all towers with updates
- `cellTowerRepository.addCellTowers(towers)` - Saves new tower observations

## Logging Output

Watch the Android logs for debugging:
```
MapViewModel: Loaded 47 historical towers, displaying 49 total
MapViewModel: Scan complete: 2 towers found
MapViewModel: Saved 2 towers to database
```

## Files Modified

- `app/src/main/java/com/stingrayshield/ui/screens/map/MapViewModel.kt`
  - Added import for `collect`
  - Added `loadHistoricalTowers()` function
  - Updated `init {}` to call `loadHistoricalTowers()`
  - Modified `scanCellTowers()` to save towers to database

## Build Status

✅ Build successful
✅ No compilation errors
✅ One warning about unused `index` parameter (pre-existing, not critical)

## Testing Recommendations

1. **Check database content:**
   - Install app and navigate around
   - Verify towers are being saved to database

2. **Test tower display:**
   - Move to different location
   - Confirm historical towers from previous location still visible
   - Verify new towers are added from current scan

3. **Verify deduplication:**
   - Check map doesn't show duplicate towers with same cell ID
   - Confirm tower count in log matches displayed towers

4. **Test refresh:**
   - Use refresh button on map
   - Confirm it rescans and updates database without removing history

## Performance Notes

- Initial load of large tower history might take a few seconds
- Flow subscription updates efficiently only when data changes
- Deduplication ensures O(n) performance with reasonable tower counts
- Database queries are indexed by cell ID for fast lookups
