package com.example.apphipertension

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Locale

object MedicineUtils {

    // Función principal para cargar y calcular la próxima toma
    fun loadNextDose(onResult: (Medicine?, LocalDateTime?) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            onResult(null, null)
            return
        }
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(user.uid)
            .collection("medicamentos")
            .get()
            .addOnSuccessListener { result ->
                val medicines = result.documents.map { doc ->
                    val data = doc.data ?: emptyMap<String, Any>()
                    Medicine(
                        id = doc.id,
                        nombre = data["nombre"] as? String ?: "",
                        dosis = data["dosis"] as? String ?: "",
                        unidad = data["unidad"] as? String ?: "",
                        // La hora viene en 12h con AM/PM (Ej: "10:00 AM", "01:30 p.m.")
                        hora = data["hora"] as? String ?: "",
                        frecuencia = data["frecuencia"] as? String ?: "",
                        fecha = data["fecha"] as? String ?: ""
                    )
                }
                val nextDose = calculateNextDose(medicines)
                onResult(nextDose?.first, nextDose?.second)
            }
            .addOnFailureListener {
                Log.e("MedicineUtils", "Error al cargar medicamentos", it)
                onResult(null, null)
            }
    }


    // Función para calcular la próxima toma entre todos los medicamentos
    private fun calculateNextDose(medicines: List<Medicine>): Pair<Medicine, LocalDateTime>? {
        val now = LocalDateTime.now()
        var closestDose: Pair<Medicine, LocalDateTime>? = null

        // ¡CAMBIO! Usamos Locale.ENGLISH que entiende "AM" y "PM"
        val formatter12h = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        for (medicine in medicines) {
            try {
                // 1. Obtener la hora LocalTime de la toma

                // ¡CAMBIO! Normalizamos la hora antes de analizarla
                // Convierte "10:00 AM" -> "10:00 AM"
                // Convierte "12:40 p.m." -> "12:40 PM"
                // Convierte "01:50 p.m." -> "01:50 PM"
                val normalizedTime = medicine.hora.uppercase(Locale.ENGLISH)
                    .replace("P.M.", "PM")
                    .replace("A.M.", "AM")

                val localTime = LocalTime.parse(normalizedTime, formatter12h)
                val localStartDate = LocalDate.parse(medicine.fecha, dateFormatter)

                // 2. Determinar la próxima fecha y hora de la toma
                val nextDoseTime = when (medicine.frecuencia) {
                    "Diario" -> getNextDailyDose(localTime, localStartDate, now)
                    "Cada 8 horas" -> getNextHourlyDose(localTime, localStartDate, now, 8)
                    "Cada 12 horas" -> getNextHourlyDose(localTime, localStartDate, now, 12)
                    "Lunes, Miércoles, Viernes" -> getNextCustomDose(localTime, localStartDate, now, listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
                    // Agrega aquí otras frecuencias si son necesarias
                    else -> null
                }

                // 3. Comparar con la dosis más cercana encontrada
                if (nextDoseTime != null && nextDoseTime.isAfter(now)) {
                    if (closestDose == null || nextDoseTime.isBefore(closestDose.second)) {
                        closestDose = Pair(medicine, nextDoseTime)
                    }
                }
            } catch (e: DateTimeParseException) {
                // Loguear el error si un medicamento tiene formato inválido
                Log.e("MedicineUtils", "Error al parsear la hora: '${medicine.hora}' del med: ${medicine.nombre}", e)
                e.printStackTrace()
            } catch (e: Exception) {
                Log.e("MedicineUtils", "Error general calculando dosis para: ${medicine.nombre}", e)
                e.printStackTrace()
            }
        }
        return closestDose
    }

    private fun getNextDailyDose(localTime: LocalTime, localStartDate: LocalDate, now: LocalDateTime): LocalDateTime {
        // La fecha debe ser al menos la de inicio
        val dateToCheck = if (localStartDate.isAfter(now.toLocalDate())) localStartDate else now.toLocalDate()

        var nextDose = LocalDateTime.of(dateToCheck, localTime)

        // Si la dosis de hoy ya pasó, la movemos a mañana
        if (nextDose.isBefore(now)) {
            nextDose = nextDose.plusDays(1)
        }

        // Asegurarse de que no sea antes de la fecha de inicio
        if (nextDose.toLocalDate().isBefore(localStartDate)) {
            return LocalDateTime.of(localStartDate, localTime)
        }

        return nextDose
    }

    private fun getNextHourlyDose(localTime: LocalTime, localStartDate: LocalDate, now: LocalDateTime, hours: Long): LocalDateTime? {
        val totalDosesPerDay = (24 / hours).toInt()
        val baseDate = if (now.toLocalDate().isBefore(localStartDate)) localStartDate else now.toLocalDate()

        // Generar todas las tomas posibles para el día base
        val dosesForBaseDay = (0 until totalDosesPerDay).map { i ->
            LocalDateTime.of(baseDate, localTime.plusHours(i * hours))
        }

        // Encontrar la primera dosis futura en el día base
        val nextDoseToday = dosesForBaseDay.find { it.isAfter(now) }
        if (nextDoseToday != null) {
            return nextDoseToday
        }

        // Si no hay dosis futuras hoy, la próxima toma es la hora inicial del día siguiente
        // (asegurándose de que el "día siguiente" no sea antes de la fecha de inicio)
        val nextDate = if (baseDate.isBefore(localStartDate)) localStartDate else baseDate.plusDays(1)
        return LocalDateTime.of(nextDate, localTime)
    }

    // Simplificado. Se puede expandir para más lógicas de frecuencia
    private fun getNextCustomDose(localTime: LocalTime, localStartDate: LocalDate, now: LocalDateTime, days: List<DayOfWeek>): LocalDateTime {
        var date = if (localStartDate.isAfter(now.toLocalDate())) localStartDate else now.toLocalDate()

        // Buscar el próximo día válido, comenzando por hoy
        for (i in 0..7) {
            val dateToCheck = date.plusDays(i.toLong())
            if (days.contains(dateToCheck.dayOfWeek) && dateToCheck.isAfter(localStartDate.minusDays(1))) {

                val nextDose = LocalDateTime.of(dateToCheck, localTime)
                if (nextDose.isAfter(now)) {
                    return nextDose
                }
            }
        }
        // Fallback: Debería encontrar algo en 7 días
        return LocalDateTime.of(now.toLocalDate().plusDays(7), localTime)
    }

    // Helper para formatear la hora de LocalTime
    fun formatTime(dateTime: LocalDateTime): String {
        // Usamos Locale.ENGLISH para AM/PM consistentemente
        val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
        return dateTime.toLocalTime().format(formatter)
    }

    // Helper para formatear la fecha con nombre del día
    fun formatDate(dateTime: LocalDateTime): String {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        return when (dateTime.toLocalDate()) {
            today -> "Hoy"
            tomorrow -> "Mañana"
            else -> {
                // Usamos Locale("es", "ES") para "Mar 01 Nov"
                val formatter = DateTimeFormatter.ofPattern("EEE dd MMM", Locale("es", "ES"))
                dateTime.toLocalDate().format(formatter)
            }
        }
    }
}