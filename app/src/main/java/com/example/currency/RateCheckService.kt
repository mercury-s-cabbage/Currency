package com.example.currency

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RateCheckService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var rateCheckAttempt = 0
    private val rateCheckInteractor = RateCheckInteractor()
    private var lastNotifiedRate: Double? = null
    private val channelId = "RateChangeChannel"
    private val channelName = "Rate Change Notifications"
    private var isChannelCreated = false

    private val rateCheckRunnable: Runnable = Runnable {
        requestAndCheckRate()
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !isChannelCreated) {
            val notificationChannel = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Channel for BTC rate change notifications"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
            isChannelCreated = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand started")
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Rate Check Service")
            .setContentText("Monitoring BTC exchange rate changes")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        startForeground(1, notification)
        handler.post(rateCheckRunnable)
        return START_STICKY
    }

    private fun requestAndCheckRate() {
        if (rateCheckAttempt < RATE_CHECK_ATTEMPTS_MAX) {
            rateCheckAttempt++
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val rate = rateCheckInteractor.requestRate()
                    if (rate.isNotEmpty()) {
                        val currentRate = parseBtcRateFromString(rate)
                        if (shouldNotify(currentRate)) {
                            val direction = getRateDirection(currentRate)
                            sendRateChangeNotification(direction, currentRate)
                            lastNotifiedRate = currentRate
                        }
                    } else {
                        Log.e(TAG, "Failed to retrieve rate.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during rate request: ${e.message}")
                }
            }
            handler.postDelayed(rateCheckRunnable, RATE_CHECK_INTERVAL)
        } else {
            Log.d(TAG, "Max attempts reached, stopping service.")
            stopSelf()
        }
    }

    private fun shouldNotify(currentRate: Double): Boolean {
        return lastNotifiedRate == null || kotlin.math.abs(currentRate - lastNotifiedRate!!) >= 1
    }

    private fun getRateDirection(currentRate: Double): String {
        return when {
            lastNotifiedRate == null -> "Получен курс BTC: $currentRate USD"
            currentRate > lastNotifiedRate!! -> "Курс BTC увеличился на ${currentRate - lastNotifiedRate!!} USD"
            currentRate < lastNotifiedRate!! -> "Курс BTC уменьшился на ${lastNotifiedRate!! - currentRate} USD"
            else -> "Курс BTC не изменился"
        }
    }

    private fun sendRateChangeNotification(direction: String, rate: Double) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Изменение курса BTC")
            .setContentText(direction)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(1, notification)
    }

    override fun onBind(p0: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(rateCheckRunnable)
    }

    companion object {
        const val TAG = "RateCheckService"
        const val RATE_CHECK_INTERVAL = 5000L
        const val RATE_CHECK_ATTEMPTS_MAX = 100

        fun startService(context: Context) {
            context.startService(Intent(context, RateCheckService::class.java))
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, RateCheckService::class.java))
        }
    }
}