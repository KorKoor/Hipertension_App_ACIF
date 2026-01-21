package com.example.apphipertension

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MeasurementAdapter
    private lateinit var tvEmpty: TextView
    private val measurementList = mutableListOf<MeasurementWithId>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        fun hideNavBar() {
            insetsController.hide(WindowInsetsCompat.Type.navigationBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            // Rehabilitar interacción de tu navbar
            bottomNav.isEnabled = true
            bottomNav.alpha = 1f
        }

        // 1. Configuración de pantalla completa
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)

        // 3. Toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Historial de Presión"
            setDisplayHomeAsUpEnabled(true)
        }
        toolbar.setNavigationOnClickListener { finish() }

        // 4. Inicialización de Vistas
        tvEmpty = findViewById(R.id.tvEmptyHistorial)
        recyclerView = findViewById(R.id.recyclerViewHistory)

        // 5. Configuración de componentes y datos
        setupRecyclerView()
        setupBottomNavigation()
        recargarHistorial()

        // 6. CORRECCIÓN VITAL DE INSETS: Evita que la Activity se detenga por errores de renderizado
        val mainLayout = findViewById<View>(R.id.main)
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                // Al devolver CONSUMED, evitamos conflictos que cierran la ventana en algunos dispositivos
                WindowInsetsCompat.CONSUMED
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Solo reseteamos el estado visual de la navbar sin disparar navegación
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.menu.findItem(R.id.nav_home)?.isChecked = true
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = MeasurementAdapter(
            measurementList,
            onEdit = { measurement -> showEditMeasurementDialog(measurement) },
            onDelete = { measurement -> confirmarEliminacion(measurement) }
        )
        recyclerView.adapter = adapter
    }

    private fun recargarHistorial() {
        val currentUser = auth.currentUser

        Log.d("HISTORIAL_CHECK", "Iniciando carga de datos...")

        if (currentUser == null) {
            Log.e("HISTORIAL_CHECK", "Usuario nulo en Firebase")
            Toast.makeText(this, "Sesión no válida", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid

        // Se restauran los orderBy para una visualización correcta (Requiere índices en Firestore)
        db.collection("users").document(userId)
            .collection("mediciones")
            .orderBy("date", Query.Direction.DESCENDING)
            .orderBy("time", Query.Direction.DESCENDING)
            .addSnapshotListener { result, e ->
                if (e != null) {
                    Log.e("HISTORIAL_CHECK", "Error Firestore: ${e.message}")
                    // Si falla por falta de índices, se mostrará este Toast
                    Toast.makeText(this, "Error de sincronización", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (result != null) {
                    Log.d("HISTORIAL_CHECK", "Datos recibidos: ${result.size()}")
                    measurementList.clear()
                    for (doc in result) {
                        val medicion = doc.toObject(Measurement::class.java)
                        measurementList.add(MeasurementWithId(doc.id, medicion))
                    }
                    adapter.notifyDataSetChanged()
                    tvEmpty.visibility = if (measurementList.isEmpty()) View.VISIBLE else View.GONE
                }
            }
    }

    private fun confirmarEliminacion(medicion: MeasurementWithId) {
        val builder = AlertDialog.Builder(this)
            .setTitle("¿Eliminar registro?")
            .setMessage("Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                val userId = auth.currentUser?.uid ?: return@setPositiveButton
                db.collection("users").document(userId)
                    .collection("mediciones").document(medicion.id)
                    .delete()
                    .addOnSuccessListener { Toast.makeText(this, "Eliminado", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Cancelar", null)

        val dialog = builder.create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#466B95"))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#466B95"))
    }

    private fun showEditMeasurementDialog(medicionId: MeasurementWithId) {
        val measurement = medicionId.measurement
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_pressure, null)

        val editSistolica = dialogView.findViewById<EditText>(R.id.editSistolica)
        val editDiastolica = dialogView.findViewById<EditText>(R.id.editDiastolica)
        val editPulso = dialogView.findViewById<EditText>(R.id.editPulso)
        val editNota = dialogView.findViewById<EditText>(R.id.editNota)
        val editFecha = dialogView.findViewById<EditText>(R.id.editFecha)
        val editHora = dialogView.findViewById<EditText>(R.id.editHora)

        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val timeFormatter24 = DateTimeFormatter.ofPattern("HH:mm")
        val timeFormatter12 = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())

        var selectedDate = LocalDate.parse(measurement.date)
        var selectedTime = LocalTime.parse(measurement.time)

        editSistolica.setText(measurement.sistolica.toString())
        editDiastolica.setText(measurement.diastolica.toString())
        editPulso.setText(measurement.pulso.toString())
        editNota.setText(measurement.nota ?: "")
        editFecha.setText(selectedDate.format(dateFormatter))
        editHora.setText(selectedTime.format(timeFormatter12))

        editFecha.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                selectedDate = LocalDate.of(y, m + 1, d)
                editFecha.setText(selectedDate.format(dateFormatter))
            }, selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth).show()
        }

        editHora.setOnClickListener {
            TimePickerDialog(this, { _, h, min ->
                selectedTime = LocalTime.of(h, min)
                editHora.setText(selectedTime.format(timeFormatter12))
            }, selectedTime.hour, selectedTime.minute, false).show()
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Editar Medición")
            .setView(dialogView)
            .setPositiveButton("Actualizar") { _, _ ->
                val s = editSistolica.text.toString().toIntOrNull()
                val d = editDiastolica.text.toString().toIntOrNull()
                val p = editPulso.text.toString().toIntOrNull()

                if (s != null && d != null && p != null) {
                    val userId = auth.currentUser?.uid ?: return@setPositiveButton
                    val update = measurement.copy(
                        date = selectedDate.format(dateFormatter),
                        time = selectedTime.format(timeFormatter24),
                        sistolica = s,
                        diastolica = d,
                        pulso = p,
                        nota = editNota.text.toString()
                    )
                    db.collection("users").document(userId)
                        .collection("mediciones").document(medicionId.id)
                        .set(update)
                }
            }
            .setNegativeButton("Cerrar", null)
            .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#466B95"))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#466B95"))
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            val intent = when (item.itemId) {
                R.id.nav_home -> Intent(this, MainActivity::class.java)
                R.id.nav_meds -> Intent(this, Medicate::class.java)
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
                overridePendingTransition(0, 0)
                true
            } ?: false
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
        sheet.show()
    }
}

// --- ADAPTADOR ---

class MeasurementAdapter(
    private val items: List<MeasurementWithId>,
    private val onEdit: (MeasurementWithId) -> Unit,
    private val onDelete: (MeasurementWithId) -> Unit
) : RecyclerView.Adapter<MeasurementAdapter.MeasurementViewHolder>() {

    class MeasurementViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateTime: TextView = view.findViewById(R.id.tvFechaHora)
        val pressure: TextView = view.findViewById(R.id.tvPresion)
        val pulse: TextView = view.findViewById(R.id.tvPulso)
        val note: TextView = view.findViewById(R.id.tvNota)
        val btnEditar: Button = view.findViewById(R.id.btnEditar)
        val btnQuitar: Button = view.findViewById(R.id.btnQuitar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MeasurementViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_measurement, parent, false)
        return MeasurementViewHolder(view)
    }

    override fun onBindViewHolder(holder: MeasurementViewHolder, position: Int) {
        val item = items[position]
        val m = item.measurement

        val horaFormateada = try {
            LocalTime.parse(m.time).format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))
        } catch (e: Exception) { m.time }

        holder.dateTime.text = "${m.date} • $horaFormateada"
        holder.pressure.text = "Presión: ${m.sistolica}/${m.diastolica} mmHg"
        holder.pulse.text = "Pulso: ${m.pulso} LPM"

        if (m.nota.isNullOrEmpty()) { // Corregido: m.note en lugar de m.nota según tu data class
            holder.note.visibility = View.GONE
        } else {
            holder.note.visibility = View.VISIBLE
            holder.note.text = "Nota: ${m.nota}"
        }

        holder.btnEditar.setOnClickListener { onEdit(item) }
        holder.btnQuitar.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount() = items.size
}