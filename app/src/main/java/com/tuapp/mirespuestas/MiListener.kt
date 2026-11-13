package com.tuapp.mirespuestas
import androidx.core.app.RemoteInput
import android.app.RemoteInput as NativeRemoteInput
import androidx.core.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat

class MiListener : NotificationListenerService() {

    private fun guardar(texto: String) {
        val p = getSharedPreferences("autoreply", Context.MODE_PRIVATE)
        val anterior = p.getString("borradores_guardados", "") ?: ""
        p.edit().putString("borradores_guardados", (texto + "\n\n" + anterior).take(5000)).apply()
        Borradores.lista.add(texto)
    }

    private fun cargarReglas(): List<ReglaMessenger> {
        val p = getSharedPreferences("autoreply", Context.MODE_PRIVATE)
        val g = p.getString("reglas", "")
        if (g.isNullOrEmpty()) return listOf(ReglaMessenger("hola", "gt"))
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

        val extras = sbn.notification.extras
        val texto = extras.getCharSequence("android.text")?.toString() ?: ""
        val titulo = extras.getCharSequence("android.title")?.toString() ?: ""

        // 1. FILTROS QUE FALTABAN
        if (texto.isBlank()) return
        if (sbn.isOngoing) return
        if (titulo == "Messenger") return // es el "Buscando mensajes nuevos"
        if (texto.contains("Buscando mensajes", ignoreCase = true)) return
        if (texto.contains("mensajes nuevos", ignoreCase = true)) return

        val p = getSharedPreferences("autoreply", Context.MODE_PRIVATE)
        if (!p.getBoolean("activo", true)) return

        val reglas = cargarReglas()
        val textoLower = texto.lowercase()
        var respuestaFinal: String? = null
        for (regla in reglas) {
            if (textoLower.contains(regla.pregunta.lowercase())) {
                respuestaFinal = regla.respuesta
                break
            }
        }
        if (respuestaFinal == null) return

        guardar("De: $titulo\nMensaje: $texto\nRespuesta: $respuestaFinal")

        // 2. BUSCAR LA ACCION DE RESPONDER DE MESSENGER
        try {
            val wearable = NotificationCompat.WearableExtender(sbn.notification)
            val todasAcciones = mutableListOf<NotificationCompat.Action>()
            todasAcciones.addAll(wearable.actions)
            // Tambien las acciones normales
            for (i in 0 until NotificationCompat.getActionCount(sbn.notification)) {
                NotificationCompat.getAction(sbn.notification, i)?.let { todasAcciones.add(it) }
            }

            guardar("DEBUG: Encontre ${todasAcciones.size} acciones")

            for (action in todasAcciones) {
                val inputs = action.remoteInputs ?: continue
                if (inputs.isEmpty()) continue

                guardar("DEBUG: Intento responder con accion: ${action.title}")

              // Usar RemoteInput NATIVO, no el de androidx
val nativeInputs = inputs.map { 
    android.app.RemoteInput.Builder(it.resultKey).build() 
}.toTypedArray()

val intent = Intent()
val bundle = Bundle()
for (ri in nativeInputs) {
    bundle.putCharSequence(ri.resultKey, respuestaFinal)
}
android.app.RemoteInput.addResultsToIntent(nativeInputs, intent, bundle)

// Esto es clave: hay que mandarlo como fillInIntent del PendingIntent
try {
    action.actionIntent?.send(this, 0, intent)
} catch (e: Exception) {
    guardar("DEBUG ERROR SEND 2: ${e.message}")
}
                guardar("DEBUG: ENVIADO OK a $titulo")
                return
            }
            guardar("DEBUG: No encontre ninguna accion con RemoteInput. Messenger no deja responder desde notificacion.")
        } catch (e: Exception) {
            guardar("DEBUG ERROR AL ENVIAR: ${e.message}")
        }
    }
}
