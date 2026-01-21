package com.example.apphipertension

import android.Manifest
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
import java.time.LocalDate

class DailyTipReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("DailyTipReceiver", "Alarma recibida. Preparando consejo del día.")

        // Evitar doble notificación (aunque la alarma es diaria, es una buena práctica)
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val todayDate = LocalDate.now().toString()
        val lastTipDate = prefs.getString("last_tip_date", "")

        if (lastTipDate != todayDate) {
            // Obtener el consejo
            val consejo = ConsejosDiarios.obtenerConsejoDelDia()

            // Enviar la notificación
            sendTipNotification(context, consejo)

            // Guardar la fecha de hoy
            prefs.edit().putString("last_tip_date", todayDate).apply()
            Log.d("DailyTipReceiver", "Consejo enviado y fecha guardada.")
        } else {
            Log.d("DailyTipReceiver", "Consejo ya enviado hoy. Saltando.")
        }
    }

    private fun sendTipNotification(context: Context, consejo: String) {
        // 1. Crear un Intent para abrir MainActivity cuando se toque la notificación
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        // 2. Verificar permiso (necesario para API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.e("DailyTipReceiver", "No se tiene permiso para enviar notificaciones.")
                return // No se puede enviar
            }
        }

        // 3. Construir la notificación
        val builder = NotificationCompat.Builder(context, NotificationUtils.DAILY_TIP_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Reemplaza con tu ícono
            .setContentTitle("Tu Consejo de Salud del Día")
            .setContentText(consejo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(consejo)) // Para texto largo
            .setPriority(NotificationCompat.PRIORITY_LOW) // Prioridad baja
            .setContentIntent(pendingIntent) // Acción al tocar
            .setAutoCancel(true) // Se cierra al tocar

        // 4. Mostrar la notificación
        with(NotificationManagerCompat.from(context)) {
            val notificationId = 2 // ID único para este TIPO de notificación (1 es para calorías)
            notify(notificationId, builder.build())
        }
    }
}