package com.crow.tradewolf.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.crow.tradewolf.R
import com.crow.tradewolf.ui.ChatActivity

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "chat_notifications"
        const val CHANNEL_NAME = "Notificaciones de Chat"
        const val CHANNEL_DESCRIPTION = "Notificaciones para nuevos mensajes"
        const val NOTIFICATION_ID_BASE = 1000
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNewMessageNotification(
        messageId: String,
        senderName: String,
        messageText: String,
        senderId: String
    ) {
        // Crear intent para abrir el chat
        val intent = Intent(context, ChatActivity::class.java).apply {
            putExtra("RECEIVER_ID", senderId)
            putExtra("RECEIVER_NAME", senderName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            messageId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Construir la notificación
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(senderName)
            .setContentText(messageText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(100, 200, 100, 200))

        with(NotificationManagerCompat.from(context)) {
            notify(messageId.hashCode(), builder.build())
        }
    }
}
