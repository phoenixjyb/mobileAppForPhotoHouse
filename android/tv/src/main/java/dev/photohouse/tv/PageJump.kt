package dev.photohouse.tv

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

/** Remote-only page selection keeps even a large catalog reachable without typing. */
@Composable internal fun PageJump(current: Int, total: Int, zh: Boolean, dismiss: () -> Unit, go: (Int) -> Unit) {
    fun t(en: String, cn: String) = if (zh) cn else en
    val last = ((total + 49) / 50).coerceAtLeast(1)
    var target by remember(current, last) { mutableStateOf(current.coerceIn(1, last)) }
    val first = remember { FocusRequester() }
    AlertDialog(onDismissRequest = dismiss,
        title = { Text(t("Go to page", "跳转页面")) },
        text = { Column {
            Text(t("Page $target of $last", "第 $target 页，共 $last 页"), Modifier.testTag("page-target"))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TvButton(t("First", "首页"), Modifier.testTag("page-first")) { target = 1 }
                TvButton("−10") { target = (target - 10).coerceAtLeast(1) }
                TvButton("−1") { target = (target - 1).coerceAtLeast(1) }
                TvButton("+1") { target = (target + 1).coerceAtMost(last) }
                TvButton("+10") { target = (target + 10).coerceAtMost(last) }
                TvButton(t("Last", "末页"), Modifier.testTag("page-last")) { target = last }
            }
        } },
        dismissButton = { TvButton(t("Cancel", "取消")) { dismiss() } },
        confirmButton = {
            TvButton(t("Go", "跳转"), Modifier.testTag("page-go").focusRequester(first)) { go(target) }
            val focusedWindow = LocalWindowInfo.current.isWindowFocused
            LaunchedEffect(focusedWindow) {
                if (focusedWindow) { withFrameNanos { }; first.requestFocus() }
            }
        })
}
