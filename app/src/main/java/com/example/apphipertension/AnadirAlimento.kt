package com.example.apphipertension

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apphipertension.databinding.ActivityAnadirAlimentoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.app.AlertDialog
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.Locale
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import android.util.Log
import androidx.core.view.WindowInsetsControllerCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class AnadirAlimento : AppCompatActivity() {

    // View Binding
    private lateinit var binding: ActivityAnadirAlimentoBinding

    // Firebase
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // RecyclerView
    private lateinit var adapter: AlimentoAdapter

    // Lista para guardar TODOS los alimentos de la base de datos
    private val listaCompletaAlimentos = mutableListOf<Alimento>()

    // Variable para guardar la fecha que recibimos de DietaActivity
    private var fechaSeleccionada: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- INICIO BLOQUE PANTALLA COMPLETA Y OCULTAR BOTONES ---
        enableEdgeToEdge()
        binding = ActivityAnadirAlimentoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Inicializar Firebase
        db = Firebase.firestore
        auth = FirebaseAuth.getInstance()


        // Configurar Toolbar
        val toolbar = binding.toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        // 1. Obtener la fecha
        fechaSeleccionada = intent.getStringExtra("FECHA_SELECCIONADA")
        if (fechaSeleccionada == null) {
            Toast.makeText(this, "Error: No se seleccionó fecha", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // 2. Configurar RecyclerView
        setupRecyclerView()

        // 3. Configurar SearchView
        setupSearchView()

        // 4. Configurar FAB/Botón personalizado
        binding.btnComidaPersonalizada.setOnClickListener {
            mostrarDialogoComidaPersonalizada()
        }

        // 5. Cargar lista actual de Firestore
        cargarAlimentosDesdeFirestore()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupRecyclerView() {
        adapter = AlimentoAdapter(emptyList(), ::onAlimentoSeleccionado)
        binding.recyclerViewAlimentos.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewAlimentos.adapter = adapter
    }

    private fun cargarAlimentosDesdeFirestore() {
        db.collection("alimentos_mexico")
            .get()
            .addOnSuccessListener { result ->
                listaCompletaAlimentos.clear()
                for (document in result) {
                    val alimento = document.toObject(Alimento::class.java)
                    listaCompletaAlimentos.add(alimento)
                }
                adapter.updateList(listaCompletaAlimentos)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar alimentos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun subirJsonAFirestore() {
        try {
            val jsonString = assets.open("alimentos_data.json").bufferedReader().use { it.readText() }
            val gson = com.google.gson.Gson()
            val listType = object : com.google.gson.reflect.TypeToken<List<Alimento>>() {}.type
            val listaAlimentos: List<Alimento> = gson.fromJson(jsonString, listType)

            val batch = db.batch()
            listaAlimentos.forEach { alimento ->
                val docRef = db.collection("alimentos_mexico").document()
                batch.set(docRef, alimento)
            }

            batch.commit()
                .addOnSuccessListener {
                    Toast.makeText(this, "Base de datos actualizada", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Fallo en el servidor: ${e.message}", Toast.LENGTH_LONG).show()
                }

        } catch (e: Exception) {
            Toast.makeText(this, "Error local: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupSearchView() {
        binding.searchViewAlimentos.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filtrarLista(newText)
                return true
            }
        })
    }

    private fun filtrarLista(query: String?) {
        if (query.isNullOrEmpty()) {
            adapter.updateList(listaCompletaAlimentos)
        } else {
            val listaFiltrada = listaCompletaAlimentos.filter { alimento ->
                alimento.nombre.lowercase().contains(query.lowercase())
            }
            adapter.updateList(listaFiltrada)
        }
    }

    private fun onAlimentoSeleccionado(alimento: Alimento) {
        mostrarDialogoGuardarAlimento(alimento)
    }

    private fun mostrarDialogoGuardarAlimento(alimento: Alimento) {
        val builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_guardar_alimento, null)

        val tvTituloDialogo = dialogView.findViewById<TextView>(R.id.tvTituloDialogo)
        val tvInfoCaloriasBase = dialogView.findViewById<TextView>(R.id.tvInfoCaloriasBase)
        val etCantidad = dialogView.findViewById<EditText>(R.id.etCantidad)
        val spinnerCategoria = dialogView.findViewById<Spinner>(R.id.spinnerCategoria)
        val tvTotalCaloriasCalculadas = dialogView.findViewById<TextView>(R.id.tvTotalCaloriasCalculadas)

        tvTituloDialogo.text = "Añadir ${alimento.nombre}"
        tvInfoCaloriasBase.text = "Base: ${alimento.calorias_base} cal / ${alimento.unidad_base}"
        tvTotalCaloriasCalculadas.text = "Total: 0 cal"

        ArrayAdapter.createFromResource(
            this,
            R.array.categorias_comida,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategoria.adapter = adapter
        }

        etCantidad.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try {
                    val cantidad = s.toString().toDouble()
                    val totalCalculado = cantidad * alimento.calorias_base
                    tvTotalCaloriasCalculadas.text = "Total: ${"%.2f".format(totalCalculado)} cal"
                } catch (e: NumberFormatException) {
                    tvTotalCaloriasCalculadas.text = "Total: 0 cal"
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        builder.setView(dialogView)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

        val dialog = builder.create()
        dialog.show()

        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton?.setTextColor(Color.parseColor("#466B95"))

        positiveButton.setOnClickListener {
            val cantidadStr = etCantidad.text.toString()
            if (cantidadStr.isEmpty() || cantidadStr == ".") {
                etCantidad.error = "Ingresa una cantidad válida"
                return@setOnClickListener
            }

            val cantidad = cantidadStr.toDouble()
            if (cantidad <= 0) {
                etCantidad.error = "La cantidad debe ser mayor a 0"
                return@setOnClickListener
            }

            var categoriaSeleccionada = spinnerCategoria.selectedItem.toString().lowercase()
            if (categoriaSeleccionada == "colación") {
                categoriaSeleccionada = "colacion"
            }
            val caloriasTotalesItem = cantidad * alimento.calorias_base

            val alimentoRegistrado = AlimentoRegistrado(
                nombre = alimento.nombre,
                cantidad = cantidad,
                unidad = alimento.unidad_base,
                calorias = caloriasTotalesItem
            )

            guardarAlimentoEnFirestore(alimentoRegistrado, categoriaSeleccionada)
            dialog.dismiss()
        }
    }

    private fun formatearFechaParaFirestore(fecha: String): String {
        val formatoEntrada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val formatoSalida = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = formatoEntrada.parse(fecha)
        return formatoSalida.format(date)
    }

    private fun guardarAlimentoEnFirestore(alimento: AlimentoRegistrado, categoria: String) {
        val userId = auth.currentUser?.uid ?: return
        val fechaId = formatearFechaParaFirestore(fechaSeleccionada!!)

        val docRef = db.collection("users").document(userId)
            .collection("registros_dieta").document(fechaId)

        docRef.set(
            mapOf(
                categoria to FieldValue.arrayUnion(alimento),
                "calorias_totales_dia" to FieldValue.increment(alimento.calorias),
                "fecha" to com.google.firebase.Timestamp.now()
            ),
            com.google.firebase.firestore.SetOptions.merge()
        )
            .addOnSuccessListener {
                Toast.makeText(this, "${alimento.nombre} guardado", Toast.LENGTH_SHORT).show()
                checkAndNotifyCalorieGoal(userId, docRef)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkAndNotifyCalorieGoal(uid: String, dailyRecordRef: com.google.firebase.firestore.DocumentReference) {
        db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
            val goal = userDoc.getDouble("meta_calorias_diarias")
            if (goal == null || goal <= 0) return@addOnSuccessListener

            dailyRecordRef.get().addOnSuccessListener { dailyDoc ->
                val currentCalories = dailyDoc.getDouble("calorias_totales_dia") ?: 0.0

                if (currentCalories > goal) {
                    val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    val todayDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                    val lastNotifDate = prefs.getString("last_calorie_notif_date", "")

                    if (todayDate != lastNotifDate) {
                        sendCalorieGoalNotification(this, currentCalories, goal)
                        prefs.edit().putString("last_calorie_notif_date", todayDate).apply()
                    }
                }
            }
        }
    }

    private fun sendCalorieGoalNotification(context: Context, currentCals: Double, goalCals: Double) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val builder = NotificationCompat.Builder(context, NotificationUtils.CALORIE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("¡Meta de Calorías Superada!")
            .setContentText("Llevas ${"%.0f".format(currentCals)} de ${"%.0f".format(goalCals)} cal. 👍")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(1, builder.build())
        }
    }

    private fun mostrarDialogoComidaPersonalizada() {
        val builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_comida_personalizada, null)

        val etNombre = dialogView.findViewById<EditText>(R.id.etNombrePersonalizado)
        val etCalorias = dialogView.findViewById<EditText>(R.id.etCaloriasPersonalizadas)
        val spinnerCategoria = dialogView.findViewById<Spinner>(R.id.spinnerCategoriaPersonalizada)

        ArrayAdapter.createFromResource(
            this,
            R.array.categorias_comida,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategoria.adapter = adapter
        }

        builder.setView(dialogView)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nombre = etNombre.text.toString()
            val caloriasStr = etCalorias.text.toString()

            if (nombre.isEmpty()) {
                etNombre.error = "Ingresa un nombre"
                return@setOnClickListener
            }

            if (caloriasStr.isEmpty() || caloriasStr == ".") {
                etCalorias.error = "Ingresa calorías válidas"
                return@setOnClickListener
            }

            val calorias = caloriasStr.toDouble()
            if (calorias <= 0) {
                etCalorias.error = "Las calorías deben ser mayores a 0"
                return@setOnClickListener
            }

            var categoria = spinnerCategoria.selectedItem.toString().lowercase()
            if (categoria == "colación") {
                categoria = "colacion"
            }

            val alimentoRegistrado = AlimentoRegistrado(
                nombre = nombre,
                cantidad = 1.0,
                unidad = "porción",
                calorias = calorias
            )

            guardarAlimentoEnFirestore(alimentoRegistrado, categoria)
            dialog.dismiss()
        }
    }
}