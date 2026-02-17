package com.stingrayshield.di

import android.content.Context
import androidx.room.Room
import com.stingrayshield.data.database.CellTowerDao
import com.stingrayshield.data.database.DetectionEventDao
import com.stingrayshield.data.database.StingrayDeviceDao
import com.stingrayshield.data.database.StingrayDatabase
import com.stingrayshield.data.repository.StingrayDeviceRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for database-related components
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the Room database instance as a singleton
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StingrayDatabase {
        return Room.databaseBuilder(
            context,
            StingrayDatabase::class.java,
            StingrayDatabase.DATABASE_NAME
        )
            .addMigrations(
                StingrayDatabase.MIGRATION_1_2,
                StingrayDatabase.MIGRATION_2_3,
                StingrayDatabase.MIGRATION_3_4
            )
            .fallbackToDestructiveMigration()
            .build()
    }

    /**
     * Provides the CellTowerDao
     */
    @Provides
    @Singleton
    fun provideCellTowerDao(database: StingrayDatabase): CellTowerDao {
        return database.cellTowerDao()
    }

    /**
     * Provides the DetectionEventDao
     */
    @Provides
    @Singleton
    fun provideDetectionEventDao(database: StingrayDatabase): DetectionEventDao {
        return database.detectionEventDao()
    }
    
    /**
     * Provides the StingrayDeviceDao
     */
    @Provides
    @Singleton
    fun provideStingrayDeviceDao(database: StingrayDatabase): StingrayDeviceDao {
        return database.stingrayDeviceDao()
    }
    
    /**
     * Provides StingrayDeviceRepository
     */
    @Provides
    @Singleton
    fun provideStingrayDeviceRepository(
        stingrayDeviceDao: StingrayDeviceDao
    ): StingrayDeviceRepository {
        return StingrayDeviceRepository(stingrayDeviceDao)
    }
}
