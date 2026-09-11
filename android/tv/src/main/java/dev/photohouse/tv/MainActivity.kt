package dev.photohouse.tv

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

class TvViewModel(api: PhotoHouseApi?) : ViewModel() {
    val store = api?.let { ConnectedStore(it, viewModelScope) }
}
class MainActivity : ComponentActivity() {
    private val model by viewModels<TvViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val origin = runCatching { TrustedOrigin.parse(BuildConfig.PHOTOHOUSE_ORIGIN) }.getOrNull()
                return TvViewModel(origin?.let { HttpsPhotoHouseApi(it, detailPreviewSize = 1024) }) as T
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        window.decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        setContent { TvApp(model.store) }
    }
    override fun onPause() { model.store?.background(); super.onPause() }
    override fun onResume() { super.onResume(); model.store?.foreground() }
}
