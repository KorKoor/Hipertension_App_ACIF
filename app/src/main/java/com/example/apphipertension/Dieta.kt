package com.example.apphipertension

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.apphipertension.databinding.ActivityDietaBinding
import androidx.appcompat.app.AppCompatActivity
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.widget.Toast
import java.util.Calendar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import android.widget.ImageButton
import androidx.core.view.WindowInsetsControllerCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject

class Dieta : AppCompatActivity() {

    // 1. Declarar la variable de binding
    private lateinit var binding: ActivityDietaBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var userId: String? = null
    private var metaCaloriasActual: Double = 2000.0

    private var fechaSeleccionadaFormatoBtn: String = "" // "dd/MM/yyyy"
    private var fechaSeleccionadaFormatoDoc: String = "" // "yyyy-MM-dd"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- INICIO BLOQUE PANTALLA COMPLETA Y OCULTAR BOTONES ---
        enableEdgeToEdge()
        binding = ActivityDietaBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // 1. Obtener referencia a la Toolbar usando el binding
        val toolbar = binding.toolbar
        // 2. Establecerla como la ActionBar de la actividad
        setSupportActionBar(toolbar)
        // 3. Mostrar el botón de regreso (la flecha)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // 4. Definir qué hace la flecha (cerrar la actividad)
        toolbar.setNavigationOnClickListener { finish() }

        auth = FirebaseAuth.getInstance()
        db = Firebase.firestore

        // 3. Personalizar las tarjetas (includes)
        configurarTarjetas()

        // 4. Configurar el DatePickerDialog
        configurarSelectorFecha()

        // 5. Configurar el OnClickListener del FAB
        configurarFab()

        // Cargar la meta de calorías del usuario
        cargarMetaCalorias()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        // Si ya tenemos una fecha seleccionada, recargamos los datos
        if (fechaSeleccionadaFormatoDoc.isNotEmpty()) {
            cargarDatosDieta(fechaSeleccionadaFormatoDoc)
        }
    }

    private fun configurarTarjetas() {
        // --- Desayuno ---
        binding.cardDesayuno.tvTituloComida.text = "Desayuno"
        binding.cardDesayuno.ivIconoComida.setImageResource(R.drawable.desayuno)
        binding.cardDesayuno.btnLimpiarComida.setOnClickListener { confirmarYLiberarComida("desayuno") }

        // --- Comida ---
        binding.cardComida.tvTituloComida.text = "Comida"
        binding.cardComida.ivIconoComida.setImageResource(R.drawable.comida)
        binding.cardComida.btnLimpiarComida.setOnClickListener { confirmarYLiberarComida("comida") }

        // --- Cena ---
        binding.cardCena.tvTituloComida.text = "Cena"
        binding.cardCena.ivIconoComida.setImageResource(R.drawable.cena)
        binding.cardCena.btnLimpiarComida.setOnClickListener { confirmarYLiberarComida("cena") }

        // --- Colación ---
        binding.cardColacion.tvTituloComida.text = "Colación"
        binding.cardColacion.ivIconoComida.setImageResource(R.drawable.colacion)
        binding.cardColacion.btnLimpiarComida.setOnClickListener { confirmarYLiberarComida("colacion") }

        limpiarUIComidas()
    }

    private fun limpiarUIComidas() {
        val textoDefault = "Añade alimentos"
        binding.cardDesayuno.tvDescripcionComida.text = textoDefault
        binding.cardDesayuno.tvCaloriasComida.text = "0 cal"

        binding.cardComida.tvDescripcionComida.text = textoDefault
        binding.cardComida.tvCaloriasComida.text = "0 cal"

        binding.cardCena.tvDescripcionComida.text = textoDefault
        binding.cardCena.tvCaloriasComida.text = "0 cal"

        binding.cardColacion.tvDescripcionComida.text = textoDefault
        binding.cardColacion.tvCaloriasComida.text = "0 cal"

        binding.tvCaloriasTotales.text = "Total calorías: 0 cal"
    }

    private fun configurarSelectorFecha() {
        val calendario = Calendar.getInstance()
        val anioActual = calendario.get(Calendar.YEAR)
        val mesActual = calendario.get(Calendar.MONTH)
        val diaActual = calendario.get(Calendar.DAY_OF_MONTH)

        actualizarFechaSeleccionada(diaActual, mesActual + 1, anioActual)

        binding.btnSeleccionarFechaDieta.setOnClickListener {
            val datePickerDialog = DatePickerDialog(this,
                { _, anioSeleccionado, mesSeleccionado, diaSeleccionado ->
                    actualizarFechaSeleccionada(diaSeleccionado, mesSeleccionado + 1, anioSeleccionado)
                },
                anioActual, mesActual, diaActual
            )
            datePickerDialog.show()
        }
    }

    private fun actualizarFechaSeleccionada(dia: Int, mes: Int, anio: Int) {
        fechaSeleccionadaFormatoBtn = "$dia/$mes/$anio"
        binding.btnSeleccionarFechaDieta.text = fechaSeleccionadaFormatoBtn

        val cal = Calendar.getInstance()
        cal.set(anio, mes - 1, dia)
        val formatoDoc = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        fechaSeleccionadaFormatoDoc = formatoDoc.format(cal.time)

        cargarDatosDieta(fechaSeleccionadaFormatoDoc)
    }

    private fun configurarFab() {
        binding.fabAgregarComida.setOnClickListener {
            val intent = Intent(this, AnadirAlimento::class.java)
            intent.putExtra("FECHA_SELECCIONADA", fechaSeleccionadaFormatoBtn)
            startActivity(intent)
        }
    }

    private fun cargarDatosDieta(fechaDocumentoId: String) {
        val uid = auth.currentUser?.uid ?: return
        limpiarUIComidas()

        val docRef = db.collection("users").document(uid)
            .collection("registros_dieta").document(fechaDocumentoId)

        docRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val registro = documentSnapshot.toObject<RegistroDieta>()
                    if (registro != null) {
                        actualizarUI(registro)
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar datos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun actualizarUI(registro: RegistroDieta) {
        val totalCalorias = registro.calorias_totales_dia
        val meta = metaCaloriasActual

        binding.tvCaloriasTotales.text = "Total calorías: ${"%.0f".format(totalCalorias)} cal"

        if (totalCalorias > meta && meta > 0) {
            binding.tvCaloriasTotales.setTextColor(Color.RED)
        } else {
            binding.tvCaloriasTotales.setTextColor(Color.BLACK)
        }

        binding.cardDesayuno.tvDescripcionComida.text = formatarListaAlimentos(registro.desayuno)
        binding.cardDesayuno.tvCaloriasComida.text = "${"%.0f".format(calcularCaloriasLista(registro.desayuno))} cal"

        binding.cardComida.tvDescripcionComida.text = formatarListaAlimentos(registro.comida)
        binding.cardComida.tvCaloriasComida.text = "${"%.0f".format(calcularCaloriasLista(registro.comida))} cal"

        binding.cardCena.tvDescripcionComida.text = formatarListaAlimentos(registro.cena)
        binding.cardCena.tvCaloriasComida.text = "${"%.0f".format(calcularCaloriasLista(registro.cena))} cal"

        binding.cardColacion.tvDescripcionComida.text = formatarListaAlimentos(registro.colacion)
        binding.cardColacion.tvCaloriasComida.text = "${"%.0f".format(calcularCaloriasLista(registro.colacion))} cal"
    }

    private fun formatarListaAlimentos(lista: List<AlimentoRegistrado>): String {
        if (lista.isEmpty()) return "Añade alimentos"
        return lista.joinToString(separator = ", ") { "${it.cantidad} ${it.nombre}" }
    }

    private fun calcularCaloriasLista(lista: List<AlimentoRegistrado>): Double {
        return lista.sumOf { it.calorias }
    }

    private fun cargarMetaCalorias() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val meta = documentSnapshot.getDouble("meta_calorias_diarias")
                metaCaloriasActual = if (meta != null && meta > 0) meta else 2000.0

                if (meta != null && meta > 0) {
                    binding.tvMetaCalorias.text = "Meta: ${"%.0f".format(meta)} cal (Editar)"
                } else {
                    binding.tvMetaCalorias.text = "Fija tu meta (clic aquí)"
                }
                binding.tvMetaCalorias.setOnClickListener {
                    mostrarDialogoMetaCalorias(uid, metaCaloriasActual)
                }
            }
            .addOnFailureListener {
                metaCaloriasActual = 2000.0
                binding.tvMetaCalorias.text = "Error al cargar meta (Editar)"
            }
    }

    private fun mostrarDialogoMetaCalorias(uid: String, metaActual: Double) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Meta de Calorías Diarias")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.hint = "Ej. 2000"
        if (metaActual > 0) {
            input.setText(metaActual.toInt().toString())
        }
        builder.setView(input)

        builder.setPositiveButton("Guardar") { dialog, _ ->
            val metaStr = input.text.toString()
            if (metaStr.isNotEmpty()) {
                try {
                    val nuevaMeta = metaStr.toDouble()
                    db.collection("users").document(uid)
                        .update("meta_calorias_diarias", nuevaMeta)
                        .addOnSuccessListener {
                            binding.tvMetaCalorias.text = "Meta: ${"%.0f".format(nuevaMeta)} cal"
                            metaCaloriasActual = nuevaMeta
                            Toast.makeText(this, "Meta actualizada", Toast.LENGTH_SHORT).show()
                            cargarDatosDieta(fechaSeleccionadaFormatoDoc)
                        }
                    dialog.dismiss()
                } catch (e: Exception) { }
            }
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun confirmarYLiberarComida(categoria: String) {
        val uid = auth.currentUser?.uid ?: return

        AlertDialog.Builder(this)
            .setTitle("Confirmar Limpieza")
            .setMessage("¿Estás seguro de que quieres eliminar todos los registros para '$categoria'?")
            .setPositiveButton("Limpiar") { dialog, _ ->
                val docRef = db.collection("users").document(uid)
                    .collection("registros_dieta").document(fechaSeleccionadaFormatoDoc)

                docRef.get().addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        val registroActual = documentSnapshot.toObject<RegistroDieta>()
                        if (registroActual != null) {
                            val caloriasARestar = when (categoria) {
                                "desayuno" -> calcularCaloriasLista(registroActual.desayuno)
                                "comida" -> calcularCaloriasLista(registroActual.comida)
                                "cena" -> calcularCaloriasLista(registroActual.cena)
                                "colacion" -> calcularCaloriasLista(registroActual.colacion)
                                else -> 0.0
                            }

                            docRef.update(mapOf(
                                categoria to emptyList<AlimentoRegistrado>(),
                                "calorias_totales_dia" to FieldValue.increment(-caloriasARestar)
                            )).addOnSuccessListener {
                                Toast.makeText(this, "$categoria limpiado", Toast.LENGTH_SHORT).show()
                                cargarDatosDieta(fechaSeleccionadaFormatoDoc)
                            }
                        }
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}