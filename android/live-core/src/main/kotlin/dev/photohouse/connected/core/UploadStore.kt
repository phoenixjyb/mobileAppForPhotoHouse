package dev.photohouse.connected.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

enum class UploadNetwork { UNMETERED, METERED, UNKNOWN }

sealed class UploadState {
    data object Idle : UploadState()
    data class AwaitingNetwork(val network: UploadNetwork) : UploadState()
    data class Uploading(val sent: Long, val total: Long) : UploadState()
    data class Succeeded(val receipt: UploadReceipt) : UploadState()
    data class Failed(val problem: LiveProblem, val retryAvailable: Boolean = true) : UploadState()
    data object Cancelled : UploadState()
}

/** One-at-a-time whole-file upload coordinator. It owns no bytes or disk cache. */
class UploadStore(
    private val api: PhotoHouseApi,
    private val token: Bearer,
    private val scope: CoroutineScope,
    private val valid: () -> Boolean,
    private val network: () -> UploadNetwork = { UploadNetwork.UNKNOWN },
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val mutable = MutableStateFlow<UploadState>(UploadState.Idle)
    val state = mutable.asStateFlow()
    private var job: Job? = null
    private var pending: Pending? = null
    private val epoch = AtomicLong(0)

    private data class Pending(val source: UploadSource, val batch: String)

    fun start(source: UploadSource, batch: String = UUID.randomUUID().toString().replace("-", ""),
              network: UploadNetwork, allowMetered: Boolean = false): Boolean {
        if (!api.uploadEnabled || !api.protectedNativeV2Enabled || job?.isActive == true || !valid()) return false
        require(batch.matches(Regex("[0-9a-f]{32}")))
        if (network != UploadNetwork.UNMETERED && !allowMetered) {
            pending = Pending(source, batch); mutable.value = UploadState.AwaitingNetwork(network); return true
        }
        pending = Pending(source, batch)
        launchPending()
        return true
    }

    /** Explicit user action after reviewing the metered/unknown-network warning. */
    fun approveNetwork(): Boolean {
        if (mutable.value !is UploadState.AwaitingNetwork || job?.isActive == true || !valid()) return false
        launchPending(); return true
    }

    /** Retry is always explicit; an interrupted request may already have created the receipt. */
    fun retry(): Boolean {
        val failed = mutable.value as? UploadState.Failed ?: return false
        if (!failed.retryAvailable || failed.problem.retryAtMillis > now() || job?.isActive == true || !valid()) return false
        val currentNetwork = network()
        if (currentNetwork != UploadNetwork.UNMETERED) {
            mutable.value = UploadState.AwaitingNetwork(currentNetwork)
            return true
        }
        launchPending(); return true
    }

    fun cancel() {
        epoch.incrementAndGet()
        job?.cancel()
        job = null
        pending = null
        mutable.value = UploadState.Cancelled
    }

    fun close() {
        epoch.incrementAndGet()
        job?.cancel()
        job = null
        pending = null
        mutable.value = UploadState.Idle
    }

    private fun launchPending() {
        val request = pending ?: return
        val attempt = epoch.incrementAndGet()
        job?.cancel()
        mutable.value = UploadState.Uploading(0, request.source.bytes)
        job = scope.launch {
            try {
                if (!valid()) throw ApiFailure(FailureKind.HTTP, 401)
                val receipt = api.uploadPhoto(token, request.source, request.batch) { sent ->
                    scope.launch { if (attempt == epoch.get() && valid()) mutable.value = UploadState.Uploading(sent, request.source.bytes) }
                }
                if (attempt == epoch.get() && valid()) { epoch.incrementAndGet(); mutable.value = UploadState.Succeeded(receipt) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                    if (attempt == epoch.get() && valid()) { epoch.incrementAndGet(); mutable.value = UploadState.Failed(problem(error), retryAvailable = retryable(error)) }
                } finally {
                if (attempt == epoch.get()) job = null
            }
        }
    }

    private fun retryable(error: Exception) = error is ApiFailure &&
        error.kind in setOf(FailureKind.OFFLINE, FailureKind.HTTP) && error.status !in setOf(401, 403)

    private fun problem(error: Exception): LiveProblem = when (error) {
        is ApiFailure -> when (error.kind) {
            FailureKind.HTTP -> LiveProblem(if (error.status == 401 || error.status == 403) Message.ACCESS_DENIED else Message.UNAVAILABLE, error.retryAfterMillis)
            FailureKind.OFFLINE -> LiveProblem(Message.UNAVAILABLE)
            FailureKind.TLS -> LiveProblem(Message.TLS_ERROR)
            FailureKind.TOO_LARGE -> LiveProblem(Message.TOO_LARGE)
            else -> LiveProblem(Message.INVALID_RESPONSE)
        }
        else -> LiveProblem(Message.UNAVAILABLE)
    }
}
