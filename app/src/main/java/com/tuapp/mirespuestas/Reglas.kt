package com.tuapp.mirespuestas

data class ReglaMessenger(val pregunta: String, var respuesta: String)

object MisDatos {
    var bienvenida: String = "Hola! Gracias por escribirnos. ¿En qué te ayudamos?"
    var bienvenidaActiva: Boolean = true
    var activo: Boolean = true
    var reglas: MutableList<ReglaMessenger> = mutableListOf(
        ReglaMessenger("precio", "Cuesta \$2"),
        ReglaMessenger("horario", "Estamos de lunes a viernes de 9:00 a 18:00 h.")
    )
}

object Borradores {
    val lista = mutableListOf<String>()
}
