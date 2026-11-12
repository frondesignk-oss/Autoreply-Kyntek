package com.tuapp.mirespuestas

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

data class ReglaMessenger(val pregunta: String, var respuesta: String)

class MainActivity : AppCompatActivity() {

    private val reglas = mutableListOf<ReglaMessenger>()
    private lateinit var contenedor: LinearLayout
    private lateinit var switchGlobal: Switch
    private lateinit var switchBienvenida: Switch
    private lateinit var txtBienvenida: EditText
    private lateinit var txtEstado: TextView

    private fun prefs() = getSharedPreferences("autoreply", Context.MODE_PRIVATE)

    private fun redondo(color: Int, radio: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radio
            setStroke(2, Color.parseColor("#E5E5E5"))
        }
    }

    private fun guardar() {
        prefs().edit()
            .putBoolean("activo", switchGlobal.isChecked)
            .putBoolean("bienvenidaActiva", switchBienvenida.isChecked)
            .putString("bienvenida", txtBienvenida.text.toString())
            .putString("reglas", reglas.joinToString("##") { it.pregunta + "||" + it.respuesta })
            .apply()
        MisDatos.bienvenida = txtBienvenida.text.toString()
    }

    private fun cargar() {
        val p = prefs()
        switchGlobal.isChecked = p.getBoolean("activo", true)
        switchBienvenida.isChecked = p.getBoolean("bienvenidaActiva", true)
        txtBienvenida.setText(p.getString("bienvenida", MisDatos.bienvenida))
        reglas.clear()
        val g = p.getString("reglas", "")
        if (g.isNullOrEmpty()) {
            reglas.add(ReglaMessenger("precio", "Cuesta \$2"))
            reglas.add(ReglaMessenger("horario", "Estamos de lunes a viernes de 9:00 a 18:00 h."))
        } else {
            g.split("##").forEach {
                val partes = it.split("||")
                if (partes.size >= 2) reglas.add(ReglaMessenger(partes[0], partes[1]))
            }
        }
        actualizarEstado()
    }

    private fun actualizarEstado() {
        if (switchGlobal.isChecked) {
            txtEstado.text = "ON  •  Respondiendo Messenger"
            txtEstado.setTextColor(Color.parseColor("#0F9D6A"))
        } else {
            txtEstado.text = "OFF  •  Pausado, no responde"
            txtEstado.setTextColor(Color.RED)
        }
    }

    private fun render() {
        contenedor.removeAllViews()
        reglas.forEachIndexed { index, regla ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackground(redondo(Color.WHITE, 32f))
                setPadding(36, 36, 36, 36)
            }
            val chip = TextView(this).apply {
                text = regla.pregunta
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#1B5E3B"))
                setBackground(redondo(Color.parseColor("#C9EFCB"), 24f))
                setPadding(28, 16, 28, 16)
            }
            val resp = TextView(this).apply {
                text = "💬  ${regla.respuesta}"
                textSize = 15f
                setPadding(0, 20, 0, 20)
            }
            val btnEditar = Button(this).apply {
                text = "Editar respuesta"
                setBackground(redondo(Color.parseColor("#E9F7F0"), 40f))
                setTextColor(Color.parseColor("#0F9D6A"))
            }
            btnEditar.setOnClickListener { mostrarDialogoEditar(index) }
            val btnBorrar = TextView(this).apply {
                text = "Eliminar esta pregunta"
                textSize = 13f
                setTextColor(Color.RED)
                gravity = Gravity.CENTER
                setPadding(0, 16, 0, 0)
                setOnClickListener {
                    reglas.removeAt(index)
                    guardar()
                    render()
                }
            }
            card.addView(chip)
            card.addView(resp)
            card.addView(btnEditar)
            card.addView(btnBorrar)
            contenedor.addView(card)
            contenedor.addView(TextView(this).apply { text = ""; textSize = 8f })
        }
    }

    private fun mostrarDialogoNueva() {
        val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 20, 40, 20) }
        val etP = EditText(this).apply { hint = "Ej: precio" }
        val etR = EditText(this).apply { hint = "Ej: Cuesta \$2" }
        lay.addView(TextView(this).apply { text = "Palabra que detecta en Messenger:" })
        lay.addView(etP)
        lay.addView(TextView(this).apply { text = "Respuesta que prepara:" })
        lay.addView(etR)
        AlertDialog.Builder(this)
            .setTitle("Nueva respuesta para Messenger")
            .setView(lay)
            .setPositiveButton("Guardar") { _, _ ->
                val p = etP.text.toString().trim()
                val r = etR.text.toString().trim()
                if (p.isEmpty() || r.isEmpty()) return@setPositiveButton
                reglas.add(ReglaMessenger(p, r))
                guardar()
                render()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoEditar(index: Int) {
        val et = EditText(this).apply { setText(reglas[index].respuesta) }
        AlertDialog.Builder(this)
            .setTitle("Editar: ${reglas[index].pregunta}")
            .setView(et)
            .setPositiveButton("Guardar") { _, _ ->
                reglas[index].respuesta = et.text.toString()
                guardar()
                render()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F7F7F7"))
            setPadding(32, 32, 32, 32)
        }
        val cabecera = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackground(redondo(Color.WHITE, 32f))
            setPadding(36, 36, 36, 36)
        }
        val txtNombre = TextView(this).apply {
            text = "💬  AutoReply Messenger"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        switchGlobal = Switch(this)
        cabecera.addView(txtNombre)
        cabecera.addView(switchGlobal)
        layout.addView(cabecera)
        txtEstado = TextView(this).apply { gravity = Gravity.CENTER; setPadding(0, 12, 0, 12) }
        layout.addView(txtEstado)
        val cardB = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackground(redondo(Color.WHITE, 32f))
            setPadding(36, 36, 36, 36)
        }
        val filaB = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val tB = TextView(this).apply {
            text = "Mensaje de bienvenida"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        switchBienvenida = Switch(this)
        filaB.addView(tB)
        filaB.addView(switchBienvenida)
        cardB.addView(filaB)
        txtBienvenida = EditText(this).apply { setPadding(28, 28, 28, 28) }
        cardB.addView(txtBienvenida)
        layout.addView(cardB)
        val btnGuardar = Button(this).apply {
            text = "GUARDAR MI BIENVENIDA"
            setBackground(redondo(Color.parseColor("#0F9D6A"), 40f))
            setTextColor(Color.WHITE)
        }
        btnGuardar.setOnClickListener { guardar(); Toast.makeText(this, "Guardado", Toast.LENGTH_SHORT).show() }
        layout.addView(btnGuardar)
        switchGlobal.setOnCheckedChangeListener { _, _ -> guardar(); actualizarEstado() }
        switchBienvenida.setOnCheckedChangeListener { _, _ -> guardar() }
        layout.addView(TextView(this).apply {
            text = "Respuestas para Messenger"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 40, 0, 8)
        })
        layout.addView(TextView(this).apply {
            text = "Si te escriben con esa palabra, prepara esa respuesta"
            textSize = 14f
            setTextColor(Color.GRAY)
            setPadding(0, 0, 0, 24)
        })
        contenedor = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        layout.addView(contenedor)
        val btnAgregar = Button(this).apply {
            text = "+  Agregar nueva respuesta"
            textSize = 18f
            setBackground(redondo(Color.parseColor("#0F9D6A"), 50f))
            setTextColor(Color.WHITE)
        }
        btnAgregar.setOnClickListener { mostrarDialogoNueva() }
        layout.addView(btnAgregar)
        val lista = TextView(this).apply { text = "Acá van a aparecer los borradores..."; setPadding(0, 30, 0, 0) }
        val btnVer = Button(this).apply {
            text = "VER BORRADORES"
            setBackground(redondo(Color.parseColor("#E0E0E0"), 40f))
            setTextColor(Color.BLACK)
        }
        btnVer.setOnClickListener { lista.text = Borradores.lista.joinToString("\n\n") }
        layout.addView(btnVer)
        layout.addView(lista)
        scroll.addView(layout)
        setContentView(scroll)
        cargar()
        render()
    }
}
