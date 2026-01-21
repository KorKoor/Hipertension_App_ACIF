// AlimentoAdapter.kt
package com.example.apphipertension

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.apphipertension.databinding.ItemAlimentoBusquedaBinding

// El constructor recibe 2 cosas:
// 1. Una lista de alimentos (inicialmente puede estar vacía)
// 2. Una "función lambda" que se ejecutará cuando alguien haga clic en un ítem
class AlimentoAdapter(
    private var alimentos: List<Alimento>,
    private val onItemClicked: (Alimento) -> Unit
) : RecyclerView.Adapter<AlimentoAdapter.AlimentoViewHolder>() {

    /**
     * El ViewHolder: Representa una sola fila (item_alimento_busqueda.xml)
     * y "sostiene" las vistas (TextViews, etc.)
     */
    inner class AlimentoViewHolder(private val binding: ItemAlimentoBusquedaBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(alimento: Alimento) {
            // 1. Pone los datos del alimento en los TextViews
            binding.tvNombreAlimento.text = alimento.nombre
            binding.tvCaloriasBase.text = "${alimento.calorias_base} cal / ${alimento.unidad_base}"

            // 2. Asigna el click listener a toda la vista de la fila
            binding.root.setOnClickListener {
                onItemClicked(alimento)
            }
        }
    }

    /**
     * Se llama cuando RecyclerView necesita crear una NUEVA fila.
     * Infla el XML y crea el ViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlimentoViewHolder {
        val binding = ItemAlimentoBusquedaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AlimentoViewHolder(binding)
    }

    /**
     * Se llama para MOSTRAR los datos en una fila específica.
     * Reutiliza los ViewHolders.
     */
    override fun onBindViewHolder(holder: AlimentoViewHolder, position: Int) {
        holder.bind(alimentos[position])
    }

    /**
     * Le dice al RecyclerView cuántos ítems hay en total.
     */
    override fun getItemCount(): Int = alimentos.size

    /**
     * Función especial para actualizar la lista (la usaremos para el buscador).
     */
    fun updateList(newList: List<Alimento>) {
        alimentos = newList
        notifyDataSetChanged() // Le dice al RecyclerView que se "redibuje"
    }
}