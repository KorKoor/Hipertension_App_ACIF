package com.example.apphipertension

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.util.Calendar

object AlarmUtils {

    private const val ALARM_REQUEST_CODE = 1001

    // --- ALARMA DIARIA DE TIP ---
    fun scheduleDailyTipAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DailyTipReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )

        Log.d("AlarmUtils", "Alarma programada para las 9:00 AM, comenzando en ${calendar.time}")
    }

    // --- RECORDATORIOS FIJOS (medición, comida, etc.) ---
    fun scheduleAllReminders(context: Context) {
        scheduleRepeatingAlarm(context, "REMIND_MEDICION", 2001, 8, 0)
        scheduleRepeatingAlarm(context, "REMIND_MEDICION", 2002, 19, 0)

        scheduleRepeatingAlarm(context, "REMIND_COMIDA", 2003, 9, 0)
        scheduleRepeatingAlarm(context, "REMIND_COMIDA", 2004, 14, 0)
        scheduleRepeatingAlarm(context, "REMIND_COMIDA", 2005, 20, 30)

        scheduleRepeatingAlarm(context, "REMIND_ACTIVIDAD", 2006, 17, 0)
        scheduleRepeatingAlarm(context, "REMIND_SINTOMAS", 2007, 20, 0)

        Log.d("AlarmUtils", "Todos los recordatorios fijos han sido programados.")
    }

    private fun scheduleRepeatingAlarm(
        context: Context,
        reminderType: String,
        requestCode: Int,
        hour: Int,
        minute: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("REMINDER_TYPE", reminderType)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
    private fun parseFrecuencia(frecuencia: String): Long {
        return when (frecuencia.lowercase()) {
            "cada 2 horas", "cada 2 hrs" -> 2 * 60 * 60 * 1000L
            "cada 4 horas", "cada 4 hrs" -> 4 * 60 * 60 * 1000L
            "cada 6 horas", "cada 6 hrs" -> 6 * 60 * 60 * 1000L
            "cada 8 horas", "cada 8 hrs" -> 8 * 60 * 60 * 1000L
            "cada 12 horas", "cada 12 hrs" -> 12 * 60 * 60 * 1000L
            "cada 24 horas", "cada 24 hrs" -> 24 * 60 * 60 * 1000L
            else -> 2 * 60 * 60 * 1000L // valor por defecto: cada 2 horas
        }
    }


    fun scheduleMedicationAlarm(context: Context, medicamento: Map<String, Any>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Parsear frecuencia desde Firestore ("Cada 2 horas")
        val intervaloMillis = parseFrecuencia(medicamento["frecuencia"] as String)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("NOMBRE_MED", medicamento["nombre"] as String)
            putExtra("DOSIS_MED", medicamento["dosis"] as String)
            putExtra("INTERVALO", intervaloMillis)
            putExtra("alarmId", medicamento["alarmId"] as Int)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            medicamento["alarmId"] as Int,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calcular hora inicial
        val partes = (medicamento["hora"] as String).split(":")
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, partes[0].toInt())
            set(Calendar.MINUTE, partes[1].toInt())
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // --- Verificación de permiso en Android 12+ ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val settingsIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                settingsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(settingsIntent)
                Log.e("AlarmUtils", "No se puede programar alarma exacta sin permiso del usuario.")
                return
            }
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.d("AlarmUtils", "Alarma de medicamento programada: ${medicamento["nombre"]} cada ${medicamento["frecuencia"]}")
        } catch (e: SecurityException) {
            Log.e("AlarmUtils", "Error al programar alarma exacta: ${e.message}")
        }
    }
}
