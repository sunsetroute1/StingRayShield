package com.stingrayshield.ui.screens.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stingrayshield.data.repository.StingrayDeviceRepository
import com.stingrayshield.domain.model.StingrayDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StingrayDevicesViewModel @Inject constructor(
    private val stingrayDeviceRepository: StingrayDeviceRepository
) : ViewModel() {
    
    private val _devices = MutableStateFlow<List<StingrayDevice>>(emptyList())
    val devices: StateFlow<List<StingrayDevice>> = _devices.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadDevices()
    }
    
    private fun loadDevices() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                stingrayDeviceRepository.getAllDevices().collect { deviceList ->
                    _devices.value = deviceList
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                android.util.Log.e("StingrayDevicesViewModel", "Error loading devices", e)
                _isLoading.value = false
            }
        }
    }
    
    fun deleteDevice(deviceId: Long) {
        viewModelScope.launch {
            try {
                val device = stingrayDeviceRepository.getDeviceById(deviceId)
                device?.let {
                    stingrayDeviceRepository.deleteDevice(it)
                }
            } catch (e: Exception) {
                android.util.Log.e("StingrayDevicesViewModel", "Error deleting device", e)
            }
        }
    }
    
    fun confirmDevice(deviceId: Long) {
        viewModelScope.launch {
            try {
                val device = stingrayDeviceRepository.getDeviceById(deviceId)
                device?.let {
                    stingrayDeviceRepository.updateDevice(it.copy(isConfirmed = true))
                }
            } catch (e: Exception) {
                android.util.Log.e("StingrayDevicesViewModel", "Error confirming device", e)
            }
        }
    }
    
    fun markAsFalsePositive(deviceId: Long) {
        viewModelScope.launch {
            try {
                val device = stingrayDeviceRepository.getDeviceById(deviceId)
                device?.let {
                    stingrayDeviceRepository.updateDevice(it.copy(isFalsePositive = true))
                }
            } catch (e: Exception) {
                android.util.Log.e("StingrayDevicesViewModel", "Error marking false positive", e)
            }
        }
    }
}

