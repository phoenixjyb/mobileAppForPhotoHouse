package dev.photohouse.connected

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.photohouse.connected.core.*

class ConnectedViewModel(api: PhotoHouseApi?) : ViewModel() {
    val store = api?.let { ConnectedStore(it, viewModelScope) }
}
class MainActivity : ComponentActivity() {
    private val model by viewModels<ConnectedViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val origin = runCatching { TrustedOrigin.parse(BuildConfig.PHOTOHOUSE_ORIGIN) }.getOrNull()
                return ConnectedViewModel(origin?.let { HttpsPhotoHouseApi(it,
                    detailPreviewSize = if (BuildConfig.PHOTOHOUSE_PROTECTED_NATIVE_V2_ENABLED) 1024 else 256,
                    protectedNativeV2Enabled = BuildConfig.PHOTOHOUSE_PROTECTED_NATIVE_V2_ENABLED,
                    discoveryEnabled = BuildConfig.PHOTOHOUSE_DISCOVERY_ENABLED,
                    photoDeliveryEnabled = BuildConfig.PHOTOHOUSE_PHOTO_DELIVERY_ENABLED) }) as T
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        window.decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        setContent { ConnectedApp(model.store) }
    }
    override fun onPause() { model.store?.background(); super.onPause() }
    override fun onResume() { super.onResume(); model.store?.foreground() }
}
