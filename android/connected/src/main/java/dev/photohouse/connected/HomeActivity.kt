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
import dev.photohouse.home.*

/** No account, bearer, cookie or protected-library adapter is reachable here. */
class HomePhoneViewModel(api: HomeApi?) : ViewModel() {
    val store = api?.let { HomeStore(it, viewModelScope) }
    override fun onCleared() { store?.background() }
}
class HomeActivity : ComponentActivity() {
    private val model by viewModels<HomePhoneViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val api = runCatching {
                    // Require the private LAN override: no fallback to public DNS or account origin.
                    HttpsCatalogApi(HomeOrigin.parse(BuildConfig.PHOTOHOUSE_HOME_ORIGIN),
                        HomeLanAddress.parse(BuildConfig.PHOTOHOUSE_HOME_LAN_ADDRESS), version = 3, browseEnabled = true)
                }.getOrNull()
                return HomePhoneViewModel(api) as T
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        window.decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        setContent { HomePhoneApp(model.store) { finish() } }
    }
    override fun onPause() { model.store?.background(); super.onPause() }
    override fun onResume() { super.onResume(); model.store?.foreground() }
}
