package com.mayo.bustrackcancun

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mayo.bustrackcancun.API.Ruta

class RutaAdapter(private var rutas: List<Ruta>, private val onClick: (Ruta) -> Unit) :
    RecyclerView.Adapter<RutaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvRutaNombre)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ruta_sugerida, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ruta = rutas[position]
        holder.tvNombre.text = ruta.nombre
        holder.itemView.setOnClickListener { onClick(ruta) }
    }

    override fun getItemCount() = rutas.size

    fun updateData(newRutas: List<Ruta>) {
        rutas = newRutas
        notifyDataSetChanged()
    }
}