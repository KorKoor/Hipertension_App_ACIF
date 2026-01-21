package com.example.apphipertension

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationUtils {

    const val CALORIE_CHANNEL_ID = "calorie_goal_channel"

    const val DAILY_TIP_CHANNEL_ID = "daily_tip_channel"
    const val REMINDER_CHANNEL_ID = "reminder_channel"
    const val MEDICATION_CHANNEL_ID = "medication_channel"

    fun createNotificationChannels(context: Context) {
        // Solo para API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // 1. Canal de Alertas de Calorías (tu código existente)
            val calorieChannelName = "Alertas de Metas"
            val calorieChannelDesc = "Notificaciones al superar metas (calorías, etc.)"
            val calorieImportance = NotificationManager.IMPORTANCE_DEFAULT
            val calorieChannel = NotificationChannel(CALORIE_CHANNEL_ID, calorieChannelName, calorieImportance).apply {
                description = calorieChannelDesc
            }

            // --- 2. NUEVO Canal de Consejos Diarios ---
            val tipChannelName = "Consejo del Día"
            val tipChannelDesc = "Notificación diaria con un consejo de salud."
            val tipImportance = NotificationManager.IMPORTANCE_LOW // Usamos LOW para que no sea intrusivo
            val tipChannel = NotificationChannel(DAILY_TIP_CHANNEL_ID, tipChannelName, tipImportance).apply {
                description = tipChannelDesc
            }
            // ------------------------------------
            val reminderChannelName = "Recordatorios de Registro"
            val reminderChannelDesc = "Notificaciones para registrar datos (comidas, mediciones, etc.)"
            val reminderImportance = NotificationManager.IMPORTANCE_DEFAULT // Importancia normal
            val reminderChannel = NotificationChannel(REMINDER_CHANNEL_ID, reminderChannelName, reminderImportance).apply {
                description = reminderChannelDesc
            }

            val medChannelName = "Recordatorios de Medicamentos"
            val medChannelDesc = "Alertas importantes para tomar tu medicación."
            val medImportance = NotificationManager.IMPORTANCE_HIGH // ¡ALTA PRIORIDAD!
            val medChannel = NotificationChannel(MEDICATION_CHANNEL_ID, medChannelName, medImportance).apply {
                description = medChannelDesc
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(calorieChannel)
            notificationManager.createNotificationChannel(tipChannel)
            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(medChannel)
        }
    }

    // You can add the function to send the notification here too later
}
