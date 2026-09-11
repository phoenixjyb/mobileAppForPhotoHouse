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
import dev.photohouse.home.*

class TvViewModel(api: HomeApi?) : ViewModel() {
    val store = api?.let { HomeStore(it, viewModelScope) }
}
class MainActivity : ComponentActivity() {
    private val model by viewModels<TvViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val api = runCatching {
                    val origin = HomeOrigin.parse(BuildConfig.PHOTOHOUSE_ORIGIN)
                    when (BuildConfig.PHOTOHOUSE_CATALOG_VERSION) {
                        1 -> if (BuildConfig.PHOTOHOUSE_LAN_ADDRESS.isEmpty()) HttpsHomeApi(origin)
                            else HttpsHomeApi(origin, HomeLanAddress.parse(BuildConfig.PHOTOHOUSE_LAN_ADDRESS))
                        2 -> if (BuildConfig.PHOTOHOUSE_LAN_ADDRESS.isEmpty()) HttpsCatalogApi(origin)
                            else HttpsCatalogApi(origin, HomeLanAddress.parse(BuildConfig.PHOTOHOUSE_LAN_ADDRESS))
                        else -> error("Unsupported catalog")
                    }
                }.getOrNull()
                return TvViewModel(api) as T
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
