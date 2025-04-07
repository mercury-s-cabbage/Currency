package com.example.currency

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class RateCheckInteractor {
    private val networkClient = NetworkClient()

    suspend fun requestRate(): String {
        return withContext(Dispatchers.IO) {
            val result = networkClient.request(MainViewModel.USD_RATE_URL)
            Log.d("RateCheckInteractor", "Received JSON: $result")
            if (!result.isNullOrEmpty()) {
                parseRate(result)
            } else {
                ""
            }
        }
    }

    private fun parseRate(jsonString: String): String {
        return try {
            val gson = Gson()
            val currencyResponse: CurrencyResponse = gson.fromJson(jsonString, CurrencyResponse::class.java)
            val eurRate = currencyResponse.EUR ?: 0.0
            Log.d("RateCheckInteractor", "Parsed EUR rate: $eurRate")
            "EUR: $eurRate"
        } catch (e: Exception) {
            Log.e("RateCheckInteractor", "Error parsing JSON", e)
            ""
        }
    }
}

fun parseBtcRateFromString(rate: String): Double {
    val regex = "EUR: ([0-9.]+)".toRegex()
    val matchResult = regex.find(rate)
    val parsedRate = matchResult?.groups?.get(1)?.value?.toDoubleOrNull() ?: 0.0
    Log.d("RateCheckInteractor", "Extracted rate from string: $parsedRate")
    return parsedRate
}