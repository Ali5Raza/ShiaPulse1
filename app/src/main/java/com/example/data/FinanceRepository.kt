package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit

import retrofit2.http.GET
import retrofit2.http.Path
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import retrofit2.Response

interface CurrencyApiService {
    // The endpoint will be dynamically created using Retrofit
    @GET("v1/currencies/{currency}.json")
    suspend fun getRates(@Path("currency") currency: String): Response<okhttp3.ResponseBody>
}

object FinanceRepository {
    private const val BASE_URL = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/"
    
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            
            .build()
    }

    private val api by lazy {
        retrofit.create(CurrencyApiService::class.java)
    }

    suspend fun fetchLiveGoldSilverPrices(
        currencyCode: String,
        prefs: SharedPreferences
    ): Pair<Double?, Double?> = withContext(Dispatchers.IO) {
        try {
            val lowerCurrency = currencyCode.lowercase()
            val response = api.getRates(lowerCurrency)
            
            val responseBody = response.body()
            if (response.isSuccessful && responseBody != null) {
                val jsonString = responseBody.string()
                val jsonObject = JSONObject(jsonString)
                val rates = jsonObject.getJSONObject(lowerCurrency)
                
                val xauRate = rates.optDouble("xau", -1.0)
                val xagRate = rates.optDouble("xag", -1.0)
                
                var goldPriceGram: Double? = null
                var silverPriceGram: Double? = null
                
                if (xauRate > 0) {
                    val goldPricePerOz = 1.0 / xauRate
                    goldPriceGram = goldPricePerOz / 31.1034768
                    prefs.edit().putFloat("cached_gold_price_${lowerCurrency}", goldPriceGram.toFloat()).apply()
                }
                
                if (xagRate > 0) {
                    val silverPricePerOz = 1.0 / xagRate
                    silverPriceGram = silverPricePerOz / 31.1034768
                    prefs.edit().putFloat("cached_silver_price_${lowerCurrency}", silverPriceGram.toFloat()).apply()
                }
                
                prefs.edit().putLong("finance_last_updated_${lowerCurrency}", System.currentTimeMillis()).apply()
                
                Pair(goldPriceGram, silverPriceGram)
            } else {
                Pair(null, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, null)
        }
    }
    
    fun getCachedPrices(currencyCode: String, prefs: SharedPreferences): Pair<Double?, Double?> {
        val lowerCurrency = currencyCode.lowercase()
        val g = prefs.getFloat("cached_gold_price_${lowerCurrency}", -1f)
        val s = prefs.getFloat("cached_silver_price_${lowerCurrency}", -1f)
        return Pair(
            if (g > 0) g.toDouble() else null,
            if (s > 0) s.toDouble() else null
        )
    }
}
