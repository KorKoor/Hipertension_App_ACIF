package com.example.apphipertension

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Reemplaza a 'Sintoma'.
// Añadimos 'duracionEnMinutos' para guardar el tiempo
@Parcelize
data class Actividad(
    val id: String,
    val nombre: String,
    val iconResId: Int,
    var seleccionado: Boolean = false,
    var duracionEnMinutos: Int = 0,
    var nota: String? = null
) : Parcelable

// Esta es la clase que guardaremos en Firestore (solo la actividad y su duración)
// La usaremos para la lista dentro del documento de historial
@Parcelize
data class ActividadRegistrada(
    val id: String = "",
    val nombre: String = "",
    val duracionEnMinutos: Int = 0
) : Parcelable

// Reemplaza a 'SintomaGuardado'.
// Esta es la clase para la lista del historial
@Parcelize
data class ActividadGuardada(
    val documentId: String,
    val fecha: String,
    val hora: String,
    val actividades: List<ActividadRegistrada>, // Lista de actividades con duración
    val nota: String? = null
) : Parcelable
