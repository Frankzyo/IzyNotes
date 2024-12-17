package com.programovil.izynotes

data class Nota(
    var id: String = "", // Cambiado a 'var' para que sea reasignable
    val titulo: String = "",
    val usuarioId: String = "",
    val elementos: List<Map<String, Any>>? = null
)

