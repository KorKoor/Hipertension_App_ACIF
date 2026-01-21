package com.example.apphipertension

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.text.InputType
import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ActividadFisica : AppCompatActivity() {

    // ¡CAMBIO! Nombres de variables
    private lateinit var actividadAdapter: ActividadAdapter
    private lateinit var btnGuardar: Button
    private lateinit var etNota: EditText
    private lateinit var btnSeleccionarFecha: Button
    private lateinit var btnSeleccionarHora: Button
    private lateinit var rvActividades: RecyclerView
    private lateinit var tvError: TextView

    private var fechaSeleccionada: LocalDate = LocalDate.now()
    private var horaSeleccionada: LocalTime = LocalTime.now()
    private var documentIdEnEdicion: String? = null

    // ¡CAMBIO! Lista de actividades
    val actividadesBase = mutableListOf(
        Actividad("caminar", "Caminar", R.drawable.caminar),
        Actividad("nadar", "Nadar", R.drawable.nadar),
        Actividad("trotar", "Trotar", R.drawable.trotar),
        Actividad("spinning", "Spinning", R.drawable.spinning),
        Actividad("pilates", "Pilates", R.drawable.pilates),
        Actividad("yoga", "Yoga / Estiramientos", R.drawable.yoga),
        Actividad("zumba", "Zumba", R.drawable.zumba),
        Actividad("gimnasio", "Gimnasio", R.drawable.gimnasio),
        Actividad("ejercicio", "Ejercicio en casa", R.drawable.ejerciciocasa),
        Actividad("otro", "Otro...", R.drawable.ic_otro)
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // ¡CAMBIO! Layout
        setContentView(R.layout.activity_actividad_fisica)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // ¡CAMBIO! IDs
        rvActividades = findViewById(R.id.rvActividades)
        btnSeleccionarFecha = findViewById(R.id.btnSeleccionarFecha)
        btnSeleccionarHora = findViewById(R.id.btnSeleccionarHora)
        btnGuardar = findViewById(R.id.btnGuardarActividad)
        etNota = findViewById(R.id.etNotaActividad)
        tvError = findViewById(R.id.tvError)

        btnSeleccionarFecha.text = fechaSeleccionada.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        btnSeleccionarHora.text = horaSeleccionada.format(DateTimeFormatter.ofPattern("hh:mm a"))

        // ¡CAMBIO! Lógica de clic
        actividadAdapter = ActividadAdapter(actividadesBase) { actividad, position ->
            if (actividad.seleccionado) {
                // Si ya está seleccionado, lo deselecciona
                actividadesBase[position].seleccionado = false
                actividadesBase[position].duracionEnMinutos = 0
                actividadesBase[position].nota = null // (Si usas la data class de sintoma)
                actividadAdapter.notifyItemChanged(position)
            } else {
                // Si NO está seleccionado, muestra diálogo para agregar duración
                if (actividad.id == "otro") {
                    // Diálogo especial para "Otro"
                    showDialogOtraActividad { nombre, duracion ->
                        actividadesBase[position].seleccionado = true
                        actividadesBase[position].nota = nombre // Usamos 'nota' para el nombre
                        actividadesBase[position].duracionEnMinutos = duracion
                        actividadAdapter.notifyItemChanged(position)
                    }
                } else {
                    // Diálogo normal para duración
                    showDialogDuracion(actividad) { duracion ->
                        actividadesBase[position].seleccionado = true
                        actividadesBase[position].duracionEnMinutos = duracion
                        actividadAdapter.notifyItemChanged(position)
                    }
                }
            }
            tvError.visibility = View.GONE
        }

        val spanCount = calculateNoOfColumns(120)
        rvActividades.layoutManager = GridLayoutManager(this, spanCount)
        rvActividades.adapter = actividadAdapter

        // --- Lógica de Edición --- (Adaptada)
        val extras = intent.extras
        // (En ActividadFisica.kt -> onCreate)
        if (extras != null && extras.containsKey(HistoryActividadFisica.EXTRA_ACTIVIDAD_DOC_ID)) {
            precargarActividadParaEdicion(extras)
        }

        // DatePicker (Sin cambios)
        btnSeleccionarFecha.setOnClickListener {
            val hoy = fechaSeleccionada
            val picker = DatePickerDialog(this, { _, year, month, dayOfMonth ->
                val fecha = LocalDate.of(year, month + 1, dayOfMonth)
                fechaSeleccionada = fecha
                btnSeleccionarFecha.text = fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            },
                hoy.year, hoy.monthValue - 1, hoy.dayOfMonth
            )
            picker.show()
        }

        // TimePicker (Sin cambios)
        btnSeleccionarHora.setOnClickListener {
            val ahora = horaSeleccionada
            val picker = android.app.TimePickerDialog(this, { _, hourOfDay, minute ->
                val hora = LocalTime.of(hourOfDay, minute)
                horaSeleccionada = hora
                btnSeleccionarHora.text = hora.format(DateTimeFormatter.ofPattern("hh:mm a"))
            },
                ahora.hour, ahora.minute, false
            )
            picker.show()
        }

        // Botón Guardar / Actualizar
        btnGuardar.setOnClickListener {
            val fecha = fechaSeleccionada
            val hora = horaSeleccionada
            val nota = etNota.text.toString()
            val actividadesSeleccionadas = actividadesBase.filter { it.seleccionado }

            if (actividadesSeleccionadas.isEmpty()) {
                tvError.visibility = View.VISIBLE
            } else {
                tvError.visibility = View.GONE

                // ¡CAMBIO! Nueva función de guardado
                guardarOActualizarActividad(
                    fecha.format(DateTimeFormatter.ISO_DATE),
                    hora.format(DateTimeFormatter.ofPattern("HH:mm")),
                    actividadesSeleccionadas,
                    nota,
                    onSuccess = {
                        val mensaje = if (documentIdEnEdicion != null) "Actividad actualizada" else "Actividad guardada"
                        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
                        if (documentIdEnEdicion != null) {
                            finish()
                        } else {
                            actividadesBase.forEach { it.seleccionado = false; it.duracionEnMinutos = 0; it.nota = null }
                            actividadAdapter.notifyDataSetChanged()
                            etNota.text.clear()
                        }
                    },
                    onError = { ex ->
                        Toast.makeText(this, "Error: ${ex.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // ¡CAMBIO! Botón e Intent
        val btnVerHistorial = findViewById<Button>(R.id.btnVerHistorialActividad)
        btnVerHistorial.setOnClickListener {
            val intent = Intent(this, HistoryActividadFisica::class.java).apply { // <- Nueva Activity de Historial
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // --- ¡NUEVA FUNCIÓN! Diálogo para pedir duración ---
    private fun showDialogDuracion(actividad: Actividad, onResult: (Int) -> Unit) {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.hint = "Minutos"

        AlertDialog.Builder(this)
            .setTitle("Duración de: ${actividad.nombre}")
            .setMessage("¿Cuántos minutos realizaste esta actividad?")
            .setView(input)
            .setPositiveButton("Aceptar") { d, _ ->
                val texto = input.text.toString().trim()
                val duracion = texto.toIntOrNull() ?: 0
                if (duracion > 0) {
                    onResult(duracion)
                } else {
                    Toast.makeText(this, "Ingresa un número válido de minutos", Toast.LENGTH_SHORT).show()
                }
                d.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // --- ¡NUEVA FUNCIÓN! Diálogo para "Otro..." ---
    private fun showDialogOtraActividad(onResult: (String, Int) -> Unit) {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(48, 24, 48, 24)

        val inputNombre = EditText(this)
        inputNombre.hint = "Nombre de la actividad"
        layout.addView(inputNombre)

        val inputDuracion = EditText(this)
        inputDuracion.inputType = InputType.TYPE_CLASS_NUMBER
        inputDuracion.hint = "Duración en minutos"
        layout.addView(inputDuracion)

        AlertDialog.Builder(this)
            .setTitle("Agregar actividad personalizada")
            .setView(layout)
            .setPositiveButton("Aceptar") { d, _ ->
                val nombre = inputNombre.text.toString().trim()
                val duracionStr = inputDuracion.text.toString().trim()
                val duracion = duracionStr.toIntOrNull() ?: 0

                if (nombre.isNotEmpty() && duracion > 0) {
                    onResult(nombre, duracion)
                    d.dismiss()
                } else {
                    Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


    // --- ¡CAMBIO! Nueva función de guardado ---
    fun guardarOActualizarActividad(
        fecha: String,
        hora: String,
        actividadesSeleccionadas: List<Actividad>,
        nota: String = "",
        onSuccess: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        // 1. Mapeo a la nueva clase ActividadRegistrada
        val actividadesMapeadas = actividadesSeleccionadas.map { act ->
            val nombreFinal = if (act.id == "otro" && !act.nota.isNullOrBlank()) {
                act.nota!! // El nombre personalizado
            } else {
                act.nombre
            }
            ActividadRegistrada(
                id = act.id,
                nombre = nombreFinal,
                duracionEnMinutos = act.duracionEnMinutos
            )
        }

        val registro = hashMapOf(
            "fecha" to fecha,
            "hora" to hora,
            "actividades" to actividadesMapeadas, // Guardamos la lista de objetos
            "nota" to nota
        )

        // ¡CAMBIO! Nueva colección
        val ref = db.collection("users").document(user.uid).collection("actividades_fisicas")

        if (documentIdEnEdicion != null) {
            // MODO EDICIÓN
            ref.document(documentIdEnEdicion!!)
                .set(registro as Map<String, Any>)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { ex -> onError(ex) }
        } else {
            // MODO CREACIÓN
            ref.add(registro)
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { ex -> onError(ex) }
        }
    }

    // --- TODO: Adaptar la lógica de precarga ---
    private fun precargarActividadParaEdicion(extras: Bundle) {
        // 1. Obtener ID del documento y actualizar el título/botón
        documentIdEnEdicion = extras.getString(HistoryActividadFisica.EXTRA_ACTIVIDAD_DOC_ID)
        findViewById<Toolbar>(R.id.toolbar)?.title = "Editar Actividad"
        btnGuardar.text = "Actualizar Registro"

        // 2. Precargar Fecha y Hora
        val fechaIso = extras.getString(HistoryActividadFisica.EXTRA_FECHA) ?: return
        val hora24h = extras.getString(HistoryActividadFisica.EXTRA_HORA) ?: return

        try {
            fechaSeleccionada = LocalDate.parse(fechaIso, DateTimeFormatter.ISO_DATE)
            horaSeleccionada = LocalTime.parse(hora24h, DateTimeFormatter.ofPattern("HH:mm"))
            btnSeleccionarFecha.text = fechaSeleccionada.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            btnSeleccionarHora.text = horaSeleccionada.format(DateTimeFormatter.ofPattern("hh:mm a", Locale("es", "ES")))
        } catch (e: Exception) {
            Toast.makeText(this, "Error cargando fecha/hora", Toast.LENGTH_LONG).show()
        }

        // 3. Precargar Nota General
        val notaGeneral = extras.getString(HistoryActividadFisica.EXTRA_NOTA)
        etNota.setText(notaGeneral)

        // 4. Precargar Actividades seleccionadas
        val actividadesGuardadas: ArrayList<ActividadRegistrada>?

        // Check Android version to use the correct method
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Newer, type-safe method for API 33+
            actividadesGuardadas = extras.getParcelableArrayList(
                HistoryActividadFisica.EXTRA_ACTIVIDADES_LIST,
                ActividadRegistrada::class.java // Specify the expected class!
            )
        } else {
            // Old method for APIs below 33
            @Suppress("DEPRECATION") // Suppress the warning for this specific line
            actividadesGuardadas = extras.getParcelableArrayList<ActividadRegistrada>(
                HistoryActividadFisica.EXTRA_ACTIVIDADES_LIST
            )
        }

        if (actividadesGuardadas == null) return

        // Mapeamos las actividades guardadas al estado de la UI
        actividadesBase.forEach { actUI ->
            val guardada = actividadesGuardadas.find { it.id == actUI.id }
            if (guardada != null) {
                // Si la encontramos, la marcamos como seleccionada
                actUI.seleccionado = true
                actUI.duracionEnMinutos = guardada.duracionEnMinutos

                // Si es "otro", también guardamos el nombre personalizado
                if (actUI.id == "otro") {
                    actUI.nota = guardada.nombre
                }
            } else {
                // Si no, nos aseguramos de que esté deseleccionada
                actUI.seleccionado = false
                actUI.duracionEnMinutos = 0
                actUI.nota = null
            }
        }
        actividadAdapter.notifyDataSetChanged()
    }


    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun calculateNoOfColumns(minItemWidthDp: Int): Int {
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        val noOfColumns = (screenWidthDp / minItemWidthDp).toInt()
        return noOfColumns.coerceAtLeast(2)
    }

    // --- ¡CAMBIO! Nuevo adaptador ---
    class ActividadAdapter(
        private val actividades: List<Actividad>,
        val onClick: (Actividad, Int) -> Unit
    ) : RecyclerView.Adapter<ActividadAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icono: ImageView = view.findViewById(R.id.imgSintoma)
            val nombre: TextView = view.findViewById(R.id.tvSintomaNombre)
            // ¡NUEVO! TextView para la duración
            val duracion: TextView = view.findViewById(R.id.tvSintomaDuracion) // <- NECESITARÁS AÑADIR ESTO AL XML

            init {
                view.setOnClickListener {
                    onClick(actividades[adapterPosition], adapterPosition)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            // ¡CAMBIO! Nuevo item layout
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_actividad, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val actividad = actividades[position]
            holder.icono.setImageResource(actividad.iconResId)

            // Lógica para mostrar nombre (para "Otro")
            val nombreMostrar = if (actividad.id == "otro" && actividad.seleccionado && !actividad.nota.isNullOrBlank()) {
                "Otro: ${actividad.nota!!.take(15)}..."
            } else {
                actividad.nombre
            }
            holder.nombre.text = nombreMostrar

            // ¡NUEVO! Mostrar duración si está seleccionada
            if (actividad.seleccionado && actividad.duracionEnMinutos > 0) {
                holder.duracion.visibility = View.VISIBLE
                holder.duracion.text = "${actividad.duracionEnMinutos} min"
            } else {
                holder.duracion.visibility = View.GONE
            }

            // Fondos (asume que tienes los mismos drawables)
            holder.itemView.setBackgroundResource(
                if (actividad.seleccionado) R.drawable.bg_sintoma_selected else R.drawable.bg_sintoma_unselected
            )
            holder.itemView.alpha = if (actividad.seleccionado) 1.0f else 0.7f
        }

        override fun getItemCount() = actividades.size
    }

}