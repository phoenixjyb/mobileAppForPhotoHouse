package dev.photohouse.connected

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.photohouse.connected.core.UploadState
import dev.photohouse.connected.core.UploadStore

/** The panel owns presentation only; the activity owns the SAF picker and URI permission. */
@Composable
internal fun UploadPanel(store: UploadStore, zh: Boolean, onPick: () -> Unit, onClose: () -> Unit) {
    val state by store.state.collectAsState()
    UploadPanelState(state, zh, onPick, onClose, store::approveNetwork, store::retry, store::cancel)
}

@Composable
internal fun UploadPanelState(
    state: UploadState, zh: Boolean, onPick: () -> Unit, onClose: () -> Unit,
    onApproveNetwork: () -> Unit, onRetry: () -> Unit, onCancel: () -> Unit,
) {
    fun t(en: String, cn: String) = if (zh) cn else en
    Card(Modifier.fillMaxWidth().testTag("upload-panel")) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(t("Add a photo", "添加照片"), style = MaterialTheme.typography.titleLarge)
            when (state) {
                UploadState.Idle, UploadState.Cancelled -> {
                    Text(t("Choose one JPEG or PNG. It will be sent to your incoming folder for review.", "选择一张 JPEG 或 PNG。照片会先发送到你的待审核收件夹。"))
                    Text(t("Memories can be attached after the photo is promoted into a library.", "照片进入资料库并完成审核后，才能附加回忆。"), style = MaterialTheme.typography.bodySmall)
                    Button(onClick = onPick, modifier = Modifier.testTag("upload-pick")) { Text(t("Choose photo", "选择照片")) }
                }
                is UploadState.AwaitingNetwork -> {
                    Text(if (state.network.name == "METERED") t("This uses mobile data. Continue?", "当前使用移动数据，是否继续？") else t("The network type is unknown. Continue only if you accept possible data charges.", "无法确认网络类型。继续前请确认可能产生流量费用。"), modifier = Modifier.testTag("upload-network-warning"))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onApproveNetwork, modifier = Modifier.testTag("upload-network-approve")) { Text(t("Continue", "继续")) }
                        OutlinedButton(onClick = onClose) { Text(t("Later", "稍后")) }
                    }
                }
                is UploadState.Uploading -> {
                    Text(t("Uploading ${state.sent} of ${state.total} bytes", "正在上传 ${state.sent} / ${state.total} 字节"), modifier = Modifier.testTag("upload-progress"))
                    LinearProgressIndicator({ (state.sent.toFloat() / state.total.coerceAtLeast(1)).coerceIn(0f, 1f) }, Modifier.fillMaxWidth())
                    OutlinedButton(onClick = onCancel, modifier = Modifier.testTag("upload-cancel")) { Text(t("Cancel", "取消")) }
                }
                is UploadState.Failed -> {
                    Text(t("The upload was interrupted. The server may already have received it; retry only if needed.", "上传中断。服务器可能已经收到照片；确认后再手动重试。"), color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("upload-failed"))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.retryAvailable) Button(onClick = onRetry, modifier = Modifier.testTag("upload-retry")) { Text(t("Retry", "重试")) }
                        OutlinedButton(onClick = onClose) { Text(t("Close", "关闭")) }
                    }
                }
                is UploadState.Succeeded -> {
                    Text(t("Received for review", "已收到，等待审核"), modifier = Modifier.testTag("upload-received"))
                    Text(t("Incoming item ${state.receipt.assetId} · ${state.receipt.tasksEnqueued} processing tasks queued. This does not mean processing is complete.", "待审核项目 ${state.receipt.assetId} · 已排队 ${state.receipt.tasksEnqueued} 个处理任务。任务尚未完成。"), style = MaterialTheme.typography.bodySmall)
                    Text(t("Memories are unavailable until an operator promotes this photo into a library.", "管理员将照片加入资料库后，才能附加回忆。"), style = MaterialTheme.typography.bodySmall)
                    Button(onClick = onClose, modifier = Modifier.testTag("upload-done")) { Text(t("Done", "完成")) }
                }
            }
        }
    }
}
