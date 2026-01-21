package com.example.apphipertension

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar
import java.text.SimpleDateFormat
import java.time.LocalTime
import android.os.Build
import java.time.format.DateTimeFormatter
import java.util.Locale

object MedicationAlarmScheduler {

    fun programarAlarma(context: Context, medicine: Medicine) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val pendingIntentId = medicine.alarmId

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("NOMBRE_MED", medicine.nombre)
                putExtra("DOSIS_MED", "${medicine.dosis} ${medicine.unidad}")
                putExtra("FRECUENCIA_MED", medicine.frecuencia)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                pendingIntentId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 1. Normalizar la hora para evitar errores con "P.M." o "A.M."
            val horaLimpia = medicine.hora
                .replace("p.m.", "PM")
                .replace("a.m.", "AM")
                .replace("P.M.", "PM")
                .replace("A.M.", "AM")
                .trim()

            // 2. Parsear con el formato correcto
            val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
            val horaLocal = LocalTime.parse(horaLimpia, formatter)

            // 2. Configurar calendario base
            val calendario = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, horaLocal.hour)
                set(Calendar.MINUTE, horaLocal.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // 3. Obtener intervalo
            val intervaloMillis = obtenerIntervaloMillis(medicine.frecuencia)

            // 4. Ajustar si la hora ya pasó
            if (calendario.timeInMillis <= System.currentTimeMillis()) {
                if (intervaloMillis >= AlarmManager.INTERVAL_DAY) {
                    calendario.add(Calendar.DAY_OF_YEAR, 1)
                } else {
                    while (calendario.timeInMillis <= System.currentTimeMillis()) {
                        calendario.timeInMillis += intervaloMillis
                    }
                }
            }

            // 5. Programar alarma exacta con verificación
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12+ → verificar permiso antes de programar
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendario.timeInMillis,
                            pendingIntent
                        )
                        val fechaHoraLog = SimpleDateFormat("dd/MM HH:mm").format(calendario.time)
                        Log.d("SCHEDULER", "Alarma programada para: ${medicine.nombre} a las $fechaHoraLog (Frecuencia: ${medicine.frecuencia})")
                    } else {
                        Log.e("SCHEDULER", "No se puede programar alarmas exactas. El permiso no está habilitado.")
                    }
                } else {
                    // Android < 12 → programar directamente
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendario.timeInMillis,
                        pendingIntent
                    )
                    val fechaHoraLog = SimpleDateFormat("dd/MM HH:mm").format(calendario.time)
                    Log.d("SCHEDULER", "Alarma programada (API<31) para: ${medicine.nombre} a las $fechaHoraLog (Frecuencia: ${medicine.frecuencia})")
                }
            } catch (se: SecurityException) {
                Log.e("SCHEDULER", "Error de seguridad al programar alarma exacta: ${se.message}")
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelarAlarma(context: Context, medicine: Medicine) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                medicine.alarmId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d("SCHEDULER", "Alarma cancelada para: ${medicine.nombre}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun obtenerIntervaloMillis(frecuenciaTexto: String): Long {
        val regex = Regex("\\d+")
        val resultado = regex.find(frecuenciaTexto)
        val horas = resultado?.value?.toLongOrNull()

        return if (horas != null) {
            horas * 3600 * 1000L
        } else {
            AlarmManager.INTERVAL_DAY
        }
    }
}
