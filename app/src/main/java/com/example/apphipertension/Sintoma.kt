package com.example.apphipertension

import java.io.Serializable

data class Sintoma(val id: String,                  // Puede ser "mareos", "dolor_cabeza", etc.
                   val nombre: String,              // Texto para mostrar
                   val iconResId: Int,              // id de drawable
                   var seleccionado: Boolean = false,
                   var nota: String? = null         // Para “Otro…” u observaciones opcionales
): Serializable
