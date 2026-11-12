package com.tuapp.mirespuestas

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class MiListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        // SOLO MESSENGER DE FACEBOOK
        if (sbn.packageName != "com.facebook.orca") return

        val prefs = getSharedPreferences("autoreply", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("activo", true)) return

        val extras = sbn.notification.extras
        val titulo = extras.getString("android.title") ?: return
        val mensaje = extras.getString("android.text") ?: return

        // Evita responderte a vos mismo o notificaciones del sistema
        if (titulo.equals("Messenger", true)) return
        if (mensaje.isBlank()) return

        val mensajeMinus = mensaje.lowercase()
        val reglasTexto = prefs.getString("reglas", "") ?: ""
        val bienvenidaActiva = prefs.getBoolean("bienvenidaActiva", true)
        val bienvenida = prefs.getString("bienvenida", MisDatos.bienvenida) ?: MisDatos.bienvenida

        // 1. Buscar si el mensaje tiene alguna palabra clave
        var respuestaEncontrada: String? = null
        var preguntaEncontrada = ""

        if (reglasTexto.isNotEmpty()) {
            reglasTexto.split("##").forEach {
                val partes = it.split("||")
                if (partes.size >= 2) {
                    val pregunta = partes[0].trim().lowercase()
                    val respuesta = partes[1].trim()
                    if (pregunta.isNotEmpty() && mensajeMinus.contains(pregunta)) {
                        respuestaEncontrada = respuesta
                        preguntaEncontrada = partes[0]
                    }
                }
            }
        }

        // 2. Si encontró una pregunta, preparar el borrador
        if (respuestaEncontrada != null) {
            val borrador = "De: $titulo\n" +
                    "Te escribió: \"$mensaje\"\n" +
                    "Detectó: $preguntaEncontrada\n" +
                    "Respuesta lista: $respuestaEncontrada"
            // No duplicar el mismo borrador
            if (!Borradores.lista.contains(borrador)) {
                Borradores.lista.add(0, borrador)
            }
            return
        }

        // 3. Si no encontró pregunta, pero es la primera vez, preparar bienvenida
        if (bienvenidaActiva && bienvenida.isNotBlank()) {
            val borradorBienvenida = "De: $titulo\n" +
                    "Te escribió: \"$mensaje\"\n" +
                    "Respuesta lista (bienvenida): $bienvenida"
            if (!Borradores.lista.contains(borradorBienvenida)) {
                Borradores.lista.add(0, borradorBienvenida)
            }
        }
    }
}
