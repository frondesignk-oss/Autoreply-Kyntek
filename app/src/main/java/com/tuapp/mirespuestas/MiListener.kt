package com.tuapp.mirespuestas

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import kotlin.random.Random

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
            .setContentTitle("Respondí mensaje a " + nombre)
            .setContentText("Respondí mensaje a " + nombre)
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
            val palabras = regla.pregunta.split(",")
            var encontro = false
            for (pal in palabras) {
                val p = pal.trim().lowercase()
                if (p.isNotEmpty() && textoLower.contains(p)) {
                    encontro = true
                    break
                }
            }
            if (encontro) {
                respuestaFinal = regla.respuesta
                break
            }
        }
        if (respuestaFinal == null) return

        guardar("De: " + titulo + "\nMensaje: " + texto + "\nRespuesta: " + respuestaFinal)

        // DELAY RANDOM 10 a 30 segundos para parecer humano
        val delay = Random.nextLong(5000, 8001)
        guardar("DEBUG: Espero " + (delay / 1000) + " seg antes de responder a " + titulo)

        val respuestaParaEnviar = respuestaFinal
        val tituloParaEnviar = titulo
        val keyParaBorrar = sbn.key
        val notificacionOriginal = sbn.notification

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                val wearable = NotificationCompat.WearableExtender(notificacionOriginal)
                val todasAcciones = mutableListOf<NotificationCompat.Action>()
                for (i in 0 until NotificationCompat.getActionCount(notificacionOriginal)) {
                    NotificationCompat.getAction(notificacionOriginal, i)?.let { todasAcciones.add(it) }
                }
                todasAcciones.addAll(wearable.actions)

                guardar("DEBUG: Encontre " + todasAcciones.size + " acciones")

                val ordenadas = todasAcciones.sortedByDescending {
                    val t = it.title?.toString()?.lowercase() ?: ""
                    if (t.contains("responder") || t.contains("reply")) 1 else 0
                }

                for (action in ordenadas) {
                    val inputs = action.remoteInputs ?: continue
                    if (inputs.isEmpty()) continue

                    guardar("DEBUG: Intento responder con accion: " + action.title)

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
                        bundle.putCharSequence(ri.resultKey, respuestaParaEnviar)
                    }
                    android.app.RemoteInput.addResultsToIntent(nativeInputs, intent, bundle)

                    try {
                        action.actionIntent?.send(this, 0, intent)
                        guardar("DEBUG: ENVIADO OK a " + tituloParaEnviar + " con accion " + action.title)
                        mostrarAviso(tituloParaEnviar)
                        try { cancelNotification(keyParaBorrar) } catch (e: Exception) {}
                        return@postDelayed
                    } catch (e: Exception) {
                        guardar("DEBUG ERROR SEND 2: " + e.message)
                    }
                }
                guardar("DEBUG: No encontre ninguna accion con RemoteInput.")
            } catch (e: Exception) {
                guardar("DEBUG ERROR AL ENVIAR: " + e.message)
            }
        }, delay)
    }
}
