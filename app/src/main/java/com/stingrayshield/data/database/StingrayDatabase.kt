package com.stingrayshield.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.stingrayshield.domain.model.CellTower
import com.stingrayshield.domain.model.DetectionEvent
import com.stingrayshield.domain.model.StingrayDevice

/**
 * Main database class for the StingrayShield application
 */
@Database(
    entities = [CellTower::class, DetectionEvent::class, StingrayDevice::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StingrayDatabase : RoomDatabase() {
    
    abstract fun cellTowerDao(): CellTowerDao
    abstract fun detectionEventDao(): DetectionEventDao
    abstract fun stingrayDeviceDao(): StingrayDeviceDao
    
    companion object {
        const val DATABASE_NAME = "stingray_shield.db"
        
        // Migration from version 1 to 2 (adding StingrayDevice table)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create StingrayDevice table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS stingray_devices (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        detectionTimestamp INTEGER NOT NULL,
                        threatLevel TEXT NOT NULL,
                        detectionEventId INTEGER,
                        cellId INTEGER,
                        locationAreaCode INTEGER,
                        mobileCountryCode TEXT,
                        mobileNetworkCode TEXT,
                        networkType TEXT,
                        signalStrength INTEGER,
                        rsrp INTEGER,
                        rsrq INTEGER,
                        sinr INTEGER,
                        rssi INTEGER,
                        arfcn INTEGER,
                        uarfcn INTEGER,
                        psc INTEGER,
                        pci INTEGER,
                        nci INTEGER,
                        tac INTEGER,
                        hardwareVendor TEXT,
                        hardwareModel TEXT,
                        firmwareVersion TEXT,
                        deviceFingerprint TEXT,
                        softwareVersion TEXT,
                        protocolVersion TEXT,
                        encryptionType TEXT,
                        cipheringIndicator INTEGER,
                        behaviorPattern TEXT,
                        anomalyTypes TEXT,
                        latitude REAL,
                        longitude REAL,
                        locationAccuracy REAL,
                        notes TEXT,
                        isConfirmed INTEGER NOT NULL,
                        isFalsePositive INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
        
        // Migration from version 2 to 3 (adding indices for cell tower queries)
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create indices for improved query performance
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_cell_towers_cellId` ON `cell_towers` (`cellId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_cell_towers_isPrimary` ON `cell_towers` (`isPrimary`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_cell_towers_timestamp` ON `cell_towers` (`timestamp`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_cell_towers_isPrimary_cellId` ON `cell_towers` (`isPrimary`, `cellId`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_cell_towers_isPrimary_timestamp` ON `cell_towers` (`isPrimary`, `timestamp`)")
            }
        }
        
        // Migration from version 3 to 4 (adding real tower location fields)
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add columns for real tower location from public databases
                database.execSQL("ALTER TABLE cell_towers ADD COLUMN towerLatitude REAL")
                database.execSQL("ALTER TABLE cell_towers ADD COLUMN towerLongitude REAL")
                database.execSQL("ALTER TABLE cell_towers ADD COLUMN towerRange INTEGER")
                database.execSQL("ALTER TABLE cell_towers ADD COLUMN locationSource TEXT")
            }
        }
    }
}
