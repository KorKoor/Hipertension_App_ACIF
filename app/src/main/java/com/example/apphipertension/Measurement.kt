package com.example.apphipertension

data class Measurement(
    val date: String = "",
    val time: String = "",
    val sistolica: Int = 0,    // Debe ser Int porque en la foto no tiene comillas
    val diastolica: Int = 0,   // Debe ser Int
    val pulso: Int = 0,        // Debe ser Int
    val nota: String = ""
)

