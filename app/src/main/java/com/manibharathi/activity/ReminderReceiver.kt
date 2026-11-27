package com.manibharathi.activity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.manibharathi.activity.R

class ReminderReceiver: BroadcastReceiver(){
 override fun onReceive(context:Context,intent:Intent?){
  val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  val channel=NotificationChannel("activity_reminders","Activity Reminders",NotificationManager.IMPORTANCE_DEFAULT)
  manager.createNotificationChannel(channel)
  val n=NotificationCompat.Builder(context,"activity_reminders")
   .setSmallIcon(R.drawable.activity_logo)
   .setContentTitle("Activity")
   .setContentText("Your daily activities are waiting. Keep going!")
   .setAutoCancel(true)
   .build()
  manager.notify(1001,n)
 }
}
