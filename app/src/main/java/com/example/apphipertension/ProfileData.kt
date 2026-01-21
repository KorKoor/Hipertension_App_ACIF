package com.example.apphipertension

data class ProfileData(
    val nombre: String = "No definido",
    val correo: String = "",
    val peso: String = "N/A",
    val altura: String = "N/A",
    val imc: String = "N/A",
    val fechaNacimiento: String = "N/A",
    val edad: String = "N/A",
    val sexo: String = "N/A",
    val proxima_cita_medica: String = "N/A", // <-- Campo Actualizado/Nuevo
    val meta_calorias_diarias: Double = 0.0, // <-- Campo Nuevo
    val alimentosEvitar_pre: List<String> = emptyList(), // <-- Campo Nuevo
    val alimentosEvitar_per: List<String> = emptyList(), // <-- Campo Nuevo
    val medicamentosEvitar: List<String> = emptyList(),  // <-- Campo Nuevo
    val padecimientos_pre: List<String> = emptyList(), // <-- Campo Nuevo
    val padecimientos_per: List<String> = emptyList()  // <-- Campo Nuevo
)