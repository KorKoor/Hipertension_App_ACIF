package com.example.apphipertension

data class Alimento(
    val nombre: String = "",
    val calorias_base: Double = 0.0, // Calorías por cada unidad_base
    val unidad_base: String = "",    // Ej: "100g", "taza", "pieza"
    val tipo: String = ""            // Ej: "desayuno", "comida", "cena", "bebida"
)
