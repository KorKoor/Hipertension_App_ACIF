package com.example.apphipertension

data class Medicine(
    val id: String = "",
    val nombre: String = "",
    val dosis: String = "",
    val unidad: String = "",
    val hora: String = "",
    val frecuencia: String = "",
    val fecha: String = "", // <-- fecha en formato "yyyy-MM-dd"
    val tieneRecordatorio: Boolean = false,
    val alarmId: Int = 0
)