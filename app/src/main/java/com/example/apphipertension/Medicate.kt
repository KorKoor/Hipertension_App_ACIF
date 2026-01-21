package com.example.apphipertension

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tuapp.utils.hideNavBar
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

class Medicate : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_medicate)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        // Llamada global
        hideNavBar(bottomNav)


        val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewMedicamentos)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val tvEmptyMedicamentos = findViewById<TextView>(R.id.tvEmptyMedicamentos)

        fun cargarYMostrarMedicamentos() {
            cargarMedicamentos { lista ->
                val adapter = MedicamentoAdapter(
                    lista,
                    onEdit = { medicine ->
                        showEditMedicineDialog(medicine) {
                            cargarYMostrarMedicamentos()
                        }
                    },
                    onDelete = { medicine ->
                        eliminarMedicamento(medicine) {
                            cargarYMostrarMedicamentos()
                        }
                    },
                    onToggleReminder = { medicine, isEnabled ->
                        actualizarRecordatorio(medicine, isEnabled)
                    }
                )
                recyclerView.adapter = adapter
                tvEmptyMedicamentos.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            }
        }
        cargarYMostrarMedicamentos()

        val btnAdd = findViewById<Button>(R.id.btnAddMedicine)
        btnAdd.setOnClickListener {
            showAddMedicineDialog {
                cargarYMostrarMedicamentos()
            }
        }

        // --- 2. NAVEGACIÓN INFERIOR CORREGIDA ---
        bottomNav.selectedItemId = R.id.nav_meds

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == bottomNav.selectedItemId && item.itemId != R.id.nav_more) {
                return@setOnItemSelectedListener true
            }

            val intent = when (item.itemId) {
                R.id.nav_home -> Intent(this, MainActivity::class.java)
                R.id.nav_meds -> null // Ya estamos aquí
                R.id.nav_calendar -> Intent(this, Calendar::class.java)
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
                overridePendingTransition(0, 0) // Transición suave
                true
            } ?: (item.itemId == R.id.nav_meds)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


    }

    override fun onResume() {
        super.onResume()
        // Aseguramos que el ícono correcto esté seleccionado al volver a la actividad
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_meds
    }

    private fun actualizarRecordatorio(medicine: Medicine, activar: Boolean) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(user.uid)
            .collection("medicamentos").document(medicine.id)
            .update("tieneRecordatorio", activar)
            .addOnSuccessListener {
                if (activar) {
                    MedicationAlarmScheduler.programarAlarma(this, medicine)
                    Toast.makeText(this, "Recordatorio activado", Toast.LENGTH_SHORT).show()
                } else {
                    MedicationAlarmScheduler.cancelarAlarma(this, medicine)
                    Toast.makeText(this, "Recordatorio desactivado", Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun guardarMedicamento(nombre: String, dosis: String, unidad: String, hora: String, frecuencia: String, fecha: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        val nuevoDoc = db.collection("users").document(user.uid).collection("medicamentos").document()

        val randomAlarmId = (System.currentTimeMillis() % 1000000).toInt()

        val medicamento = hashMapOf(
            "nombre" to nombre,
            "dosis" to dosis,
            "unidad" to unidad,
            "hora" to hora,
            "frecuencia" to frecuencia,
            "fecha" to fecha,
            "tieneRecordatorio" to false,
            "alarmId" to randomAlarmId
        )

        nuevoDoc.set(medicamento)
            .addOnSuccessListener { /* Éxito */ }
            .addOnFailureListener { /* Error */ }
    }

    fun cargarMedicamentos(onResult: (List<Medicine>) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .document(user.uid)
            .collection("medicamentos")
            .get()
            .addOnSuccessListener { result ->
                val lista = result.documents.map { doc ->
                    val datos = doc.data ?: emptyMap<String, Any>()
                    val alarmIdLong = datos["alarmId"] as? Long ?: datos["alarmId"] as? Int ?: doc.id.hashCode()

                    Medicine(
                        id = doc.id,
                        nombre = datos["nombre"] as? String ?: "",
                        dosis = datos["dosis"] as? String ?: "",
                        unidad = datos["unidad"] as? String ?: "",
                        hora = datos["hora"] as? String ?: "",
                        frecuencia = datos["frecuencia"] as? String ?: "",
                        fecha = datos["fecha"] as? String ?: "",
                        tieneRecordatorio = datos["tieneRecordatorio"] as? Boolean ?: false,
                        alarmId = alarmIdLong.toInt()
                    )
                }
                onResult(lista)
            }
    }

    private fun showMoreMenuBottomSheet() {
        val sheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_more, null)
        sheet.setContentView(view)

        view.findViewById<LinearLayout>(R.id.llAnalisis).setOnClickListener {
            startActivity(Intent(this, Analysis::class.java))
            sheet.dismiss()
        }
        view.findViewById<LinearLayout>(R.id.llSintomas).setOnClickListener {
            startActivity(Intent(this, Sintomas::class.java))
            sheet.dismiss()
        }
        view.findViewById<LinearLayout>(R.id.llDieta).setOnClickListener {
            startActivity(Intent(this, Dieta::class.java))
            sheet.dismiss()
        }
        view.findViewById<LinearLayout>(R.id.llActividadFisica).setOnClickListener {
            startActivity(Intent(this, ActividadFisica::class.java))
            sheet.dismiss()
        }
        sheet.show()
    }



    class MedicamentoAdapter(
        private val items: List<Medicine>,
        private val onEdit: (Medicine) -> Unit,
        private val onDelete: (Medicine) -> Unit,
        private val onToggleReminder: (Medicine, Boolean) -> Unit
    ) : RecyclerView.Adapter<MedicamentoAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val fecha = view.findViewById<TextView>(R.id.tvFecha)
            val nombre = view.findViewById<TextView>(R.id.tvNombre)
            val dosisUnidad = view.findViewById<TextView>(R.id.tvDosisUnidad)
            val frecuencia = view.findViewById<TextView>(R.id.tvFrecuencia)
            val hora = view.findViewById<TextView>(R.id.tvHora)
            val btnEditar = view.findViewById<Button>(R.id.btnEditar)
            val btnQuitar = view.findViewById<Button>(R.id.btnQuitar)
            val switchRecordatorio = view.findViewById<SwitchMaterial>(R.id.switchRecordatorio)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_medicine, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val m = items[position]
            holder.fecha.text = m.fecha
            holder.nombre.text = m.nombre
            holder.dosisUnidad.text = "${m.dosis} ${m.unidad}"
            holder.frecuencia.text = m.frecuencia
            holder.hora.text = m.hora

            holder.switchRecordatorio.setOnCheckedChangeListener(null)
            holder.switchRecordatorio.isChecked = m.tieneRecordatorio

            holder.switchRecordatorio.setOnCheckedChangeListener { _, isChecked ->
                onToggleReminder(m, isChecked)
            }

            holder.btnEditar.setOnClickListener { onEdit(m) }
            holder.btnQuitar.setOnClickListener { onDelete(m) }
        }

        override fun getItemCount() = items.size
    }

    fun eliminarMedicamento(medicine: Medicine, onSuccess: () -> Unit) {
        MedicationAlarmScheduler.cancelarAlarma(this, medicine)

        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(user.uid).collection("medicamentos")
            .document(medicine.id).delete()
            .addOnSuccessListener { onSuccess() }
    }

    private fun showAddMedicineDialog(onSaved: () -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_medicine, null)
        val etFecha = dialogView.findViewById<EditText>(R.id.etFechaMed)
        val etNombre = dialogView.findViewById<EditText>(R.id.etNombreMed)
        val etDosis = dialogView.findViewById<EditText>(R.id.etDosisMed)
        val spinnerUnidad = dialogView.findViewById<Spinner>(R.id.spinnerUnidad)
        val etHora = dialogView.findViewById<EditText>(R.id.etHoraMed)
        val spinnerFrecuencia = dialogView.findViewById<Spinner>(R.id.spinnerFrecuencia)

        val formato = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
        var fechaSeleccionada = java.time.LocalDate.now()
        etFecha.setText(fechaSeleccionada.format(formato))

        etFecha.setOnClickListener {
            val actual = fechaSeleccionada
            val dp = android.app.DatePickerDialog(
                this,
                { _, y, m, d ->
                    fechaSeleccionada = java.time.LocalDate.of(y, m + 1, d)
                    etFecha.setText(fechaSeleccionada.format(formato))
                },
                actual.year, actual.monthValue - 1, actual.dayOfMonth
            )
            dp.show()
        }

        var horaSeleccionada = java.time.LocalTime.now()
        etHora.setText(horaSeleccionada.format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a")))

        etHora.setOnClickListener {
            val tp = android.app.TimePickerDialog(
                this,
                { _, h, min ->
                    horaSeleccionada = java.time.LocalTime.of(h, min)
                    val horaFormateada = horaSeleccionada.format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"))
                    etHora.setText(horaFormateada)
                },
                horaSeleccionada.hour, horaSeleccionada.minute, false
            )
            tp.show()
        }

        AlertDialog.Builder(this)
            .setTitle("Añadir medicamento")
            .setView(dialogView)
            .setPositiveButton("Guardar") { dialog, _ ->
                val fecha = etFecha.text.toString().trim()
                val nombre = etNombre.text.toString().trim()
                val dosis = etDosis.text.toString().trim()
                val unidad = spinnerUnidad.selectedItem.toString()
                val hora = etHora.text.toString().trim()
                val frecuencia = spinnerFrecuencia.selectedItem.toString()

                if (fecha.isNotBlank() && nombre.isNotBlank() && dosis.isNotBlank() && hora.isNotBlank()) {
                    guardarMedicamento(nombre, dosis, unidad, hora, frecuencia, fecha)
                    onSaved()
                } else {
                    Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditMedicineDialog(medicine: Medicine, onSaved: () -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_medicine, null)
        val etFecha = dialogView.findViewById<EditText>(R.id.etFechaMed)
        val etNombre = dialogView.findViewById<EditText>(R.id.etNombreMed)
        val etDosis = dialogView.findViewById<EditText>(R.id.etDosisMed)
        val spinnerUnidad = dialogView.findViewById<Spinner>(R.id.spinnerUnidad)
        val etHora = dialogView.findViewById<EditText>(R.id.etHoraMed)
        val spinnerFrecuencia = dialogView.findViewById<Spinner>(R.id.spinnerFrecuencia)

        val formatoFecha = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val formatoHora = java.time.format.DateTimeFormatter.ofPattern("hh:mm a")

        etFecha.setText(medicine.fecha ?: "")
        etNombre.setText(medicine.nombre)
        etDosis.setText(medicine.dosis)

        val horaLimpia = medicine.hora
            .replace(".", "")
            .replace(" ", " ")
            .uppercase()
            .trim()

        val horaLocal = try {
            java.time.LocalTime.parse(horaLimpia, formatoHora)
        } catch (e: Exception) {
            try {
                java.time.LocalTime.parse(horaLimpia)
            } catch (e: Exception) {
                java.time.LocalTime.now()
            }
        }
        etHora.setText(horaLocal.format(formatoHora))

        val unidades = resources.getStringArray(R.array.unidades_medicina)
        spinnerUnidad.setSelection(unidades.indexOf(medicine.unidad))

        val frecuencias = resources.getStringArray(R.array.frecuencias_medicina)
        spinnerFrecuencia.setSelection(frecuencias.indexOf(medicine.frecuencia))

        etFecha.setOnClickListener {
            val fechaActual = try {
                java.time.LocalDate.parse(etFecha.text.toString(), formatoFecha)
            } catch (e: Exception) {
                java.time.LocalDate.now()
            }
            val dp = android.app.DatePickerDialog(
                this,
                { _, y, m, d ->
                    val nuevaFecha = java.time.LocalDate.of(y, m + 1, d)
                    etFecha.setText(nuevaFecha.format(formatoFecha))
                },
                fechaActual.year, fechaActual.monthValue - 1, fechaActual.dayOfMonth
            )
            dp.show()
        }

        etHora.setOnClickListener {
            val horaActual = try {
                val hLimpia = etHora.text.toString().replace(".", "").uppercase().trim()
                java.time.LocalTime.parse(hLimpia, formatoHora)
            } catch (e: Exception) {
                java.time.LocalTime.now()
            }
            val tp = android.app.TimePickerDialog(
                this,
                { _, h, min ->
                    val nuevaHora = java.time.LocalTime.of(h, min)
                    etHora.setText(nuevaHora.format(formatoHora))
                },
                horaActual.hour, horaActual.minute, false
            )
            tp.show()
        }

        AlertDialog.Builder(this)
            .setTitle("Editar medicamento")
            .setView(dialogView)
            .setPositiveButton("Guardar") { dialog, _ ->
                val nuevaFecha = etFecha.text.toString().trim()
                val nuevoNombre = etNombre.text.toString().trim()
                val nuevaDosis = etDosis.text.toString().trim()
                val nuevaUnidad = spinnerUnidad.selectedItem.toString()
                val nuevaHora = etHora.text.toString().trim()
                val nuevaFrecuencia = spinnerFrecuencia.selectedItem.toString()

                if (nuevaFecha.isNotBlank() && nuevoNombre.isNotBlank() && nuevaDosis.isNotBlank() && nuevaHora.isNotBlank()) {
                    val user = FirebaseAuth.getInstance().currentUser ?: return@setPositiveButton

                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(user.uid)
                        .collection("medicamentos")
                        .document(medicine.id)
                        .update(
                            mapOf(
                                "fecha" to nuevaFecha,
                                "nombre" to nuevoNombre,
                                "dosis" to nuevaDosis,
                                "unidad" to nuevaUnidad,
                                "hora" to nuevaHora,
                                "frecuencia" to nuevaFrecuencia
                            )
                        )
                        .addOnSuccessListener {
                            Toast.makeText(this, "Medicamento actualizado", Toast.LENGTH_SHORT).show()

                            if (medicine.tieneRecordatorio) {
                                val medicinaActualizada = medicine.copy(
                                    nombre = nuevoNombre,
                                    dosis = nuevaDosis,
                                    unidad = nuevaUnidad,
                                    hora = nuevaHora,
                                    frecuencia = nuevaFrecuencia
                                )
                                MedicationAlarmScheduler.programarAlarma(this, medicinaActualizada)
                            }

                            onSaved()
                        }
                } else {
                    Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

}