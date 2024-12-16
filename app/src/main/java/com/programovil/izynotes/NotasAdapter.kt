package com.programovil.izynotes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class NotasAdapter(
    private val listaNotas: MutableList<Nota>,
    private val onEliminarClick: (String) -> Unit // Callback para eliminar una nota
) : RecyclerView.Adapter<NotasAdapter.ViewHolder>() {

    // ViewHolder que define las vistas para cada elemento de la lista
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titulo: TextView = itemView.findViewById(R.id.textTitulo)
        val descripcion: TextView = itemView.findViewById(R.id.textDescripcion)
        val imagen: ImageView = itemView.findViewById(R.id.ivImagenNota)
        val botonEliminar: ImageView = itemView.findViewById(R.id.btnEliminarNota) // Botón de eliminar
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nota, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val nota = listaNotas[position]

        // Asignar título
        holder.titulo.text = nota.titulo

        // Buscar y asignar descripción
        val descripcionTexto = nota.elementos?.find { it["tipo"] == "texto" }?.get("contenido")
        holder.descripcion.text = descripcionTexto?.toString() ?: "Sin descripción"

        // Asignar imagen opcional
        val imagenUrl = nota.elementos?.find { it["tipo"] == "imagen" }?.get("url")
        if (imagenUrl != null) {
            holder.imagen.visibility = View.VISIBLE
            Glide.with(holder.itemView.context).load(imagenUrl).into(holder.imagen)
        } else {
            holder.imagen.visibility = View.GONE
        }

        // Configurar botón eliminar
        holder.botonEliminar.setOnClickListener {
            onEliminarClick(nota.id) // Notificar a la actividad
        }
    }

    override fun getItemCount(): Int = listaNotas.size
}
