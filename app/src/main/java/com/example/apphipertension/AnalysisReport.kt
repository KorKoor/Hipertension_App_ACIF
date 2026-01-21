package com.example.apphipertension

// AnalysisReport.kt
data class AnalysisReport(
    val profileData: ProfileData,
    val mediciones: List<Measurement>, // Asume que Measurement tiene fecha, hora, sis, dia, pulso, nota
    val medicamentos: List<Medicine>, // Asume que Medicine tiene nombre, dosis, unidad, frecuencia, fecha, hora
    val sintomas: List<SintomaGuardado>, // Ya la tienes

    // --- NUEVOS CAMPOS ---
    // Asume que tu DataFetcher cargará los registros de dieta (ej. últimos 7 días o el de hoy)
    val registrosDieta: List<RegistroDieta>, // La clase que ya definimos
    // Asume que tu DataFetcher cargará los registros de actividad (ej. últimos 7 días o el de hoy)
    val registrosActividad: List<ActividadGuardada> // La clase que ya definimos
)
