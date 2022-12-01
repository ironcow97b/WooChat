package com.kcw.woochat.api

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PRIVATE
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kcw.woochat.GlobalApplication
import com.kcw.woochat.R
import com.kcw.woochat.view.ChatActivity
import com.kcw.woochat.view.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.tasks.await


class FirebaseMessage : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
        val body = message.notification?.body
        val channelID = message.notification?.channelId
        val data = message.data

        val senderID = data.getValue("senderID")
        val senderName = data.getValue("senderName")

        Log.d("FirebaseMessage", "Message Received : $title | $body | $senderID | $senderName | $channelID")

        if (!title.isNullOrEmpty() && !body.isNullOrEmpty() && !channelID.isNullOrEmpty()) {
            val chatID = (application as GlobalApplication).getChatID()
            if (chatID != senderID) {
                setPush(title, body, senderID, senderName, channelID)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        Log.d("FirebaseMessage", "New Token : $token")
    }

    suspend fun getFireToken(): String {
        val data = CoroutineScope(Dispatchers.IO).async {
            val token = FirebaseMessaging.getInstance().token.await()

            token
        }

        return data.await()
    }

    private fun setPush(title: String, body: String, senderID: String, senderName: String, channelID: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(application, ChatActivity::class.java)
        intent.putExtra("userID", senderID)
        intent.putExtra("userName", senderName)

        val pendingIntent =
            PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        //notification builder 설정
        val builder = NotificationCompat.Builder(applicationContext, channelID)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.createNotificationChannel(
            NotificationChannel(
                channelID,
                "WooChat",
                NotificationManager.IMPORTANCE_HIGH
            )
        )
        notificationManager.notify(0, builder.build())
    }
}