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
    private val onEliminarClick: (String) -> Unit,
    private val onEditarClick: (Nota) -> Unit
) : RecyclerView.Adapter<NotasAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titulo: TextView = itemView.findViewById(R.id.textTitulo)
        val descripcion: TextView = itemView.findViewById(R.id.textDescripcion)
        val imagen: ImageView = itemView.findViewById(R.id.ivImagenNota)
        val botonEliminar: ImageView = itemView.findViewById(R.id.btnEliminarNota)
        val botonEditar: ImageView = itemView.findViewById(R.id.btnEditarNota)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nota, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val nota = listaNotas[position]


        val colores = listOf(R.color.light_blue, R.color.light_yellow, R.color.light_green)
        val colorFondo = colores[position % colores.size]
        holder.itemView.setBackgroundColor(holder.itemView.context.getColor(colorFondo))

        holder.titulo.text = nota.titulo

        val descripcionTexto = nota.elementos?.find { it["tipo"] == "texto" }?.get("contenido")
        holder.descripcion.text = descripcionTexto?.toString() ?: "Sin descripción"

        val imagenUrl = nota.elementos?.find { it["tipo"] == "imagen" }?.get("url")
        if (imagenUrl != null) {
            holder.imagen.visibility = View.VISIBLE
            Glide.with(holder.itemView.context).load(imagenUrl).into(holder.imagen)
        } else {
            holder.imagen.visibility = View.GONE
        }

        holder.botonEliminar.setOnClickListener {
            onEliminarClick(nota.id)
        }


        holder.botonEditar.setOnClickListener {
            onEditarClick(nota)
        }
    }

    override fun getItemCount(): Int = listaNotas.size
}

