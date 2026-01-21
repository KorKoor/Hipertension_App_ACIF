package com.example.apphipertension

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val nombreMed = intent.getStringExtra("NOMBRE_MED") ?: "Medicamento"
        val dosisMed = intent.getStringExtra("DOSIS_MED") ?: ""
        val intervalo = intent.getLongExtra("INTERVALO", 2 * 60 * 60 * 1000) // default 2h
        val alarmId = intent.getIntExtra("alarmId", 0)

        // 1. Crear el canal de Alta Prioridad
        crearCanalNotificacion(context)

        // 2. Intent para abrir la app al tocar la notificación
        val tapIntent = Intent(context, Medicate::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntentActivity = PendingIntent.getActivity(
            context, 0, tapIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Sonido de Alarma
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        // 4. Construir la notificación
        val builder = NotificationCompat.Builder(context, "canal_med_alta_prioridad")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("¡Hora de tu medicina!")
            .setContentText("Te toca tomar: $nombreMed ($dosisMed)")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setContentIntent(pendingIntentActivity)
            .setAutoCancel(true)
            .setFullScreenIntent(pendingIntentActivity, true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Ya la tomé",
                pendingIntentActivity
            )

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        // 5. Reprogramar la siguiente alarma
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val nextTime = System.currentTimeMillis() + intervalo

        val nextIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("NOMBRE_MED", nombreMed)
            putExtra("DOSIS_MED", dosisMed)
            putExtra("INTERVALO", intervalo)
            putExtra("alarmId", alarmId)
        }

        val nextPendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextTime,
            nextPendingIntent
        )
    }

    private fun crearCanalNotificacion(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alarmas de Medicación"
            val descriptionText = "Notificaciones de alta prioridad para tomar medicina"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel("canal_med_alta_prioridad", name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setSound(alarmSound, audioAttributes)
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
