package com.example.apphipertension

import com.google.firebase.Timestamp

data class AlimentoRegistrado(
    val nombre: String = "",
    val cantidad: Double = 0.0,  // Ej: 1.5, 2.0
    val unidad: String = "",     // Ej: "tazas", "piezas"
    val calorias: Double = 0.0   // El total calculado (calorias_base * cantidad)
)

data class RegistroDieta(
    val fecha: Timestamp = Timestamp.now(),
    val calorias_totales_dia: Double = 0.0,
    val desayuno: List<AlimentoRegistrado> = emptyList(),
    val comida: List<AlimentoRegistrado> = emptyList(),
    val cena: List<AlimentoRegistrado> = emptyList(),
    val colacion: List<AlimentoRegistrado> = emptyList()
)
