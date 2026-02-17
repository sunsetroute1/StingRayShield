package com.stingrayshield.service

import android.content.Context
import com.stingrayshield.data.repository.CellTowerRepository
import com.stingrayshield.data.repository.DetectionEventRepository
import com.stingrayshield.data.repository.StingrayDeviceRepository
import com.stingrayshield.detection.StingrayDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Hilt module to provide dependencies for the DetectorService and app-wide detection
 */
@Module
@InstallIn(SingletonComponent::class)
object DetectorServiceModule {

    @Singleton
    @Provides
    fun provideDetectorCoroutineScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
    
    @Singleton
    @Provides
    fun provideStingrayDetector(
        cellTowerRepository: CellTowerRepository,
        detectionEventRepository: DetectionEventRepository,
        stingrayDeviceRepository: StingrayDeviceRepository,
        detectorScope: CoroutineScope
    ): StingrayDetector {
        return StingrayDetector(
            cellTowerRepository,
            detectionEventRepository,
            stingrayDeviceRepository,
            detectorScope
        )
    }
}
