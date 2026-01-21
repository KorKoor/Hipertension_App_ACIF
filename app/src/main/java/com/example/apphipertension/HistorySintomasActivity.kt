package com.example.apphipertension

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Nota: Asegúrate de que SintomaGuardado y Sintoma estén definidos en tu proyecto
// o dentro de este mismo archivo si son clases simples.

class HistorySintomasActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DOCUMENT_ID = "document_id_edicion"
        const val EXTRA_FECHA = "fecha_edicion"
        const val EXTRA_HORA = "hora_edicion"
        const val EXTRA_NOTA = "nota_edicion"
        const val EXTRA_SINTOMA_IDS = "sintoma_ids_edicion"
    }

    private lateinit var rvHistorial: RecyclerView
    private lateinit var tvNoHistorial: TextView
    private lateinit var historialAdapter: HistorialSintomasAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history_sintomas)

        // Configurar Toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbarHistorial)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            title = "Historial de Síntomas"
            setDisplayHomeAsUpEnabled(true)
        }
        toolbar.setNavigationOnClickListener { finish() }

        rvHistorial = findViewById(R.id.rvHistorialSintomas)
        tvNoHistorial = findViewById(R.id.tvNoHistorial)

        setupRecyclerView()
        setupRealtimeListener()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupRecyclerView() {
        rvHistorial.layoutManager = LinearLayoutManager(this)
        historialAdapter = HistorialSintomasAdapter(
            mutableListOf(),
            onEditClick = { registro -> iniciarEdicion(registro) },
            onDeleteClick = { registro -> confirmarYEliminar(registro) }
        )
        rvHistorial.adapter = historialAdapter
    }

    private fun formatHour12h(hour24: String): String {
        return try {
            val time = LocalTime.parse(hour24)
            time.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))
        } catch (e: Exception) {
            hour24
        }
    }

    private fun formatDate(dateISO: String): String {
        return try {
            val date = LocalDate.parse(dateISO)
            date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        } catch (e: Exception) {
            dateISO
        }
    }

    private fun setupRealtimeListener() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            tvNoHistorial.text = "Inicia sesión para ver tu historial."
            tvNoHistorial.visibility = View.VISIBLE
            return
        }

        db.collection("users")
            .document(userId)
            .collection("sintomas")
            .orderBy("fecha", Query.Direction.DESCENDING)
            .orderBy("hora", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    // ESTA LÍNEA ES CLAVE: Imprime el error real y el LINK del índice en la consola
                    android.util.Log.e("FirestoreError", "Error en síntomas: ${e.message}", e)

                    tvNoHistorial.text = "Error al cargar datos. Revisa la configuración del servidor."
                    tvNoHistorial.visibility = View.VISIBLE
                    return@addSnapshotListener
                }

                val listaHistorial = mutableListOf<SintomaGuardado>()

                snapshots?.documents?.forEach { doc ->
                    val fecha = doc.getString("fecha") ?: ""
                    val hora = doc.getString("hora") ?: ""
                    val nota = doc.getString("nota") ?: ""

                    val sintomasRaw = doc.get("sintomas") as? List<Map<String, Any>> ?: emptyList()
                    val sintomasRegistrados = sintomasRaw.map { map ->
                        Sintoma(
                            id = map["id"] as? String ?: "",
                            nombre = map["nombre"] as? String ?: "Desconocido",
                            iconResId = 0,
                            seleccionado = true
                        )
                    }

                    listaHistorial.add(SintomaGuardado(
                        documentId = doc.id,
                        fecha = fecha,
                        hora = hora,
                        sintomas = sintomasRegistrados,
                        nota = nota
                    ))
                }

                actualizarInterfaz(listaHistorial)
            }
    }

    private fun actualizarInterfaz(lista: List<SintomaGuardado>) {
        if (lista.isEmpty()) {
            tvNoHistorial.visibility = View.VISIBLE
            rvHistorial.visibility = View.GONE
        } else {
            tvNoHistorial.visibility = View.GONE
            rvHistorial.visibility = View.VISIBLE
            historialAdapter.updateData(lista)
        }
    }

    private fun iniciarEdicion(registro: SintomaGuardado) {
        val intent = Intent(this, Sintomas::class.java).apply {
            putExtra(EXTRA_DOCUMENT_ID, registro.documentId)
            putExtra(EXTRA_FECHA, registro.fecha)
            putExtra(EXTRA_HORA, registro.hora)
            putExtra(EXTRA_NOTA, registro.nota)

            val sintomaIds = ArrayList(registro.sintomas.map { it.id })
            putStringArrayListExtra(EXTRA_SINTOMA_IDS, sintomaIds)

            // Manejo del síntoma personalizado "otro"
            registro.sintomas.find { it.id == "otro" }?.let {
                putExtra("otro_sintoma_nota", it.nombre)
            }
        }
        startActivity(intent)
    }

    private fun confirmarYEliminar(registro: SintomaGuardado) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar registro")
            .setMessage("¿Deseas borrar el reporte del ${formatDate(registro.fecha)}?")
            .setPositiveButton("Eliminar") { _, _ -> eliminarRegistro(registro.documentId) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarRegistro(documentId: String) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .collection("sintomas").document(documentId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Eliminado con éxito", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al eliminar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // --- Adaptador Interno ---
    inner class HistorialSintomasAdapter(
        private var historial: MutableList<SintomaGuardado>,
        private val onEditClick: (SintomaGuardado) -> Unit,
        private val onDeleteClick: (SintomaGuardado) -> Unit
    ) : RecyclerView.Adapter<HistorialSintomasAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvDateTime: TextView = view.findViewById(R.id.tvHistorialDateTime)
            val tvSintomas: TextView = view.findViewById(R.id.tvHistorialSintomas)
            val tvNota: TextView = view.findViewById(R.id.tvHistorialNota)
            val btnEditar: Button = view.findViewById(R.id.btnEditarSintoma)
            val btnEliminar: Button = view.findViewById(R.id.btnEliminarSintoma)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_historial_sintoma, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val registro = historial[position]

            holder.tvDateTime.text = "${formatDate(registro.fecha)} • ${formatHour12h(registro.hora)}"

            // Unimos los nombres de los síntomas en un párrafo legible
            val sintomasText = registro.sintomas.joinToString(", ") { it.nombre }
            holder.tvSintomas.text = "Síntomas: $sintomasText"

            if (registro.nota.isEmpty()) {
                holder.tvNota.visibility = View.GONE
            } else {
                holder.tvNota.visibility = View.VISIBLE
                holder.tvNota.text = "Nota: ${registro.nota}"
            }

            holder.btnEditar.setOnClickListener { onEditClick(registro) }
            holder.btnEliminar.setOnClickListener { onDeleteClick(registro) }
        }

        override fun getItemCount() = historial.size

        fun updateData(newHistorial: List<SintomaGuardado>) {
            this.historial.clear()
            this.historial.addAll(newHistorial)
            notifyDataSetChanged()
        }
    }
}