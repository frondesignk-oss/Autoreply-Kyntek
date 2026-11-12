package com.tuapp.mirespuestas

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class MiListener : NotificationListenerService() {

    private fun cargarReglas(): List<ReglaMessenger> {
        val p = getSharedPreferences("autoreply", Context.MODE_PRIVATE)
        val g = p.getString("reglas", "")
        if (g.isNullOrEmpty()) {
            return listOf(
                ReglaMessenger("precio", "Cuesta \$2"),
                ReglaMessenger("horario", "Estamos de lunes a viernes de 9:00 a 18:00 h.")
            )
        }
        val lista = mutableListOf<ReglaMessenger>()
        g.split("##").forEach {
            val partes = it.split("||")
            if (partes.size >= 2) lista.add(ReglaMessenger(partes[0], partes[1]))
        }
        return lista
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (sbn.packageName != "com.facebook.orca" && sbn.packageName != "com.facebook.messenger") return

        val p = getSharedPreferences("autoreply", Context.MODE_PRIVATE)
        val activo = p.getBoolean("activo", true)
        if (!activo) return

        val extras = sbn.notification.extras
        val texto = extras.getCharSequence("android.text")?.toString() ?: return
        val titulo = extras.getCharSequence("android.title")?.toString() ?: ""
        if (texto.isBlank()) return

        val bienvenidaActiva = p.getBoolean("bienvenidaActiva", true)
        val bienvenida = p.getString("bienvenida", "Hola! Gracias por escribirnos. ¿En qué te ayudamos?") ?: ""

        val reglas = cargarReglas()
        val textoLower = texto.lowercase()

        var respuestaEncontrada: String? = null
        for (regla in reglas) {
            if (textoLower.contains(regla.pregunta.lowercase())) {
                respuestaEncontrada = regla.respuesta
                break
            }
        }

        val respuestaFinal = respuestaEncontrada ?: if (bienvenidaActiva) bienvenida else return
        Borradores.lista.add("De: $titulo\nMensaje: $texto\nRespuesta: $respuestaFinal")
    }
}
