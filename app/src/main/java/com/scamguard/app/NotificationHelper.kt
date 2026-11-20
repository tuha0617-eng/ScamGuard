package com.scamguard.app
import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
object NotificationHelper {
    private const val CHANNEL_ID = "scamguard_alerts"
    private const val CHANNEL_NAME = "Cảnh báo ScamGuard"
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo khi phát hiện nguy cơ lừa đảo"
            }
            val manager = context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return NotificationManagerCompat
            .from(context)
            .areNotificationsEnabled()
    }
    @SuppressLint("MissingPermission")
    fun showRiskNotification(
        context: Context,
        riskLevel: String,
        reason: String,
        recommendation: String
    ) {
        val appContext = context.applicationContext

        createNotificationChannel(appContext)

        if (!hasNotificationPermission(appContext)) {
            return
        }
        val title: String
        val notificationId: Int

        when (riskLevel.uppercase()) {
            "HIGH" -> {
                title = "Cảnh báo lừa đảo nguy hiểm!"
                notificationId = 1001
            }
            "MEDIUM" -> {
                title = "Cảnh báo: Cần chú ý!"
                notificationId = 1002
            }
            else -> {
                title = "Kết quả kiểm tra an toàn"
                notificationId = 1003
            }
        }
        val content = """
            Mức độ rủi ro: $riskLevel
            Lý do: $reason
            Khuyến nghị: $recommendation
        """.trimIndent()
        val notification = NotificationCompat.Builder(
            appContext,
            CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(reason)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(content)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(appContext)
            .notify(notificationId, notification)
    }
    fun showHighRiskNotification(context: Context) {
        showRiskNotification(
            context = context,
            riskLevel = "HIGH",
            reason = "Nội dung có dấu hiệu lừa đảo hoặc chiếm đoạt tài sản.",
            recommendation = "Không nhấn vào liên kết, không cung cấp mật khẩu, mã OTP hoặc thông tin ngân hàng."
        )
    }
}
