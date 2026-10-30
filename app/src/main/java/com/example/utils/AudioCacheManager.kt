package com.example.utils

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object AudioCacheManager {
    private var cache: SimpleCache? = null

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @Synchronized
    fun getCache(context: Context): SimpleCache? {
        if (cache == null) {
            try {
                val cacheDir = File(context.cacheDir, "audio_cache")
                val evictor = LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024) // 100MB
                val databaseProvider = StandaloneDatabaseProvider(context)
                cache = SimpleCache(cacheDir, evictor, databaseProvider)
            } catch (e: Exception) {
                android.util.Log.e("AudioCacheManager", "Error initializing SimpleCache: ${e.message}", e)
                cache = null
            }
        }
        return cache
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    fun getCacheDataSourceFactory(context: Context): androidx.media3.datasource.DataSource.Factory {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)
        val cacheInstance = getCache(context)
        return if (cacheInstance != null) {
            try {
                CacheDataSource.Factory()
                    .setCache(cacheInstance)
                    .setUpstreamDataSourceFactory(httpDataSourceFactory)
                    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            } catch (e: Exception) {
                android.util.Log.e("AudioCacheManager", "Error creating CacheDataSource.Factory: ${e.message}", e)
                httpDataSourceFactory
            }
        } else {
            httpDataSourceFactory
        }
    }
}
