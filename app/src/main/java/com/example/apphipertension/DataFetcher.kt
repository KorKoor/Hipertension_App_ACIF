package com.example.apphipertension

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await

object DataFetcher {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getUid(): String? = auth.currentUser?.uid

    suspend fun fetchAllReportData(): AnalysisReport? {
        val uid = getUid() ?: return null

        try {
            // 1. Cargar Perfil del Usuario
            val profileDoc = db.collection("users").document(uid).get().await()
            val profileData = profileDoc.let { doc ->
                ProfileData(
                    nombre = doc.getString("nombre") ?: auth.currentUser?.email ?: "Usuario",
                    correo = auth.currentUser?.email ?: "",
                    peso = doc.getString("peso") ?: "N/A",
                    altura = doc.getString("altura") ?: "N/A",
                    imc = doc.getString("imc") ?: "N/A",
                    fechaNacimiento = doc.getString("fecha_nacimiento") ?: "N/A",
                    edad = doc.getString("edad") ?: "N/A",
                    sexo = doc.getString("sexo") ?: "N/A",
                    proxima_cita_medica = doc.getString("proxima_cita_medica") ?: "N/A",
                    meta_calorias_diarias = doc.getDouble("meta_calorias_diarias") ?: 0.0,
                    alimentosEvitar_pre = doc.get("alimentosEvitar_pre") as? List<String> ?: emptyList(),
                    alimentosEvitar_per = doc.get("alimentosEvitar_per") as? List<String> ?: emptyList(),
                    medicamentosEvitar = doc.get("medicamentosEvitar") as? List<String> ?: emptyList(),
                    padecimientos_pre = doc.get("padecimientos_pre") as? List<String> ?: emptyList(),
                    padecimientos_per = doc.get("padecimientos_per") as? List<String> ?: emptyList()
                )
            }

            // --- CONSULTAS CON MANEJO INDIVIDUAL DE ERRORES ---

            // 2. Mediciones de Presión
            val mediciones = try {
                db.collection("users").document(uid).collection("mediciones")
                    .orderBy("date", Query.Direction.DESCENDING)
                    .orderBy("time", Query.Direction.DESCENDING)
                    .get().await().toObjects(Measurement::class.java)
            } catch (e: Exception) {
                Log.e("DataFetcher", "Error en Mediciones (Posible falta de índice): ${e.message}")
                emptyList()
            }

            // 3. Medicamentos
            val medicamentos = try {
                db.collection("users").document(uid).collection("medicamentos")
                    .orderBy("fecha", Query.Direction.DESCENDING)
                    .get().await().toObjects(Medicine::class.java)
            } catch (e: Exception) {
                Log.e("DataFetcher", "Error en Medicamentos: ${e.message}")
                emptyList()
            }

            // 4. Síntomas (Mapeo Manual Seguro)
            val sintomas = try {
                val snapshot = db.collection("users").document(uid).collection("sintomas")
                    .orderBy("fecha", Query.Direction.DESCENDING)
                    .orderBy("hora", Query.Direction.DESCENDING)
                    .get().await()

                snapshot.documents.mapNotNull { doc ->
                    val sintomasRaw = doc.get("sintomas") as? List<Map<String, Any>> ?: emptyList()
                    val sintomaList = sintomasRaw.map { item ->
                        Sintoma(
                            id = item["id"] as? String ?: "",
                            nombre = item["nombre"] as? String ?: "Sintoma desconocido",
                            iconResId = 0,
                            seleccionado = true,
                            nota = if (item["id"] == "otro") item["nombre"] as? String else null
                        )
                    }
                    SintomaGuardado(
                        documentId = doc.id,
                        fecha = doc.getString("fecha") ?: "N/A",
                        hora = doc.getString("hora") ?: "N/A",
                        sintomas = sintomaList,
                        nota = doc.getString("nota") ?: ""
                    )
                }
            } catch (e: Exception) {
                Log.e("DataFetcher", "Error en Síntomas (Revisar índice): ${e.message}")
                emptyList()
            }

            // 5. Registros de Dieta
            val registrosDieta = try {
                db.collection("users").document(uid).collection("registros_dieta")
                    .orderBy(com.google.firebase.firestore.FieldPath.documentId(), Query.Direction.DESCENDING)
                    .get().await().mapNotNull { it.toObject<RegistroDieta>() }
            } catch (e: Exception) {
                Log.e("DataFetcher", "Error en Dieta: ${e.message}")
                emptyList()
            }

            // 6. Actividad Física (Corrección de tipos de datos)
            val registrosActividad = try {
                val snapshot = db.collection("users").document(uid).collection("actividades_fisicas")
                    .orderBy("fecha", Query.Direction.DESCENDING)
                    .orderBy("hora", Query.Direction.DESCENDING)
                    .get().await()

                snapshot.documents.mapNotNull { doc ->
                    val actividadesRaw = doc.get("actividades") as? List<Map<String, Any>> ?: emptyList()
                    val actividades = actividadesRaw.map { map ->
                        ActividadRegistrada(
                            id = map["id"] as? String ?: "",
                            nombre = map["nombre"] as? String ?: "Desconocida",
                            // Firestore devuelve números como Long, convertimos a Int
                            duracionEnMinutos = (map["duracionEnMinutos"] as? Long)?.toInt() ?: 0
                        )
                    }
                    ActividadGuardada(
                        documentId = doc.id,
                        fecha = doc.getString("fecha") ?: "",
                        hora = doc.getString("hora") ?: "",
                        actividades = actividades,
                        nota = doc.getString("nota")
                    )
                }
            } catch (e: Exception) {
                Log.e("DataFetcher", "Error en Actividad Física (Revisar índice): ${e.message}")
                emptyList()
            }

            // Retornar el reporte completo
            return AnalysisReport(
                profileData,
                mediciones,
                medicamentos,
                sintomas,
                registrosDieta,
                registrosActividad
            )

        } catch (e: Exception) {
            Log.e("DataFetcher", "Error crítico al obtener datos del reporte", e)
            return null
        }
    }
}