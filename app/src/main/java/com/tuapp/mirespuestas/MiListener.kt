package com.tuapp.mirespuestas

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput

class MiListener : NotificationListenerService() {

    private fun guardar(texto: String) {
        val p = getSharedPreferences("autoreply", Context.MODE_PRIVATE)
        val anterior = p.getString("borradores_guardados", "") ?: ""
        p.edit().putString("borradores_guardados", (texto + "\n\n" + anterior).take(5000)).apply()
        Borradores.lista.add(texto)
    }

    private fun mostrarAviso(nombre: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "kyntek_avisos"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Kyntek Reply", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }
        val intentApp = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(this, 0, intentApp, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val noti = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Respondí mensaje a $nombre")
            .setContentText("Respondí mensaje a $nombre")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        manager.notify(System.currentTimeMillis().toInt(), noti)
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

        if (texto.isBlank()) return
        if (sbn.isOngoing) return
        if (titulo == "Messenger") return
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

        try {
            val wearable = NotificationCompat.WearableExtender(sbn.notification)
            val todasAcciones = mutableListOf<NotificationCompat.Action>()
            // Primero las normales, después las wearable. Las normales son las que Messenger realmente usa
            for (i in 0 until NotificationCompat.getActionCount(sbn.notification)) {
                NotificationCompat.getAction(sbn.notification, i)?.let { todasAcciones.add(it) }
            }
            todasAcciones.addAll(wearable.actions)

            guardar("DEBUG: Encontre ${todasAcciones.size} acciones")

            // Ordenar: primero la que dice Responder / Reply
            val ordenadas = todasAcciones.sortedByDescending {
                val t = it.title?.toString()?.lowercase() ?: ""
                if (t.contains("responder") || t.contains("reply")) 1 else 0
            }

            for (action in ordenadas) {
                val inputs = action.remoteInputs ?: continue
                if (inputs.isEmpty()) continue

                guardar("DEBUG: Intento responder con accion: ${action.title}")

                // NO reconstruir. Usar los RemoteInput originales tal cual vienen de Messenger
                val nativeInputs = inputs.map { compatInput ->
                    val builder = android.app.RemoteInput.Builder(compatInput.resultKey)
                        .setLabel(compatInput.label)
                        .setAllowFreeFormInput(compatInput.allowFreeFormInput)
                    compatInput.choices?.let { builder.setChoices(it) }
                    builder.addExtras(compatInput.extras)
                    builder.build()
                }.toTypedArray()

                val intent = Intent()
                val bundle = Bundle()
                for (ri in nativeInputs) {
                    bundle.putCharSequence(ri.resultKey, respuestaFinal)
                }
                android.app.RemoteInput.addResultsToIntent(nativeInputs, intent, bundle)

                try {
                    action.actionIntent?.send(this, 0, intent)
                    guardar("DEBUG: ENVIADO OK a $titulo con accion ${action.title}")
                    mostrarAviso(titulo)
                    // Esperar un poquito y cancelar la notificación para que no se reintente
                    try { cancelNotification(sbn.key) } catch (e: Exception) {}
                    return
                } catch (e: Exception) {
                    guardar("DEBUG ERROR SEND 2: ${e.message}")
                }
            }
            guardar("DEBUG: No encontre ninguna accion con RemoteInput.")
        } catch (e: Exception) {
            guardar("DEBUG ERROR AL ENVIAR: ${e.message}")
        }
    }
}
