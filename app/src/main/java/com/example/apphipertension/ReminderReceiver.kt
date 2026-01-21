package com.example.apphipertension

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderType = intent.getStringExtra("REMINDER_TYPE")
        if (reminderType == null) {
            Log.e("ReminderReceiver", "Tipo de recordatorio nulo.")
            return
        }

        val title: String
        val message: String
        val notificationId: Int
        val destinationActivity: Class<*> // A qué pantalla ir al tocar

        when (reminderType) {
            "REMIND_MEDICION" -> {
                title = "Registro de Presión Arterial"
                message = "Es un buen momento para registrar tu presión arterial. La constancia es clave."
                notificationId = 3 // ID único para este recordatorio
                destinationActivity = MainActivity::class.java // O una activity específica si la tienes
            }
            "REMIND_COMIDA" -> {
                title = "Registro de Comida"
                message = "¿Qué comiste? ¡Registra tus alimentos para llevar un control de tus calorías!"
                notificationId = 4
                destinationActivity = Dieta::class.java
            }
            "REMIND_ACTIVIDAD" -> {
                title = "Registro de Actividad Física"
                message = "¿Tuviste actividad física hoy? ¡No olvides registrarla!"
                notificationId = 5
                destinationActivity = ActividadFisica::class.java
            }
            "REMIND_SINTOMAS" -> {
                title = "Registro de Síntomas"
                message = "¿Cómo te sientes? Tómate un momento para registrar cualquier síntoma."
                notificationId = 6
                destinationActivity = Sintomas::class.java
            }
            else -> return
        }

        Log.d("ReminderReceiver", "Enviando recordatorio: $title")
        sendReminderNotification(context, title, message, notificationId, destinationActivity)
    }

    private fun sendReminderNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int,
        destination: Class<*>
    ) {
        val intent = Intent(context, destination).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.e("ReminderReceiver", "No hay permiso para notificaciones.")
                return
            }
        }

        val builder = NotificationCompat.Builder(context, NotificationUtils.REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Reemplaza con tu ícono
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }
}