package com.scazzumvivendi.seento.ui.wearable

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.scazzumvivendi.seento.data.ble.BleDeviceScanner
import com.scazzumvivendi.seento.data.ble.MdsMusicClient

class WearableViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return WearableViewModel(
            scanner = BleDeviceScanner(context.applicationContext),
            mdsClient = MdsMusicClient(context.applicationContext),
            preferences = context.applicationContext.getSharedPreferences("seento_preferences", Context.MODE_PRIVATE),
            context = context.applicationContext
        ) as T
    }
}
