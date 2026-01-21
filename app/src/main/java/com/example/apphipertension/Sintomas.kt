package com.example.apphipertension

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.time.LocalTime
import android.widget.Toast
import androidx.core.view.WindowInsetsControllerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class Sintomas : AppCompatActivity() {
    private lateinit var sintomasAdapter: SintomasAdapter
    private lateinit var btnGuardar: Button
    private lateinit var etNota: EditText
    private lateinit var btnSeleccionarFecha: Button
    private lateinit var btnSeleccionarHora: Button
    private lateinit var rvSintomas: RecyclerView
    private lateinit var tvError: TextView
    private var fechaSeleccionada: LocalDate = LocalDate.now()
    private var horaSeleccionada: LocalTime = LocalTime.now()
    private var documentIdEnEdicion: String? = null

    val sintomasBase = mutableListOf(
        Sintoma("bien", "Estoy bien", R.drawable.ic_bien),
        Sintoma("mareos", "Mareos", R.drawable.ic_mareos),
        Sintoma("dolor_cabeza", "Dolor de cabeza", R.drawable.ic_dolor_cabeza),
        Sintoma("vision_borrosa", "Visión borrosa", R.drawable.ic_vision_borrosa),
        Sintoma("zumbido", "Zumbido en oídos", R.drawable.ic_zumbido),
        Sintoma("dolor_pecho", "Dolor en el pecho", R.drawable.ic_dolor_pecho),
        Sintoma("fatiga", "Fatiga", R.drawable.ic_fatiga),
        Sintoma("sangrado", "Sangrado nasal", R.drawable.ic_sangrado),
        Sintoma("dificultad_respirar", "Dificultad para respirar", R.drawable.ic_dificultad_respirar),
        Sintoma("nauseas", "Náuseas", R.drawable.ic_nauseas),
        Sintoma("vomitos", "Vómitos", R.drawable.ic_vomitos),
        Sintoma("otro", "Otro...", R.drawable.ic_otro)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- 1. CONFIGURACIÓN DE PANTALLA COMPLETA Y OCULTAR BOTONES ---
        enableEdgeToEdge()
        setContentView(R.layout.activity_sintomas)

        // Configura Toolbar con flecha de regreso
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        rvSintomas = findViewById(R.id.rvSintomas)
        btnSeleccionarFecha = findViewById(R.id.btnSeleccionarFecha)
        btnSeleccionarHora = findViewById(R.id.btnSeleccionarHora)
        btnGuardar = findViewById(R.id.btnGuardarSintomas)
        etNota = findViewById(R.id.etNotaSintoma)
        tvError = findViewById(R.id.tvError)

        btnSeleccionarFecha.text = fechaSeleccionada.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        btnSeleccionarHora.text = horaSeleccionada.format(DateTimeFormatter.ofPattern("hh:mm a"))

        sintomasAdapter = SintomasAdapter(sintomasBase) { sintoma, position ->
            if (sintoma.id == "otro") {
                if (sintoma.seleccionado) {
                    sintomasBase[position].seleccionado = false
                    sintomasBase[position].nota = null
                    sintomasAdapter.notifyItemChanged(position)
                } else {
                    showDialogOtroSintoma { texto ->
                        sintomasBase[position].seleccionado = true
                        sintomasBase[position].nota = texto
                        sintomasAdapter.notifyItemChanged(position)
                    }
                }
            } else {
                sintoma.seleccionado = !sintoma.seleccionado
                sintomasBase[position].nota = null
                sintomasAdapter.notifyItemChanged(position)
            }
            tvError.visibility = View.GONE
        }

        val spanCount = calculateNoOfColumns(120)
        rvSintomas.layoutManager = GridLayoutManager(this, spanCount)
        rvSintomas.adapter = sintomasAdapter

        val extras = intent.extras
        if (extras != null && extras.containsKey(HistorySintomasActivity.EXTRA_DOCUMENT_ID)) {
            precargarSintomasParaEdicion(extras)
        }

        btnSeleccionarFecha.setOnClickListener {
            val hoy = fechaSeleccionada
            val picker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val fecha = LocalDate.of(year, month + 1, dayOfMonth)
                    fechaSeleccionada = fecha
                    btnSeleccionarFecha.text = fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                },
                hoy.year, hoy.monthValue - 1, hoy.dayOfMonth
            )
            picker.show()
        }

        btnSeleccionarHora.setOnClickListener {
            val ahora = horaSeleccionada
            val picker = android.app.TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    val hora = LocalTime.of(hourOfDay, minute)
                    horaSeleccionada = hora
                    btnSeleccionarHora.text = hora.format(DateTimeFormatter.ofPattern("hh:mm a"))
                },
                ahora.hour, ahora.minute, false
            )
            picker.show()
        }

        btnGuardar.setOnClickListener {
            val fecha = fechaSeleccionada
            val hora = horaSeleccionada
            val nota = etNota.text.toString()
            val sintomasSeleccionados = sintomasBase.filter { it.seleccionado }

            if (sintomasSeleccionados.isEmpty()) {
                tvError.visibility = View.VISIBLE
            } else {
                tvError.visibility = View.GONE
                guardarOActualizarSintomas(
                    fecha.format(DateTimeFormatter.ISO_DATE),
                    hora.format(DateTimeFormatter.ofPattern("HH:mm")),
                    sintomasSeleccionados,
                    nota,
                    onSuccess = {
                        val mensaje = if (documentIdEnEdicion != null) "Síntomas actualizados" else "Síntomas guardados"
                        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
                        if (documentIdEnEdicion != null) {
                            finish()
                        } else {
                            sintomasBase.forEach { it.seleccionado = false; it.nota = null }
                            sintomasAdapter.notifyDataSetChanged()
                            etNota.text.clear()
                        }
                    },
                    onError = { ex ->
                        Toast.makeText(this, "Error: ${ex.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        findViewById<Button>(R.id.btnVerHistorial).setOnClickListener {
            val intent = Intent(this, HistorySintomasActivity::class.java).apply {
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

    private fun precargarSintomasParaEdicion(extras: Bundle) {
        documentIdEnEdicion = extras.getString(HistorySintomasActivity.EXTRA_DOCUMENT_ID)
        findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)?.title = "Editar Síntomas"
        btnGuardar.text = "Actualizar Registro"

        val fechaIso = extras.getString(HistorySintomasActivity.EXTRA_FECHA) ?: return
        val hora24h = extras.getString(HistorySintomasActivity.EXTRA_HORA) ?: return

        try {
            fechaSeleccionada = LocalDate.parse(fechaIso, DateTimeFormatter.ISO_DATE)
            horaSeleccionada = LocalTime.parse(hora24h, DateTimeFormatter.ofPattern("HH:mm"))
            btnSeleccionarFecha.text = fechaSeleccionada.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            btnSeleccionarHora.text = horaSeleccionada.format(DateTimeFormatter.ofPattern("hh:mm a", Locale("es", "ES")))
        } catch (e: Exception) {
            Toast.makeText(this, "Error cargando fecha/hora: ${e.message}", Toast.LENGTH_LONG).show()
        }

        etNota.setText(extras.getString(HistorySintomasActivity.EXTRA_NOTA))

        val idsSeleccionados = extras.getStringArrayList(HistorySintomasActivity.EXTRA_SINTOMA_IDS) ?: return
        val notaOtroSintoma = extras.getString("otro_sintoma_nota")

        sintomasBase.forEach { sintoma ->
            if (idsSeleccionados.contains(sintoma.id)) {
                sintoma.seleccionado = true
                if (sintoma.id == "otro") sintoma.nota = notaOtroSintoma
            } else {
                sintoma.seleccionado = false
                sintoma.nota = null
            }
        }
        sintomasAdapter.notifyDataSetChanged()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun calculateNoOfColumns(minItemWidthDp: Int): Int {
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        return (screenWidthDp / minItemWidthDp).toInt().coerceAtLeast(2)
    }

    fun getIconResIdForSintoma(id: String): Int {
        return when (id) {
            "bien" -> R.drawable.ic_bien
            "mareos" -> R.drawable.ic_mareos
            "dolor_cabeza" -> R.drawable.ic_dolor_cabeza
            "vision_borrosa" -> R.drawable.ic_vision_borrosa
            "zumbido" -> R.drawable.ic_zumbido
            "dolor_pecho" -> R.drawable.ic_dolor_pecho
            "fatiga" -> R.drawable.ic_fatiga
            "sangrado" -> R.drawable.ic_sangrado
            "dificultad_respirar" -> R.drawable.ic_dificultad_respirar
            "nauseas" -> R.drawable.ic_nauseas
            "vomitos" -> R.drawable.ic_vomitos
            else -> R.drawable.ic_otro
        }
    }

    fun guardarOActualizarSintomas(
        fecha: String, hora: String, sintomasSeleccionados: List<Sintoma>,
        nota: String = "", onSuccess: () -> Unit = {}, onError: (Exception) -> Unit = {}
    ) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()

        val sintomasMapeados = sintomasSeleccionados.map { sintoma ->
            val nombreFinal = if (sintoma.id == "otro" && !sintoma.nota.isNullOrBlank()) sintoma.nota!! else sintoma.nombre
            mapOf("id" to sintoma.id, "nombre" to nombreFinal)
        }

        val registro = hashMapOf("fecha" to fecha, "hora" to hora, "sintomas" to sintomasMapeados, "nota" to nota)
        val ref = db.collection("users").document(user.uid).collection("sintomas")

        if (documentIdEnEdicion != null) {
            ref.document(documentIdEnEdicion!!).set(registro as Map<String, Any>)
                .addOnSuccessListener { onSuccess() }.addOnFailureListener { onError(it) }
        } else {
            ref.add(registro).addOnSuccessListener { onSuccess() }.addOnFailureListener { onError(it) }
        }
    }

    fun showDialogOtroSintoma(onResult: (String) -> Unit) {
        val input = EditText(this)
        input.hint = "Describe el síntoma"
        AlertDialog.Builder(this).setTitle("Agregar síntoma personalizado").setView(input)
            .setPositiveButton("Aceptar") { d, _ ->
                val texto = input.text.toString().trim()
                if (texto.isNotEmpty()) onResult(texto)
                d.dismiss()
            }.setNegativeButton("Cancelar", null).show()
    }

    class SintomasAdapter(private val sintomas: List<Sintoma>, val onClick: (Sintoma, Int) -> Unit) :
        RecyclerView.Adapter<SintomasAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icono: ImageView = view.findViewById(R.id.imgSintoma)
            val nombre: TextView = view.findViewById(R.id.tvSintomaNombre)
            init { view.setOnClickListener { onClick(sintomas[adapterPosition], adapterPosition) } }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_sintoma, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val sintoma = sintomas[position]
            holder.icono.setImageResource(sintoma.iconResId)
            val nombreMostrar = if (sintoma.id == "otro" && sintoma.seleccionado && !sintoma.nota.isNullOrBlank()) {
                "Otro: ${sintoma.nota!!.take(15)}..."
            } else sintoma.nombre
            holder.nombre.text = nombreMostrar
            holder.itemView.setBackgroundResource(if (sintoma.seleccionado) R.drawable.bg_sintoma_selected else R.drawable.bg_sintoma_unselected)
            holder.itemView.alpha = if (sintoma.seleccionado) 1.0f else 0.7f
        }

        override fun getItemCount() = sintomas.size
    }
}