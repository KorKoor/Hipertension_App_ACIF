package com.example.apphipertension

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.CalendarView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.tuapp.utils.hideNavBar
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton


class Calendar : AppCompatActivity() {

    private lateinit var calendarView: CalendarView
    private lateinit var cardsContainer: LinearLayout
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- 1. CONFIGURACIÓN DE PANTALLA COMPLETA (MODO INMERSIVO) ---
        enableEdgeToEdge()
        setContentView(R.layout.activity_calendar)

        calendarView = findViewById(R.id.calendarView)
        cardsContainer = findViewById(R.id.cardsContainer)

        // 2. Configuración de Fecha Inicial (Hoy)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val fechaHoy = sdf.format(java.util.Date())
        updateCardsForSelectedDate(fechaHoy)

        // 3. Listener para cambio de fecha en el calendario
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val fechaSeleccionada = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            updateCardsForSelectedDate(fechaSeleccionada)
        }

        // 4. Configuración del Menú de Navegación
        setupBottomNavigation()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_calendar
    }

    // --- LÓGICA DE NAVEGACIÓN REVISADA ---
    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        hideNavBar(bottomNav)
        bottomNav.selectedItemId = R.id.nav_calendar

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == bottomNav.selectedItemId && item.itemId != R.id.nav_more) {
                return@setOnItemSelectedListener true
            }

            val intent = when (item.itemId) {
                R.id.nav_home -> Intent(this, MainActivity::class.java)
                R.id.nav_meds -> Intent(this, Medicate::class.java)
                R.id.nav_calendar -> null
                R.id.nav_profile -> Intent(this, Profile::class.java)
                R.id.nav_more -> {
                    showMoreMenuBottomSheet()
                    return@setOnItemSelectedListener false
                }
                else -> null
            }

            intent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                startActivity(it)
                overridePendingTransition(0, 0)
                true
            } ?: (item.itemId == R.id.nav_calendar)
        }
    }

    private fun showMoreMenuBottomSheet() {
        val sheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_more, null)
        sheet.setContentView(view)

        view.findViewById<LinearLayout>(R.id.llSintomas).setOnClickListener {
            startActivity(Intent(this, Sintomas::class.java))
            sheet.dismiss()
        }
        view.findViewById<LinearLayout>(R.id.llActividadFisica).setOnClickListener {
            startActivity(Intent(this, ActividadFisica::class.java))
            sheet.dismiss()
        }
        view.findViewById<LinearLayout>(R.id.llDieta).setOnClickListener {
            startActivity(Intent(this, Dieta::class.java))
            sheet.dismiss()
        }
        sheet.show()
    }

    // --- CARGA DE DATOS ORIGINAL SIN CAMBIOS ---
    private fun updateCardsForSelectedDate(dateStr: String) {
        cardsContainer.removeAllViews()
        val userId = auth.currentUser?.uid ?: return

        val layoutHTA = createSectionLayout("Presión Arterial (HTA)")
        val layoutSintomas = createSectionLayout("Resumen de Síntomas")
        val layoutDieta = createSectionLayout("Alimentación y Dieta")
        val layoutActividad = createSectionLayout("Actividad Física")
        val layoutMeds = createSectionLayout("Medicamentos")

        cardsContainer.addView(layoutHTA)
        cardsContainer.addView(layoutSintomas)
        cardsContainer.addView(layoutDieta)
        cardsContainer.addView(layoutActividad)
        cardsContainer.addView(layoutMeds)

        // 1. CARGAR HTA
                db.collection("users")
                    .document(userId)
                    .collection("mediciones")
                    .whereEqualTo("date", dateStr)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot.isEmpty) {
                            layoutHTA.addView(createEmptyMessage("Sin registros de presión este día."))
                        } else {
                            snapshot.documents.forEach { doc ->
                                val sistolica = doc.getLong("sistolica") ?: 0L
                                val diastolica = doc.getLong("diastolica") ?: 0L
                                val pulso = doc.getLong("pulso") ?: 0L
                                val hora = doc.getString("time") ?: ""
                                val nota = doc.getString("nota") ?: ""

                                val view = layoutInflater.inflate(R.layout.item_calendar_measurement, layoutHTA, false).apply {
                                    findViewById<TextView>(R.id.tvPresion).text = "$sistolica/$diastolica mmHg"
                                    findViewById<TextView>(R.id.tvPulso).text = "Pulso: $pulso LPM"
                                    findViewById<TextView>(R.id.tvFechaHora).text = "Hora: ${formatTo12Hours(hora)}"

                                    val tvNota = findViewById<TextView>(R.id.tvNota)
                                    if (nota.isNotEmpty()) {
                                        tvNota.text = "Nota: $nota"
                                        tvNota.visibility = View.VISIBLE
                                    }
                                }

                                // 👉 Acción para editar directamente con un diálogo desde el botón
                                val btnEditar = view.findViewById<MaterialButton>(R.id.btnEditar)
                                btnEditar.setOnClickListener {
                                    val dialogView = layoutInflater.inflate(R.layout.dialog_editar_presion, null)
                                    val etSistolica = dialogView.findViewById<EditText>(R.id.etSistolica)
                                    val etDiastolica = dialogView.findViewById<EditText>(R.id.etDiastolica)
                                    val etPulso = dialogView.findViewById<EditText>(R.id.etPulso)
                                    val etHora = dialogView.findViewById<EditText>(R.id.etHora)
                                    val etNota = dialogView.findViewById<EditText>(R.id.etNota)

                                    // Rellenar con valores actuales
                                    etSistolica.setText(sistolica.toString())
                                    etDiastolica.setText(diastolica.toString())
                                    etPulso.setText(pulso.toString())
                                    etHora.setText(hora)
                                    etNota.setText(nota)

                                    AlertDialog.Builder(this@Calendar)
                                        .setTitle("Editar presión arterial")
                                        .setView(dialogView)
                                        .setPositiveButton("Guardar") { dialogInterface, _ ->
                                            val nuevosDatos = mapOf(
                                                "sistolica" to etSistolica.text.toString().toIntOrNull(),
                                                "diastolica" to etDiastolica.text.toString().toIntOrNull(),
                                                "pulso" to etPulso.text.toString().toIntOrNull(),
                                                "time" to etHora.text.toString(),
                                                "nota" to etNota.text.toString()
                                            )

                                            db.collection("users")
                                                .document(userId)
                                                .collection("mediciones")
                                                .document(doc.id)
                                                .update(nuevosDatos)
                                                .addOnSuccessListener {
                                                    Toast.makeText(this@Calendar, "Registro actualizado", Toast.LENGTH_SHORT).show()
                                                    updateCardsForSelectedDate(dateStr)
                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(this@Calendar, "Error al actualizar", Toast.LENGTH_SHORT).show()
                                                }

                                            dialogInterface.dismiss()
                                        }
                                        .setNegativeButton("Cancelar", null)
                                        .show()
                                }
                                layoutHTA.addView(view)
                            }
                        }
                    }

        // 2. CARGAR SÍNTOMAS
        db.collection("users").document(userId).collection("sintomas")
            .whereEqualTo("fecha", dateStr)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    layoutSintomas.addView(createEmptyMessage("No se registraron síntomas."))
                } else {
                    for (doc in result) {
                        val view = layoutInflater.inflate(R.layout.item_calendar_sintoma, layoutSintomas, false)
                        val listaRaw = doc.get("sintomas") as? List<Map<String, Any>>
                        val resumen = listaRaw?.joinToString(", ") { it["nombre"].toString() } ?: "Ninguno"
                        view.findViewById<TextView>(R.id.tvSintomaHora).text = "Hora: ${formatTo12Hours(doc.getString("hora") ?: "")}"
                        view.findViewById<TextView>(R.id.tvSintomaLista).text = "Reporte: $resumen"
                        layoutSintomas.addView(view)
                    }
                }
            }

        // 3. CARGAR DIETA
        db.collection("users").document(userId).collection("registros_dieta")
            .document(dateStr).get().addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val view = layoutInflater.inflate(R.layout.item_calendar_dieta, layoutDieta, false)
                    val registro = doc.toObject<RegistroDieta>()
                    view.findViewById<TextView>(R.id.tvDietaTotalCalorias).text =
                        "Consumo diario: ${registro?.calorias_totales_dia?.toInt() ?: 0} kcal"
                    layoutDieta.addView(view)
                } else {
                    layoutDieta.addView(createEmptyMessage("No hay registro de dieta."))
                }
            }

        // 4. CARGAR ACTIVIDAD
        db.collection("users").document(userId).collection("actividades_fisicas")
            .whereEqualTo("fecha", dateStr).get().addOnSuccessListener { result ->
                if (result.isEmpty) {
                    layoutActividad.addView(createEmptyMessage("Sin actividad física registrada."))
                } else {
                    for (doc in result) {
                        val view = layoutInflater.inflate(R.layout.item_calendar_actividad, layoutActividad, false)
                        val actividades = doc.get("actividades") as? List<Map<String, Any>>
                        val resumen = actividades?.joinToString(", ") { "${it["nombre"]} (${it["duracionEnMinutos"]} min)" }
                        view.findViewById<TextView>(R.id.tvActividadLista).text = resumen
                        layoutActividad.addView(view)
                    }
                }
            }

        // 5. CARGAR MEDICAMENTOS
        db.collection("users").document(userId).collection("medicamentos")
            .whereEqualTo("fecha", dateStr).get().addOnSuccessListener { result ->
                if (result.isEmpty) {
                    layoutMeds.addView(createEmptyMessage("No se registraron medicamentos tomados."))
                } else {
                    for (doc in result) {
                        val view = layoutInflater.inflate(R.layout.item_calendar_medicine, layoutMeds, false)
                        view.findViewById<TextView>(R.id.tvMedicineNombre).text = doc.getString("nombre")
                        view.findViewById<TextView>(R.id.tvMedicineHora).text = "Toma: ${formatTo12Hours(doc.getString("hora") ?: "")}"
                        layoutMeds.addView(view)
                    }
                }
            }
    }

    private fun formatTo12Hours(time24: String): String {
        return try {
            LocalTime.parse(time24).format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))
        } catch (e: Exception) { time24 }
    }

    private fun createSectionLayout(title: String): LinearLayout {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(0, 20, 0, 10)
        val textView = TextView(this)
        textView.text = title
        textView.textSize = 17f
        textView.setTypeface(null, Typeface.BOLD)
        textView.setTextColor(Color.parseColor("#466B95"))
        textView.setPadding(10, 10, 10, 5)
        layout.addView(textView)
        return layout
    }

    private fun createEmptyMessage(message: String): TextView {
        val textView = TextView(this)
        textView.text = message
        textView.setTextColor(Color.GRAY)
        textView.textSize = 14f
        textView.setPadding(30, 0, 0, 10)
        return textView
    }
}