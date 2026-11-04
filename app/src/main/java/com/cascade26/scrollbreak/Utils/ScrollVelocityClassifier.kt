package com.cascade26.scrollbreak.Utils

import java.util.ArrayDeque

/** Per-app, rolling-window scroll-rate classifier. It deliberately stores no view text. */
class ScrollVelocityClassifier(
    private val windowMs: Long = WINDOW_MS,
    private val threshold: Int = THRESHOLD_SCROLLS
) {
    private val timestamps = ArrayDeque<Long>()
    private var lastContentSignature: String? = null
    private var sameContentSince = 0L

    @Synchronized
    fun recordScroll(nowMs: Long): Boolean {
        trim(nowMs)
        timestamps.addLast(nowMs)
        trim(nowMs)
        return timestamps.size >= threshold
    }

    /** Returns true once the same structural content has remained stable for 20 seconds. */
    @Synchronized
    fun observeContent(signature: String?, nowMs: Long): Boolean {
        if (signature.isNullOrEmpty()) {
            lastContentSignature = null
            sameContentSince = 0L
            return false
        }
        if (signature != lastContentSignature) {
            lastContentSignature = signature
            sameContentSince = nowMs
            return false
        }
        if (sameContentSince == 0L) sameContentSince = nowMs
        if (nowMs - sameContentSince >= STABLE_CONTENT_MS) {
            timestamps.clear()
            sameContentSince = nowMs
            return true
        }
        return false
    }

    @Synchronized
    fun hasStableContent(signature: String?, nowMs: Long): Boolean =
        !signature.isNullOrEmpty()
            && signature == lastContentSignature
            && sameContentSince > 0L
            && nowMs - sameContentSince >= STABLE_CONTENT_MS

    @Synchronized
    fun reset() {
        timestamps.clear()
        lastContentSignature = null
        sameContentSince = 0L
    }

    @Synchronized
    fun scrollCount(nowMs: Long): Int {
        trim(nowMs)
        return timestamps.size
    }

    private fun trim(nowMs: Long) {
        while (timestamps.isNotEmpty()) {
            val oldest = timestamps.peekFirst() ?: break
            if (nowMs - oldest <= windowMs) break
            timestamps.removeFirst()
        }
    }

    companion object {
        const val WINDOW_MS = 60_000L
        const val STABLE_CONTENT_MS = 20_000L
        const val THRESHOLD_SCROLLS = 30
    }
}
