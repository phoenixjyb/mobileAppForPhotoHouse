package dev.photohouse.tv

/** Only fixed labels and native numeric codes leave the player; never exception messages/URLs. */
internal data class TvPlaybackFailure(val stage: Stage, val what: Int? = null, val extra: Int? = null) {
    enum class Stage { SETUP, NATIVE, PREPARE_TIMEOUT, CONTROL }
    val code: String get() = "TV-${stage.name}" + if (stage == Stage.NATIVE) " (${what ?: 0}, ${extra ?: 0})" else ""
    fun message(zh: Boolean): String {
        val text = when (stage) {
            Stage.PREPARE_TIMEOUT -> if (zh) "视频加载超时。请重试。" else "Video preparation timed out. Please retry."
            Stage.SETUP -> if (zh) "无法启动电视播放器。" else "The TV player could not start."
            Stage.CONTROL -> if (zh) "电视播放器操作失败。请重新打开视频。" else "The TV player could not complete the action. Reopen the video."
            Stage.NATIVE -> when (extra) {
                -1004 -> if (zh) "播放器无法读取视频。" else "The player could not read the video."
                -1007 -> if (zh) "播放器无法识别此视频文件。" else "The player could not parse this video."
                -1010 -> if (zh) "电视播放器不支持此视频格式。" else "The TV player does not support this video format."
                -110 -> if (zh) "电视播放器响应超时。" else "The TV player timed out."
                else -> if (zh) "电视播放器报告播放失败。" else "The TV player reported a playback failure."
            }
        }
        return "$text\n$code"
    }
}
