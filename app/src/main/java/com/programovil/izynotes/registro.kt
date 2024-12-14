package com.programovil.izynotes

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class registro : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        // Botón para volver a la pantalla de login
        val volver = findViewById<ImageButton>(R.id.imageButton5)
        volver.setOnClickListener {
            val intent = Intent(this, login::class.java)
            startActivity(intent)
        }

        // Configuración del botón para registrar al usuario
        setup()
    }

    private fun setup() {
        val botonCrearCuenta = findViewById<Button>(R.id.button2)

        botonCrearCuenta.setOnClickListener {
            val nombre = findViewById<EditText>(R.id.editTextText2).text.toString()
            val email = findViewById<EditText>(R.id.editTextText3).text.toString()
            val password = findViewById<EditText>(R.id.editTextTextPassword2).text.toString()

            if (nombre.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                // Registrar al usuario en Firebase Authentication
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Guardar datos adicionales en Firebase Realtime Database
                            val usuarioId = FirebaseAuth.getInstance().currentUser?.uid
                            val database = FirebaseDatabase.getInstance().getReference("usuarios")

                            val usuario = mapOf(
                                "nombre" to nombre,
                                "email" to email
                            )

                            if (usuarioId != null) {
                                database.child(usuarioId).setValue(usuario)
                                    .addOnCompleteListener {
                                        if (it.isSuccessful) {
                                            // Redirigir a la ventana de login
                                            val intent = Intent(this, login::class.java)
                                            startActivity(intent)
                                            finish()
                                        } else {
                                            Toast.makeText(this, "Error al guardar los datos", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }
                        } else {
                            Toast.makeText(this, "Error al registrar: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
