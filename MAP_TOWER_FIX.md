# Cell Tower Location Bug Fix

## Problem Summary
Cell towers were being displayed at incorrect physical locations on the map. They were using a simplistic circular distribution around the user's location rather than realistic position estimation based on signal strength.

## Root Cause Analysis

### Issue 1: Hardcoded Circular Placement
**File:** `MapViewModel.kt` (lines 149-153)

The old code was spreading towers in a simple circle:
```kotlin
val angle = (index * 60.0) * (Math.PI / 180.0)
val radius = 0.0004 // About 40 meters (static distance)
val towerLat = latitude + (radius * kotlin.math.cos(angle))
val towerLon = longitude + (radius * kotlin.math.sin(angle))
```

**Problems:**
- Used only the tower index for angle calculation, making placement arbitrary
- Fixed radius of ~40 meters for all towers regardless of signal strength
- Ignored the sophisticated `CellTowerLocationEstimator` utility that was already implemented
- Towers didn't reflect their actual distance from the user based on signal strength

### Issue 2: Unused CellTowerLocationEstimator
**File:** `CellTowerLocationEstimator.kt`

The codebase already had a complete location estimation system that:
- Calculates tower distance using path loss models (free-space, urban, indoor)
- Uses different reference signals for each network type (2G/3G/4G/5G)
- Applies network-specific path loss exponents for accurate distance estimation
- Uses cell ID to determine consistent angular direction (so same tower always appears in same spot)
- Converts distance to latitude/longitude offsets with proper Earth radius calculations

But it was completely ignored by the MapViewModel.

## Solution Implemented

### Import CellTowerLocationEstimator
Added import statement to MapViewModel:
```kotlin
import com.stingrayshield.util.CellTowerLocationEstimator
```

### Replace Position Calculation for All Network Types

Changed from hardcoded circular placement to intelligent position estimation for each cell type:

**LTE (4G):**
```kotlin
val (towerLat, towerLon) = CellTowerLocationEstimator.estimateTowerPosition(
    latitude, longitude, sig.dbm, "LTE", cellId
)
```

**WCDMA (3G):**
```kotlin
val (towerLat, towerLon) = CellTowerLocationEstimator.estimateTowerPosition(
    latitude, longitude, sig.dbm, "3G", cellId
)
```

**GSM (2G):**
```kotlin
val (towerLat, towerLon) = CellTowerLocationEstimator.estimateTowerPosition(
    latitude, longitude, sig.dbm, "2G", cellId
)
```

**CDMA:**
```kotlin
val (towerLat, towerLon) = CellTowerLocationEstimator.estimateTowerPosition(
    latitude, longitude, sig.dbm, "CDMA", id.basestationId
)
```

**5G NR:**
```kotlin
val (towerLat, towerLon) = CellTowerLocationEstimator.estimateTowerPosition(
    latitude, longitude, sig.dbm, "5G", nci.toInt()
)
```

## How the Fix Works

### Signal-Based Distance Estimation
The estimator uses the path loss formula:
```
signalStrength = referenceSignal - 10 * n * log10(d/d0)
Solving for d: d = d0 * 10^((referenceSignal - signalStrength) / (10 * n))
```

Where:
- **referenceSignal**: Reference signal at 1 meter (varies by network type)
- **n**: Path loss exponent (2.0 for free space, 2.7-3.5 for urban, 3.0-5.0 for indoor)
- **d**: Calculated distance in meters
- **Clamped range**: 50 meters (min) to 35 km (max) for realistic tower distances

### Consistent Angular Direction
```kotlin
val angle = (cellId % 360) * (PI / 180.0)
```

Uses the cell ID modulo 360 to generate a consistent angle. This means:
- Same tower ID always appears in the same direction
- Different towers get different angular positions
- Much more realistic distribution

### Accurate Geographic Conversion
```kotlin
val latOffset = (distance * cos(angle)) / 111320.0
val lngOffset = (distance * sin(angle)) / (111320.0 * cos(userLat * PI / 180.0))
```

Converts distance in meters to geographic offsets using:
- 1 degree latitude ≈ 111,320 meters
- 1 degree longitude ≈ 111,320 * cos(latitude) meters

## Network-Specific Parameters

The estimator uses different parameters for each network type to account for their propagation characteristics:

| Network | Reference Signal @ 1m | Path Loss Exponent | Frequency Range |
|---------|----------------------|-------------------|-----------------|
| 5G NR   | -40 dBm             | 2.5               | Sub-6 GHz / mmWave |
| 4G LTE  | -45 dBm             | 3.0               | 700-2600 MHz    |
| 3G UMTS | -50 dBm             | 3.2               | 850-2100 MHz    |
| 2G GSM  | -55 dBm             | 3.5               | 850-1900 MHz    |
| CDMA    | -50 dBm             | 3.0               | 800-1900 MHz    |

## Expected Improvements

✅ **Stronger signal towers** appear closer to the user's location
✅ **Weaker signal towers** appear further away
✅ **Same tower ID** always appears in the same direction
✅ **Realistic distances** based on cellular propagation models
✅ **Network-aware** positioning (5G behaves differently than 2G)
✅ **Better visual distribution** on the map for detection analysis

## Testing Recommendations

1. **Move to different locations** and verify tower positions adjust accordingly
2. **Compare signal strength** with displayed distance (stronger = closer)
3. **Check consistency** - same towers should always be in similar positions relative to you
4. **Test different network types** (LTE, 5G, etc.) to see different distance characteristics
5. **Verify map patterns** - primary/serving towers should typically be closer than neighbor cells

## Files Modified

- `app/src/main/java/com/stingrayshield/ui/screens/map/MapViewModel.kt`
  - Added import for `CellTowerLocationEstimator`
  - Updated `createCellTower()` function for all network types (LTE, WCDMA, GSM, CDMA, NR)

## Build Status

✅ Build successful with no compilation errors
✅ All deprecation warnings are pre-existing (unrelated to these changes)
