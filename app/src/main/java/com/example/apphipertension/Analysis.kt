package com.example.apphipertension

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.core.content.FileProvider
import android.content.Intent
import android.net.Uri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class Analysis : AppCompatActivity() {

    private lateinit var tvReporte: TextView
    private lateinit var btnExportarPDF: Button
    private lateinit var progressIndicator: CircularProgressIndicator
    private var currentReportData: AnalysisReport? = null
    private val STORAGE_PERMISSION_CODE = 102

    private val padecimientosMap = mapOf(
        "diabetes" to "Diabetes",
        "colesterol" to "Colesterol Alto",
        "trigliceridos" to "Triglicéridos Altos",
        "renal" to "Enfermedad Renal",
        "cardiaco" to "Problema Cardíaco"
    )
    private val alimentosMap = mapOf(
        "harinas" to "Harinas Refinadas",
        "grasas" to "Grasas",
        "azucares" to "Azúcares Añadidos",
        "embutidos" to "Embutidos",
        "sal" to "Exceso de Sal",
        "alcohol" to "Alcohol"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- 1. CONFIGURACIÓN DE PANTALLA COMPLETA Y OCULTAR BOTONES ---
        setContentView(R.layout.activity_analysis)


        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Análisis y Reporte"
        toolbar.setNavigationOnClickListener { finish() }

        tvReporte = findViewById(R.id.tvReporte)
        btnExportarPDF = findViewById(R.id.btnExportarPDF)
        progressIndicator = findViewById(R.id.progressIndicator)
        btnExportarPDF.isEnabled = false

        loadAndGenerateReport()

        btnExportarPDF.setOnClickListener {
            if (currentReportData != null) {
                exportarInformeMedicoAPDF(currentReportData!!)
            } else {
                Toast.makeText(this, "Aún no se han cargado los datos del reporte.", Toast.LENGTH_SHORT).show()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadAndGenerateReport() {
        progressIndicator.visibility = View.VISIBLE
        tvReporte.text = "Cargando datos..."
        btnExportarPDF.isEnabled = false

        lifecycleScope.launch {
            val reportData = DataFetcher.fetchAllReportData()
            currentReportData = reportData
            progressIndicator.visibility = View.GONE

            if (reportData != null) {
                val formattedReport = formatReportForDisplay(reportData)
                tvReporte.text = formattedReport
                btnExportarPDF.isEnabled = true
            } else {
                tvReporte.text = "Error al cargar los datos o usuario no autenticado."
                btnExportarPDF.isEnabled = false
            }
        }
    }

    private fun formatReportForDisplay(report: AnalysisReport): String {
        val sb = StringBuilder()
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

        sb.append("--- REPORTE DE ANÁLISIS CLÍNICO ---\n")
        sb.append("Fecha de generación: $today\n")
        sb.append("===================================\n\n")

        sb.append("I. INFORMACIÓN DEL PACIENTE Y ANTECEDENTES\n")
        sb.append("-------------------------------------------\n")
        sb.append("Nombre: ${report.profileData.nombre}\n")
        sb.append("Correo: ${report.profileData.correo}\n")
        sb.append("Edad/Sexo: ${report.profileData.edad} años / ${report.profileData.sexo}\n")
        sb.append("IMC: ${report.profileData.imc} (Peso: ${report.profileData.peso} kg / Altura: ${report.profileData.altura} m)\n")
        sb.append("Próxima Cita Médica: ${report.profileData.proxima_cita_medica}\n")

        val padecimientosPreNombres = report.profileData.padecimientos_pre.map { padecimientosMap[it] ?: it }
        val padecimientosPer = report.profileData.padecimientos_per
        val todosPadecimientos = (padecimientosPreNombres + padecimientosPer).filter { it.isNotBlank() }.joinToString(", ")
        sb.append("Padecimientos Registrados: ${if (todosPadecimientos.isBlank()) "Ninguno" else todosPadecimientos}\n")

        val alimentosPreNombres = report.profileData.alimentosEvitar_pre.map { alimentosMap[it] ?: it }
        val alimentosPer = report.profileData.alimentosEvitar_per
        val todosAlimentos = (alimentosPreNombres + alimentosPer).filter { it.isNotBlank() }.joinToString(", ")
        sb.append("Alimentos a Evitar: ${if (todosAlimentos.isBlank()) "Ninguno" else todosAlimentos}\n")

        val medicamentosEvitar = report.profileData.medicamentosEvitar.joinToString(", ")
        sb.append("Medicamentos a Evitar: ${if (medicamentosEvitar.isBlank()) "Ninguno" else medicamentosEvitar}\n\n")

        sb.append("II. MEDICACIÓN ACTUAL (${report.medicamentos.size} registros)\n")
        sb.append("-----------------------------------\n")
        if (report.medicamentos.isNotEmpty()) {
            report.medicamentos.forEachIndexed { index, m ->
                sb.append("${index + 1}. ${m.nombre} - ${m.dosis} ${m.unidad}\n")
                sb.append("   Frecuencia: ${m.frecuencia} (Inicio: ${m.fecha}, Hora: ${m.hora})\n")
            }
        } else {
            sb.append("No hay medicamentos registrados.\n")
        }
        sb.append("\n")

        sb.append("III. HISTORIAL DE MEDICIONES (${report.mediciones.size} registros)\n")
        sb.append("-----------------------------------\n")
        if (report.mediciones.isNotEmpty()) {
            report.mediciones.forEach { m ->
                val status = getPressureStatus(m.sistolica, m.diastolica)
                sb.append("» ${m.date} ${m.time} | PAS/PAD: ${m.sistolica}/${m.diastolica} (Pulso: ${m.pulso}) - $status\n")
            }
        } else {
            sb.append("No hay mediciones registradas.\n")
        }
        sb.append("\n")

        sb.append("IV. REGISTRO DE SÍNTOMAS (${report.sintomas.size} registros)\n")
        sb.append("-----------------------------------\n")
        if (report.sintomas.isNotEmpty()) {
            report.sintomas.forEach { s ->
                val sintomaNombres = s.sintomas.filter { it.id != "bien" }.joinToString(", ") { it.nombre }
                val displaySintomas = if (sintomaNombres.isBlank()) "Asintomático" else sintomaNombres
                sb.append("» ${s.fecha} ${s.hora} | Síntomas: $displaySintomas\n")
                if (s.nota.isNotBlank()) {
                    sb.append("   Nota: ${s.nota}\n")
                }
            }
        } else {
            sb.append("No hay registros de síntomas.\n")
        }
        sb.append("\n")

        sb.append("V. REGISTRO DE DIETA (${report.registrosDieta.size} registros)\n")
        sb.append("-----------------------------------\n")
        if (report.registrosDieta.isNotEmpty()) {
            report.registrosDieta.forEach { dia ->
                val fechaFormateada = dia.fecha.toDate()
                    .toInstant().atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

                sb.append("» Fecha: $fechaFormateada | Total: ${"%.0f".format(dia.calorias_totales_dia)} cal (Meta: ${"%.0f".format(report.profileData.meta_calorias_diarias)} cal)\n")
                val desayunoStr = dia.desayuno.joinToString(", ") { "${it.cantidad} ${it.nombre}" }
                if (desayunoStr.isNotBlank()) sb.append("   Des: $desayunoStr\n")
                val comidaStr = dia.comida.joinToString(", ") { "${it.cantidad} ${it.nombre}" }
                if (comidaStr.isNotBlank()) sb.append("   Com: $comidaStr\n")
            }
        } else {
            sb.append("No hay registros de dieta.\n")
        }
        sb.append("\n")

        sb.append("VI. REGISTRO DE ACTIVIDAD FÍSICA (${report.registrosActividad.size} registros)\n")
        sb.append("-------------------------------------------\n")
        if (report.registrosActividad.isNotEmpty()) {
            report.registrosActividad.forEach { reg ->
                val actividadesStr = reg.actividades.joinToString(", ") { "${it.nombre} (${it.duracionEnMinutos} min)" }
                val totalMinutos = reg.actividades.sumOf { it.duracionEnMinutos }
                sb.append("» Fecha: ${reg.fecha} ${reg.hora} | Total: $totalMinutos min\n")
                sb.append("   Actividades: $actividadesStr\n")
                if (!reg.nota.isNullOrBlank()) {
                    sb.append("   Nota: ${reg.nota}\n")
                }
            }
        } else {
            sb.append("No hay registros de actividad física.\n")
        }

        sb.append("===================================\n")
        return sb.toString()
    }


    private fun getPressureStatus(sistolica: Int, diastolica: Int): String {
        return when {
            sistolica < 120 && diastolica < 80 -> "Normal"
            sistolica >= 120 && sistolica <= 129 && diastolica < 80 -> "Elevada"
            sistolica >= 130 && sistolica <= 139 || diastolica >= 80 && diastolica <= 89 -> "Hipertensión Etapa 1"
            sistolica >= 140 || diastolica >= 90 -> "Hipertensión Etapa 2"
            sistolica > 180 || diastolica > 120 -> "Crisis Hipertensiva"
            else -> "Indeterminado"
        }
    }


    private fun exportarInformeMedicoAPDF(report: AnalysisReport) {
        val pdfDoc = PdfDocument()
        val titlePaint = Paint().apply {
            textSize = 22f
            isFakeBoldText = true
            color = android.graphics.Color.parseColor("#466B95")
        }
        val sectionTitlePaint = Paint().apply {
            textSize = 18f
            isFakeBoldText = true
            color = android.graphics.Color.parseColor("#333333")
        }
        val normalBoldPaint = Paint().apply {
            textSize = 14f
            isFakeBoldText = true
            color = android.graphics.Color.BLACK
        }
        val normalPaint = Paint().apply {
            textSize = 14f
            color = android.graphics.Color.BLACK
        }
        val smallPaint = Paint().apply {
            textSize = 10f
            color = android.graphics.Color.BLACK
        }
        val smallBoldPaint = Paint().apply {
            textSize = 10f
            isFakeBoldText = true
            color = android.graphics.Color.BLACK
        }

        var pageNumber = 1
        var y = 70f
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDoc.startPage(pageInfo)
        var canvas: Canvas = page.canvas
        val marginX = 40f
        val lineSpacing = 16f
        val sectionSpacing = 22f
        val pageBottomMargin = 800f

        fun checkY(forcePageBreak: Boolean = false): Boolean {
            if (y > pageBottomMargin || forcePageBreak) {
                pdfDoc.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDoc.startPage(pageInfo)
                canvas = page.canvas
                y = 50f
                canvas.drawText("Página $pageNumber", 520f, 820f, smallPaint)
                return true
            }
            return false
        }

        fun drawTextLine(text: String, paint: Paint, x: Float = marginX, indent: Float = 0f): Boolean {
            canvas.drawText(text, x + indent, y, paint)
            y += lineSpacing
            return checkY()
        }

        fun drawSectionTitle(title: String) {
            checkY(y > pageBottomMargin - 50f)
            drawTextLine(title, sectionTitlePaint)
            y += 5f
            checkY()
        }

        canvas.drawText("INFORME DE ANÁLISIS MÉDICO", 150f, y, titlePaint)
        y += lineSpacing * 2; checkY()
        drawTextLine("Fecha del Reporte: ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}", normalPaint)
        y += sectionSpacing; checkY()

        drawSectionTitle("I. DATOS DEL PACIENTE Y ANTECEDENTES")
        drawTextLine("Nombre: ${report.profileData.nombre}", normalPaint, indent = 120f)
        canvas.drawText("Nombre:", marginX, y - lineSpacing, normalBoldPaint)
        drawTextLine("Edad/Sexo: ${report.profileData.edad} años / ${report.profileData.sexo}", normalPaint, indent = 120f)
        canvas.drawText("Edad/Sexo:", marginX, y - lineSpacing, normalBoldPaint)
        drawTextLine("Peso/Altura: ${report.profileData.peso} kg / ${report.profileData.altura} m", normalPaint, indent = 120f)
        canvas.drawText("Peso/Altura:", marginX, y - lineSpacing, normalBoldPaint)
        drawTextLine("IMC: ${report.profileData.imc}", normalPaint, indent = 120f)
        canvas.drawText("IMC:", marginX, y - lineSpacing, normalBoldPaint)
        drawTextLine("Próx. Cita: ${report.profileData.proxima_cita_medica}", normalPaint, indent = 120f)
        canvas.drawText("Próx. Cita:", marginX, y - lineSpacing, normalBoldPaint)

        val padecimientosPreNombres = report.profileData.padecimientos_pre.map { padecimientosMap[it] ?: it }
        val padecimientosPer = report.profileData.padecimientos_per
        val todosPadecimientos = (padecimientosPreNombres + padecimientosPer).filter { it.isNotBlank() }.joinToString("; ")
        drawTextLine("Padecimientos: ${if (todosPadecimientos.isBlank()) "Ninguno" else todosPadecimientos}", normalPaint, indent = 120f)
        canvas.drawText("Padecimientos:", marginX, y - lineSpacing, normalBoldPaint)

        val alimentosPreNombres = report.profileData.alimentosEvitar_pre.map { alimentosMap[it] ?: it }
        val alimentosPer = report.profileData.alimentosEvitar_per
        val todosAlimentos = (alimentosPreNombres + alimentosPer).filter { it.isNotBlank() }.joinToString("; ")
        drawTextLine("Alim. a Evitar: ${if (todosAlimentos.isBlank()) "Ninguno" else todosAlimentos}", normalPaint, indent = 120f)
        canvas.drawText("Alim. a Evitar:", marginX, y - lineSpacing, normalBoldPaint)

        val medicamentosEvitar = report.profileData.medicamentosEvitar.joinToString(", ")
        drawTextLine("Medic. a Evitar: ${if (medicamentosEvitar.isBlank()) "Ninguno" else medicamentosEvitar}", normalPaint, indent = 120f)
        canvas.drawText("Medic. a Evitar:", marginX, y - lineSpacing, normalBoldPaint)

        y += sectionSpacing; checkY()

        drawSectionTitle("II. MEDICACIÓN ACTUAL (${report.medicamentos.size} registros)")
        if (report.medicamentos.isNotEmpty()) {
            report.medicamentos.forEachIndexed { index, m ->
                val nombre = "${index + 1}. ${m.nombre} (${m.dosis} ${m.unidad})"
                val freq = "Frecuencia: ${m.frecuencia} (Inicio: ${m.fecha}, Hora: ${m.hora})"
                drawTextLine(nombre, normalBoldPaint, indent = 10f)
                drawTextLine(freq, smallPaint, indent = 20f)
                y += 5f; checkY()
            }
        } else {
            drawTextLine("No hay medicamentos registrados.", normalPaint, indent = 10f)
        }
        y += sectionSpacing; checkY()

        drawSectionTitle("III. HISTORIAL DE MEDICIONES (${report.mediciones.size} registros)")
        if (report.mediciones.isNotEmpty()) {
            report.mediciones.forEach { m ->
                val status = getPressureStatus(m.sistolica, m.diastolica)
                val texto = "» ${m.date} ${m.time} | PAS/PAD: ${m.sistolica}/${m.diastolica} (Pulso: ${m.pulso}) | $status"
                drawTextLine(texto, smallPaint, indent = 10f)
            }
        } else {
            drawTextLine("No hay mediciones registradas.", normalPaint, indent = 10f)
        }
        y += sectionSpacing; checkY()

        drawSectionTitle("IV. REGISTRO DE SÍNTOMAS (${report.sintomas.size} registros)")
        if (report.sintomas.isNotEmpty()) {
            report.sintomas.forEach { s ->
                val sintomaNombres = s.sintomas.filter { it.id != "bien" }.joinToString(", ") { it.nombre }
                val displaySintomas = if (sintomaNombres.isBlank()) "Asintomático" else sintomaNombres
                drawTextLine("» ${s.fecha} ${s.hora} | Síntomas: $displaySintomas", smallBoldPaint, indent = 10f)
                if (s.nota.isNotBlank()) {
                    drawTextLine("Nota: ${s.nota}", smallPaint, indent = 25f)
                }
                y += 5f; checkY()
            }
        } else {
            drawTextLine("No hay registros de síntomas.", normalPaint, indent = 10f)
        }
        y += sectionSpacing; checkY()

        drawSectionTitle("V. RESUMEN DE DIETA (${report.registrosDieta.size} registros)")
        if (report.registrosDieta.isNotEmpty()) {
            report.registrosDieta.forEach { dia ->
                val fechaFormateada = dia.fecha.toDate()
                    .toInstant().atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                val total = "%.0f".format(dia.calorias_totales_dia)
                val meta = "%.0f".format(report.profileData.meta_calorias_diarias)
                drawTextLine("» $fechaFormateada | Total: $total cal (Meta: $meta cal)", smallBoldPaint, indent = 10f)

                val desayunoStr = dia.desayuno.joinToString(", ") { "${it.cantidad} ${it.nombre}" }
                if (desayunoStr.isNotBlank()) drawTextLine("Des: $desayunoStr", smallPaint, indent = 25f)
                val comidaStr = dia.comida.joinToString(", ") { "${it.cantidad} ${it.nombre}" }
                if (comidaStr.isNotBlank()) drawTextLine("Com: $comidaStr", smallPaint, indent = 25f)
                y += 5f; checkY()
            }
        } else {
            drawTextLine("No hay registros de dieta.", normalPaint, indent = 10f)
        }
        y += sectionSpacing; checkY()

        drawSectionTitle("VI. ACTIVIDAD FÍSICA (${report.registrosActividad.size} registros)")
        if (report.registrosActividad.isNotEmpty()) {
            report.registrosActividad.forEach { reg ->
                val totalMin = reg.actividades.sumOf { it.duracionEnMinutos }
                val actividadesStr = reg.actividades.joinToString(", ") { "${it.nombre} (${it.duracionEnMinutos} min)" }
                drawTextLine("» ${reg.fecha} ${reg.hora} | Total: $totalMin min", smallBoldPaint, indent = 10f)
                drawTextLine("Act: $actividadesStr", smallPaint, indent = 25f)
                if (!reg.nota.isNullOrBlank()) {
                    drawTextLine("Nota: ${reg.nota}", smallPaint, indent = 25f)
                }
                y += 5f; checkY()
            }
        } else {
            drawTextLine("No hay registros de actividad física.", normalPaint, indent = 10f)
        }

        pdfDoc.finishPage(page)
        val directory = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val pdfFile = File(directory, "Reporte_Hipertension_${LocalDate.now()}.pdf")

        try {
            FileOutputStream(pdfFile).use { pdfDoc.writeTo(it) }
            Toast.makeText(this, "PDF guardado en: Documentos/${pdfFile.name}", Toast.LENGTH_LONG).show()

            val fileUri: Uri = FileProvider.getUriForFile(
                this,
                "com.example.apphipertension.fileprovider",
                pdfFile
            )

            val viewPdfIntent = Intent(Intent.ACTION_VIEW)
            viewPdfIntent.setDataAndType(fileUri, "application/pdf")
            viewPdfIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY

            startActivity(viewPdfIntent)

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
        pdfDoc.close()
    }
}