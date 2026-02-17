package com.stingrayshield.di

import android.content.Context
import com.stingrayshield.data.repository.CellTowerRepository
import com.stingrayshield.data.repository.DetectionEventRepository
import com.stingrayshield.data.repository.StingrayDeviceRepository
import com.stingrayshield.util.AlarmManager
import com.stingrayshield.util.BackupRestoreManager
import com.stingrayshield.util.DataExportManager
import com.stingrayshield.util.DeviceControlManager
import com.stingrayshield.util.FirebaseManager
import com.stingrayshield.util.NetworkStatsHelper
import com.stingrayshield.util.TelephonyHelper
import com.stingrayshield.util.ThreatNotificationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for utility classes
 */
@Module
@InstallIn(SingletonComponent::class)
object UtilModule {
    
    /**
     * Provides AlarmManager instance
     */
    @Provides
    @Singleton
    fun provideAlarmManager(@ApplicationContext context: Context): AlarmManager {
        return AlarmManager(context)
    }
    
    /**
     * Provides DeviceControlManager instance
     */
    @Provides
    @Singleton
    fun provideDeviceControlManager(@ApplicationContext context: Context): DeviceControlManager {
        return DeviceControlManager(context)
    }
    
    /**
     * Provides TelephonyHelper instance
     */
    @Provides
    @Singleton
    fun provideTelephonyHelper(@ApplicationContext context: Context): TelephonyHelper {
        return TelephonyHelper(context)
    }
    
    /**
     * Provides ThreatNotificationManager instance
     */
    @Provides
    @Singleton
    fun provideThreatNotificationManager(@ApplicationContext context: Context): ThreatNotificationManager {
        return ThreatNotificationManager(context)
    }
    
    /**
     * Provides FirebaseManager instance with privacy settings
     */
    @Provides
    @Singleton
    fun provideFirebaseManager(@ApplicationContext context: Context): FirebaseManager {
        return FirebaseManager(context)
    }
    
    /**
     * Provides NetworkStatsHelper instance
     */
    @Provides
    @Singleton
    fun provideNetworkStatsHelper(@ApplicationContext context: Context): NetworkStatsHelper {
        return NetworkStatsHelper(context)
    }
    
    /**
     * Provides DataExportManager instance
     */
    @Provides
    @Singleton
    fun provideDataExportManager(@ApplicationContext context: Context): DataExportManager {
        return DataExportManager(context)
    }
    
    /**
     * Provides BackupRestoreManager instance
     */
    @Provides
    @Singleton
    fun provideBackupRestoreManager(
        @ApplicationContext context: Context,
        cellTowerRepository: CellTowerRepository,
        detectionEventRepository: DetectionEventRepository,
        stingrayDeviceRepository: StingrayDeviceRepository
    ): BackupRestoreManager {
        return BackupRestoreManager(context, cellTowerRepository, detectionEventRepository, stingrayDeviceRepository)
    }
}

