package com.example.apphipertension

data class SintomaGuardado(
    val documentId: String = "",
    val fecha: String,
    val hora: String,
    val sintomas: List<Sintoma>,
    val nota: String
)
