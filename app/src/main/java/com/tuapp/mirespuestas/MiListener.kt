package com.tuapp.mirespuestas

import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

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
        if (sbn.packageName != "com.facebook.orca") return

        val p = getSharedPreferences("autoreply", Context.MODE_PRIVATE)
        if (!p.getBoolean("activo", true)) return

        val notification = sbn.notification
        val extras = notification.extras
        val texto = extras.getCharSequence("android.text")?.toString() ?: return
        if (texto.isBlank()) return
        if (sbn.isOngoing) return

        // Evitar responder 2 veces el mismo mensaje
        val clave = sbn.key + "|" + texto
        val ultimo = p.getString("ultimo_procesado", "")
        if (clave == ultimo) return
        p.edit().putString("ultimo_procesado", clave).apply()

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

        // Guardar en prefs para que VER BORRADORES lo muestre aunque cierres la app
        val anterior = p.getString("borradores_guardados", "")
        val nuevo = "De: ${extras.getCharSequence("android.title")}\nMensaje: $texto\nRespuesta: $respuestaFinal"
        p.edit().putString("borradores_guardados", (nuevo + "\n\n" + anterior).take(4000)).apply()
        Borradores.lista.add(nuevo)

        // 1. Buscar en acciones normales
        // 2. Buscar en WearableExtender, que es donde Messenger esconde el Responder
        try {
            val listaAcciones = mutableListOf<NotificationCompat.Action>()

            notification.actions?.forEach {
                listaAcciones.add(NotificationCompat.Action(it.icon, it.title, it.actionIntent))
            }

            val wearable = NotificationCompat.WearableExtender(notification)
            listaAcciones.addAll(wearable.actions)

            for (action in listaAcciones) {
                val remoteInputs = action.remoteInputs ?: continue
                if (remoteInputs.isEmpty()) continue

                val intent = Intent()
                val bundle = Bundle()
                for (ri in remoteInputs) {
                    bundle.putCharSequence(ri.resultKey, respuestaFinal)
                }
                RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)
                action.actionIntent?.send(this, 0, intent)
                break
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
