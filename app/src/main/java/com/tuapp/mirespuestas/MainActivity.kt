package com.tuapp.mirespuestas
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40,40,40,40) }
        val titulo = TextView(this).apply { text = "Mi Respuestas - Sin anuncios"; textSize = 20f }
        val txtBienvenida = EditText(this).apply { setText(MisDatos.bienvenida) }
        val btnGuardar = Button(this).apply { text = "Guardar mi bienvenida" }
        val lista = TextView(this).apply { text = "Acá van a aparecer los borradores..." }
        val btnVer = Button(this).apply { text = "Ver borradores" }

        btnGuardar.setOnClickListener { MisDatos.bienvenida = txtBienvenida.text.toString(); Toast.makeText(this, "Guardado", Toast.LENGTH_SHORT).show() }
        btnVer.setOnClickListener { lista.text = Borradores.lista.joinToString("\n\n") }

        layout.addView(titulo); layout.addView(txtBienvenida); layout.addView(btnGuardar); layout.addView(btnVer); layout.addView(lista)
        setContentView(layout)
    }
}
