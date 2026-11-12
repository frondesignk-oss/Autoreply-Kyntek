package com.tuapp.mirespuestas

data class Regla(val palabra: String, val respuestas: List<String>)

object MisDatos {
    var bienvenida = "¡Hola! Gracias por escribirnos 😊 ¿En qué puedo ayudarte hoy?"
    var bienvenidaActiva = true
    val yaSaludados = mutableSetOf<String>()

    var reglas = listOf(
        Regla("precio", listOf(
            "Nuestros precios van desde $150 hasta $500 según el modelo.",
            "Te puedo pasar el catálogo con precios actualizados si querés.",
            "¿Te comparto la lista con promos de este mes?"
        )),
        Regla("horario", listOf(
            "Estamos de lunes a viernes de 9 a 18h.",
            "Los findes respondemos de 10 a 14h por acá."
        ))
    )

    fun buscarRespuesta(texto: String): String? {
        val t = texto.lowercase()
        for (r in reglas) {
            if (t.contains(r.palabra)) return r.respuestas.random()
        }
        return null
    }
}
