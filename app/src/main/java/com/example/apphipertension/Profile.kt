package com.example.apphipertension

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.*
import androidx.core.content.edit
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.Calendar
import android.text.InputType
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import android.Manifest
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.appcompat.app.AlertDialog
import com.tuapp.utils.hideNavBar

class Profile : AppCompatActivity() {

    private lateinit var profileImage: ImageView
    private lateinit var etPeso: EditText
    private lateinit var etAltura: EditText
    private lateinit var etIMC: EditText
    private lateinit var etFechaNacimiento: EditText
    private lateinit var tvEdad: TextView
    private lateinit var rgSexo: RadioGroup
    private lateinit var rbMujer: RadioButton
    private lateinit var rbHombre: RadioButton
    private lateinit var etFechaDiagnostico: EditText
    private lateinit var tvCorreo: TextView
    private lateinit var btnLogout: Button
    private lateinit var btnSave: Button
    private lateinit var profileName: TextView
    private lateinit var changeName: TextView
    private lateinit var changePhoto: TextView

    private var selectedBirthDate: String = ""
    private var selectedDiagnosisDate: String = ""
    private var userUid: String = ""
    private lateinit var tvEditarMetaCalorias: TextView
    private var metaCaloriasActual: Double = 2000.0
    private val CALENDAR_PERMISSION_CODE = 103

    // Variables para Alimentos
    private lateinit var cgAlimentosPredefinidos: ChipGroup
    private lateinit var cgAlimentosPersonalizados: ChipGroup
    private lateinit var etAlimentoPersonalizado: EditText
    private lateinit var btnAgregarAlimentoPer: Button

    private val alimentosPredefinidos = mapOf(
        "harinas" to "Harinas Refinadas",
        "grasas" to "Grasas",
        "azucares" to "Azúcares Añadidos",
        "embutidos" to "Embutidos",
        "sal" to "Exceso de Sal",
        "alcohol" to "Alcohol"
    )

    // Variables para Medicamentos
    private lateinit var cgMedicamentosPersonalizados: ChipGroup
    private lateinit var etMedicamentoPersonalizado: EditText
    private lateinit var btnAgregarMedicamentoPer: Button

    // Variables para Padecimientos
    private lateinit var cgPadecimientosPredefinidos: ChipGroup
    private lateinit var cgPadecimientosPersonalizados: ChipGroup
    private lateinit var etPadecimientoPersonalizado: EditText
    private lateinit var btnAgregarPadecimientoPer: Button

    private val padecimientosPredefinidos = mapOf(
        "diabetes" to "Diabetes",
        "colesterol" to "Colesterol Alto",
        "trigliceridos" to "Triglicéridos Altos",
        "renal" to "Enfermedad Renal",
        "cardiaco" to "Problema Cardíaco"
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- 1. CONFIGURACIÓN DE PANTALLA COMPLETA (MODO INMERSIVO) ---
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        // Referencias UI
        profileImage = findViewById(R.id.profileImage)
        etPeso = findViewById(R.id.etPeso)
        etAltura = findViewById(R.id.etAltura)
        etIMC = findViewById(R.id.etIMC)
        etFechaNacimiento = findViewById(R.id.etFechaNacimiento)
        tvEdad = findViewById(R.id.tvEdad)
        rgSexo = findViewById(R.id.rgSexo)
        rbMujer = findViewById(R.id.rbMujer)
        rbHombre = findViewById(R.id.rbHombre)
        etFechaDiagnostico = findViewById(R.id.etFechaDiagnostico)
        tvCorreo = findViewById(R.id.tvCorreo)
        btnLogout = findViewById(R.id.btnLogout)
        profileName = findViewById(R.id.profileName)
        changeName = findViewById(R.id.changeName)
        changePhoto = findViewById(R.id.changePhoto)

        val btnCalcularIMC = findViewById<Button>(R.id.btnCalcularIMC)
        btnCalcularIMC.setOnClickListener { calculateIMC() }
        val btnSave = findViewById<Button>(R.id.btnSaveProfile)
        btnSave.setOnClickListener { saveProfileData() }

        changeName.setOnClickListener {
            val input = EditText(this)
            input.hint = "Nuevo nombre"
            val dialog = android.app.AlertDialog.Builder(this)
                .setTitle("Cambiar nombre")
                .setView(input)
                .setPositiveButton("Guardar") { _, _ ->
                    val newName = input.text.toString()
                    if (newName.isNotBlank()) {
                        profileName.text = newName
                        saveProfileData()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .create()
            dialog.show()
        }

        changePhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 101)
        }

        tvEditarMetaCalorias = findViewById(R.id.tvEditarMetaCalorias)
        cgAlimentosPredefinidos = findViewById(R.id.cgAlimentosPredefinidos)
        cgAlimentosPersonalizados = findViewById(R.id.cgAlimentosPersonalizados)
        etAlimentoPersonalizado = findViewById(R.id.etAlimentoPersonalizado)
        btnAgregarAlimentoPer = findViewById(R.id.btnAgregarAlimentoPer)

        tvEditarMetaCalorias.setOnClickListener {
            mostrarDialogoMetaCalorias(userUid, metaCaloriasActual)
        }

        setupAlimentosPredefinidos()

        btnAgregarAlimentoPer.setOnClickListener {
            val nombreAlimento = etAlimentoPersonalizado.text.toString().trim()
            if (nombreAlimento.isNotEmpty()) {
                addGenericCustomChip(nombreAlimento, cgAlimentosPersonalizados)
                etAlimentoPersonalizado.text.clear()
            }
        }

        cgMedicamentosPersonalizados = findViewById(R.id.cgMedicamentosPersonalizados)
        etMedicamentoPersonalizado = findViewById(R.id.etMedicamentoPersonalizado)
        btnAgregarMedicamentoPer = findViewById(R.id.btnAgregarMedicamentoPer)

        btnAgregarMedicamentoPer.setOnClickListener {
            val nombreMedicamento = etMedicamentoPersonalizado.text.toString().trim()
            if (nombreMedicamento.isNotEmpty()) {
                addGenericCustomChip(nombreMedicamento, cgMedicamentosPersonalizados)
                etMedicamentoPersonalizado.text.clear()
            }
        }

        cgPadecimientosPredefinidos = findViewById(R.id.cgPadecimientosPredefinidos)
        cgPadecimientosPersonalizados = findViewById(R.id.cgPadecimientosPersonalizados)
        etPadecimientoPersonalizado = findViewById(R.id.etPadecimientoPersonalizado)
        btnAgregarPadecimientoPer = findViewById(R.id.btnAgregarPadecimientoPer)

        setupPadecimientosPredefinidos()

        btnAgregarPadecimientoPer.setOnClickListener {
            val nombrePadecimiento = etPadecimientoPersonalizado.text.toString().trim()
            if (nombrePadecimiento.isNotEmpty()) {
                addGenericCustomChip(nombrePadecimiento, cgPadecimientosPersonalizados)
                etPadecimientoPersonalizado.text.clear()
            }
        }

        etFechaNacimiento.setOnClickListener { showDatePicker(true) }
        etFechaDiagnostico.setOnClickListener { showDatePicker(false) }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, Login::class.java))
            finish()
        }

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            userUid = user.uid
            profileName.text = user.displayName ?: user.email ?: ""
            tvCorreo.text = user.email ?: ""
            loadProfileData()
            loadProfileImage()
        }

        // --- 2. CONFIGURACIÓN NAVBAR ---
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
        bottomNav.selectedItemId = R.id.nav_profile
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        hideNavBar(bottomNav)
        bottomNav.selectedItemId = R.id.nav_profile

        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == bottomNav.selectedItemId && item.itemId != R.id.nav_more) {
                return@setOnItemSelectedListener true
            }

            val intent = when (item.itemId) {
                R.id.nav_home -> Intent(this, MainActivity::class.java)
                R.id.nav_meds -> Intent(this, Medicate::class.java)
                R.id.nav_calendar -> Intent(this, com.example.apphipertension.Calendar::class.java)
                R.id.nav_profile -> null // Ya estamos aquí
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
            } ?: (item.itemId == R.id.nav_profile)
        }
    }

    // Mantenemos toda tu lógica original de apoyo
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

    private fun showDatePicker(isBirth: Boolean) {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        val picker = DatePickerDialog(this, { _, y, m, d ->
            val dateStr = "%02d/%02d/%04d".format(d, m+1, y)
            if (isBirth) {
                etFechaNacimiento.setText(dateStr)
                calculateAge()
            } else {
                etFechaDiagnostico.setText(dateStr)
                selectedDiagnosisDate = dateStr
                askToCreateReminder(y, m, d)
            }
        }, year, month, day)
        picker.show()
    }

    private fun calculateIMC() {
        val peso = etPeso.text.toString().toFloatOrNull()
        val altura = etAltura.text.toString().toFloatOrNull()
        if (peso != null && altura != null && altura > 0) {
            val imc = peso / (altura * altura)
            etIMC.setText(String.format("%.2f", imc))
        } else {
            Toast.makeText(this, "Introduce peso y altura válidos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateAge() {
        val fecha = etFechaNacimiento.text.toString()
        try {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val birthDate = LocalDate.parse(fecha, formatter)
            val hoy = LocalDate.now()
            val edad = Period.between(birthDate, hoy).years
            tvEdad.text = "Edad: $edad"
        } catch (e: Exception) {
            tvEdad.text = "Edad: "
        }
    }

    private fun saveProfileData() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid
        val db = FirebaseFirestore.getInstance()

        val sexo = when (rgSexo.checkedRadioButtonId) {
            R.id.rbMujer -> "Mujer"
            R.id.rbHombre -> "Hombre"
            else -> ""
        }

        val alimentosPre = mutableListOf<String>()
        for (i in 0 until cgAlimentosPredefinidos.childCount) {
            val chip = cgAlimentosPredefinidos.getChildAt(i) as Chip
            if (chip.isChecked) alimentosPre.add(chip.tag.toString())
        }

        val alimentosPer = mutableListOf<String>()
        for (i in 0 until cgAlimentosPersonalizados.childCount) {
            val chip = cgAlimentosPersonalizados.getChildAt(i) as Chip
            alimentosPer.add(chip.text.toString())
        }

        val medicamentosPer = mutableListOf<String>()
        for (i in 0 until cgMedicamentosPersonalizados.childCount) {
            val chip = cgMedicamentosPersonalizados.getChildAt(i) as Chip
            medicamentosPer.add(chip.text.toString())
        }

        val padecimientosPre = mutableListOf<String>()
        for (i in 0 until cgPadecimientosPredefinidos.childCount) {
            val chip = cgPadecimientosPredefinidos.getChildAt(i) as Chip
            if (chip.isChecked) padecimientosPre.add(chip.tag.toString())
        }

        val padecimientosPer = mutableListOf<String>()
        for (i in 0 until cgPadecimientosPersonalizados.childCount) {
            val chip = cgPadecimientosPersonalizados.getChildAt(i) as Chip
            padecimientosPer.add(chip.text.toString())
        }

        val data = hashMapOf(
            "nombre" to profileName.text.toString(),
            "peso" to etPeso.text.toString(),
            "altura" to etAltura.text.toString(),
            "imc" to etIMC.text.toString(),
            "fecha_nacimiento" to etFechaNacimiento.text.toString(),
            "edad" to tvEdad.text.toString().removePrefix("Edad: ").trim(),
            "sexo" to sexo,
            "correo" to tvCorreo.text.toString(),
            "proxima_cita_medica" to etFechaDiagnostico.text.toString(),
            "alimentosEvitar_pre" to alimentosPre,
            "alimentosEvitar_per" to alimentosPer,
            "medicamentosEvitar" to medicamentosPer,
            "padecimientos_pre" to padecimientosPre,
            "padecimientos_per" to padecimientosPer
        )
        db.collection("users").document(uid).set(data, SetOptions.merge())
            .addOnSuccessListener { Toast.makeText(this, "Datos guardados", Toast.LENGTH_SHORT).show() }
    }

    private fun loadProfileData() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    profileName.text = doc.getString("nombre") ?: (user.email ?: "")
                    etPeso.setText(doc.getString("peso") ?: "")
                    etAltura.setText(doc.getString("altura") ?: "")
                    etIMC.setText(doc.getString("imc") ?: "")
                    etFechaNacimiento.setText(doc.getString("fecha_nacimiento") ?: "")
                    tvEdad.text = "Edad: " + (doc.getString("edad") ?: "")
                    when (doc.getString("sexo")) {
                        "Mujer" -> rgSexo.check(R.id.rbMujer)
                        "Hombre" -> rgSexo.check(R.id.rbHombre)
                    }
                    etFechaDiagnostico.setText(doc.getString("proxima_cita_medica") ?: "")
                    metaCaloriasActual = doc.getDouble("meta_calorias_diarias") ?: 2000.0
                    tvEditarMetaCalorias.text = "${"%.0f".format(metaCaloriasActual)} cal"
                    tvCorreo.text = doc.getString("correo") ?: (user.email ?: "")

                    cgAlimentosPersonalizados.removeAllViews()
                    val alimentosPre = doc.get("alimentosEvitar_pre") as? List<String> ?: emptyList()
                    for (i in 0 until cgAlimentosPredefinidos.childCount) {
                        val chip = cgAlimentosPredefinidos.getChildAt(i) as Chip
                        if (alimentosPre.contains(chip.tag.toString())) chip.isChecked = true
                    }

                    val alimentosPer = doc.get("alimentosEvitar_per") as? List<String> ?: emptyList()
                    alimentosPer.forEach { addGenericCustomChip(it, cgAlimentosPersonalizados) }

                    cgMedicamentosPersonalizados.removeAllViews()
                    val medicamentosPer = doc.get("medicamentosEvitar") as? List<String> ?: emptyList()
                    medicamentosPer.forEach { addGenericCustomChip(it, cgMedicamentosPersonalizados) }

                    cgPadecimientosPersonalizados.removeAllViews()
                    val padecimientosPre = doc.get("padecimientos_pre") as? List<String> ?: emptyList()
                    for (i in 0 until cgPadecimientosPredefinidos.childCount) {
                        val chip = cgPadecimientosPredefinidos.getChildAt(i) as Chip
                        chip.isChecked = padecimientosPre.contains(chip.tag.toString())
                    }
                    val padecimientosPer = doc.get("padecimientos_per") as? List<String> ?: emptyList()
                    padecimientosPer.forEach { addGenericCustomChip(it, cgPadecimientosPersonalizados) }
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == RESULT_OK) {
            val imageUri = data?.data
            if (imageUri != null && userUid.isNotBlank()) {
                profileImage.setImageURI(imageUri)
                val inputStream = contentResolver.openInputStream(imageUri)
                val bmp = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                val path = saveImageToInternalStorage(bmp, userUid)
                getSharedPreferences("user_profile", MODE_PRIVATE).edit().putString("profile_image_path", path).apply()
            }
        }
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap, filename: String): String {
        val file = File(filesDir, "$filename-profile.png")
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()
        return file.absolutePath
    }

    private fun loadProfileImage() {
        if (userUid.isBlank()) return
        val path = getSharedPreferences("user_profile", MODE_PRIVATE).getString("profile_image_path", null)
        if (path != null && File(path).exists()) {
            profileImage.setImageBitmap(BitmapFactory.decodeFile(path))
        } else {
            profileImage.setImageResource(R.drawable.profile)
        }
    }

    private fun setupAlimentosPredefinidos() {
        cgAlimentosPredefinidos.removeAllViews()
        for ((id, nombre) in alimentosPredefinidos) {
            val chip = Chip(this)
            chip.text = nombre
            chip.tag = id
            chip.isCheckable = true
            cgAlimentosPredefinidos.addView(chip)
        }
    }

    private fun addGenericCustomChip(nombre: String, chipGroup: ChipGroup) {
        val chip = Chip(this)
        chip.text = nombre
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener { chipGroup.removeView(it) }
        chipGroup.addView(chip)
    }

    private fun setupPadecimientosPredefinidos() {
        cgPadecimientosPredefinidos.removeAllViews()
        for ((id, nombre) in padecimientosPredefinidos) {
            val chip = Chip(this)
            chip.text = nombre
            chip.tag = id
            chip.isCheckable = true
            cgPadecimientosPredefinidos.addView(chip)
        }
    }

    private fun mostrarDialogoMetaCalorias(uid: String, metaActual: Double) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Meta de Calorías Diarias")
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        if (metaActual > 0) input.setText(metaActual.toInt().toString())
        builder.setView(input)
        builder.setPositiveButton("Guardar") { dialog, _ ->
            val nuevaMeta = input.text.toString().toDoubleOrNull()
            if (nuevaMeta != null) {
                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .update("meta_calorias_diarias", nuevaMeta).addOnSuccessListener {
                        metaCaloriasActual = nuevaMeta
                        tvEditarMetaCalorias.text = "${"%.0f".format(nuevaMeta)} cal"
                    }
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar", null).show()
    }

    private fun askToCreateReminder(year: Int, month: Int, day: Int) {
        AlertDialog.Builder(this)
            .setTitle("Crear Recordatorio")
            .setMessage("¿Deseas añadir un recordatorio para tu próxima cita médica en el calendario?")
            .setPositiveButton("Sí, añadir") { _, _ -> checkAndRequestCalendarPermission(year, month, day) }
            .setNegativeButton("No, gracias", null).show()
    }

    private fun checkAndRequestCalendarPermission(year: Int, month: Int, day: Int) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            createCalendarEvent(year, month, day)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_CALENDAR), CALENDAR_PERMISSION_CODE)
        }
    }

    private fun createCalendarEvent(year: Int, month: Int, day: Int) {
        try {
            val cal = Calendar.getInstance()
            cal.set(year, month, day, 10, 0)
            val startTime = cal.timeInMillis
            val intent = Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
                .putExtra(CalendarContract.Events.TITLE, "Cita Médica (AppHipertensión)")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}