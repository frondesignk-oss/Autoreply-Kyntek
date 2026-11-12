package com.tuapp.mirespuestas
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class MiListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != "com.facebook.orca") return
        val nombre = sbn.notification.extras.getString("android.title") ?: return
        val mensaje = sbn.notification.extras.getCharSequence("android.text")?.toString() ?: return
        if (mensaje.isEmpty()) return

        val borrador: String = if (MisDatos.bienvenidaActiva && !MisDatos.yaSaludados.contains(nombre)) {
            MisDatos.yaSaludados.add(nombre)
            MisDatos.bienvenida
        } else {
            MisDatos.buscarRespuesta(mensaje) ?: "Gemini: Responder a: $mensaje"
        }
        // Guarda el borrador para que lo veas en la app
        Borradores.lista.add("$nombre: $mensaje\n-> Sugerencia: $borrador")
    }
}
object Borradores { val lista = mutableListOf<String>() }
