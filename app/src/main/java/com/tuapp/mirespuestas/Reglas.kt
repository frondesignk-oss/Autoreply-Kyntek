package com.tuapp.mirespuestas

import android.content.Context

object MisDatos {
    // Tus reglas por defecto
    var reglas: MutableList<String> = mutableListOf(
        "precio|Cuesta \$2",
        "cuanto|Cuesta \$2",
        "horario|Atendemos de 9 a 18hs"
    )

    private const val PREFS = "mis_respuestas_prefs"
    private const val KEY = "reglas_guardadas"

    fun cargar(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val guardado = prefs.getString(KEY, null)
        if (guardado != null) {
            // Si hay algo guardado, lo usamos. Si no, dejamos los de por defecto
            reglas = guardado.split(";;").toMutableList()
        }
    }

    fun guardar(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY, reglas.joinToString(";;")).apply()
    }
}

object Borradores {
    val lista = mutableListOf<String>()
}
