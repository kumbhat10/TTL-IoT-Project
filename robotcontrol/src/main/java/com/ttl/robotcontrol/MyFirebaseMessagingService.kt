package com.ttl.robotcontrol

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.*

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: ${remoteMessage.from}")
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
        }
        sendNotification(remoteMessage)
        remoteMessage.notification?.let {   // Check if message contains a notification payload.
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }
    }

    @SuppressLint("SimpleDateFormat")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
//        Firebase.database.getReference("FCM_Token").setValue(token)
    }

    private fun sendNotification(remoteMessage: RemoteMessage) {
        val intent = Intent(this, Splash::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
//        val channelId = getString(R.string.notification_channel_id)

        val channelId = if(remoteMessage.data.toString() =="robot") {
           getString(R.string.notification_channel_id)
        }
        else {
            getString(R.string.notification_channel_id_ex)
        }

        val defaultSoundUri = if(remoteMessage.data.toString() =="robot") {
            Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/" + R.raw.robotalertmerged)
        }
        else {
            Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/" + R.raw.excavator_online)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.remote)
            .setContentTitle(remoteMessage.notification?.title)
            .setContentText( remoteMessage.notification?.body)
//            .setAutoCancel(true)
            .setSound(defaultSoundUri)
//            .setFullScreenIntent(pendingIntent, true)
            .setColor(ContextCompat.getColor(this, R.color.font_yellow))
//            .setStyle(NotificationCompat.InboxStyle())
            .setLargeIcon(ContextCompat.getDrawable(applicationContext, R.drawable.roboticon)?.toBitmap())

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        val channel = NotificationChannel(channelId, "Daily Notification", NotificationManager.IMPORTANCE_HIGH)
        val audioAttributes =  AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build()
        channel.setSound(defaultSoundUri, audioAttributes)
        channel.setShowBadge(true)
        notificationManager.createNotificationChannel(channel)

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}
