/*
 * Velune - by Nikhil
 * Licensed Under GPL-3.0
 */

package com.nikhil.yt.playback

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener

/**
 * Sends content:// and file:// requests straight to [local] (no cache copy made),
 * everything else to [remote] (the normal cache + network chain).
 */
@UnstableApi
class LocalAwareDataSource(
    private val local: DataSource,
    private val remote: DataSource,
) : DataSource {
    private var active: DataSource? = null

    override fun addTransferListener(transferListener: TransferListener) {
        local.addTransferListener(transferListener)
        remote.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        val scheme = dataSpec.uri.scheme
        val target = if (scheme == "content" || scheme == "file") local else remote
        active = target
        return target.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        active!!.read(buffer, offset, length)

    override fun getUri() = active?.uri

    override fun getResponseHeaders(): Map<String, List<String>> =
        active?.responseHeaders ?: emptyMap()

    override fun close() {
        try {
            active?.close()
        } finally {
            active = null
        }
    }
}
