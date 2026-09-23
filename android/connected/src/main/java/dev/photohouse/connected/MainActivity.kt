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
import android.net.Uri
import dev.photohouse.connected.core.*

class ConnectedViewModel(api: PhotoHouseApi?, persistence: SessionPersistence? = null, uploadNetwork: () -> UploadNetwork = { UploadNetwork.UNKNOWN }, batchPersistence: UploadQueuePersistence? = null, batchSource: ((UploadQueueRecord) -> BatchUploadSource?)? = null) : ViewModel() {
    val store = api?.let { ConnectedStore(it, viewModelScope, persistence, uploadNetwork = uploadNetwork, batchPersistence = batchPersistence, batchSource = batchSource).also { store -> store.restoreSession() } }
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
                    photoDeliveryEnabled = BuildConfig.PHOTOHOUSE_PHOTO_DELIVERY_ENABLED,
                    preparedVideoEnabled = BuildConfig.PHOTOHOUSE_PREPARED_VIDEO_ENABLED,
                    mediaFilterEnabled = BuildConfig.PHOTOHOUSE_MEDIA_FILTER_ENABLED,
                    preparedBrowseEnabled = BuildConfig.PHOTOHOUSE_PREPARED_BROWSE_ENABLED,
                    uploadEnabled = BuildConfig.PHOTOHOUSE_UPLOAD_ENABLED) },
                    origin?.let { KeystoreSessionPersistence(applicationContext, BuildConfig.PHOTOHOUSE_ORIGIN) },
                    uploadNetwork = { uploadNetwork(applicationContext) },
                    batchPersistence = applicationContext.getSharedPreferences("photohouse_upload_queue", MODE_PRIVATE).let(::SharedPreferencesUploadQueuePersistence),
                    batchSource = { record -> record.locator?.let { batchUploadSource(applicationContext, Uri.parse(it)) } }) as T
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
