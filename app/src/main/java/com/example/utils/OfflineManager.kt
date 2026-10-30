package com.example.utils

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

@UnstableApi
object OfflineManager {

    suspend fun downloadAudio(context: Context, url: String, onProgress: (Float) -> Unit): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val cache = AudioCacheManager.getCache(context) ?: return@withContext false
                val dataSource = DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true).createDataSource()
                val cacheDataSource = CacheDataSource.Factory().setCache(cache).setUpstreamDataSourceFactory { dataSource }.createDataSource()
                
                val dataSpec = DataSpec(Uri.parse(url))
                val cacheWriter = CacheWriter(cacheDataSource, dataSpec, null) { requestLength, bytesCached, newBytesCached ->
                    if (requestLength > 0) {
                        val progress = bytesCached.toFloat() / requestLength.toFloat()
                        onProgress(progress)
                    }
                }
                cacheWriter.cache()
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun isAudioCached(context: Context, url: String): Boolean {
        val cache = AudioCacheManager.getCache(context) ?: return false
        val key = androidx.media3.datasource.cache.CacheKeyFactory.DEFAULT.buildCacheKey(DataSpec(Uri.parse(url)))
        val cachedBytes = cache.getCachedBytes(key, 0, androidx.media3.common.C.LENGTH_UNSET.toLong())
        // Normally we'd compare to total length, but getting total length requires network.
        // We'll consider it cached if there's any data for now, or you could do a more precise check if you store metadata.
        return cachedBytes > 0
    }
}
