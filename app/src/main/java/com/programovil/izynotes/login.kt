package com.programovil.izynotes

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth

class login : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val botonRegistrar = findViewById<Button>(R.id.botonRegistro)
        botonRegistrar.setOnClickListener {
            val intent = Intent(this, registro::class.java)
            startActivity(intent)
        }

        setup()

    }

    private fun setup() {
        title = "Autenticación"

        // Referencia a los botones
        val botonRegistro: Button = findViewById(R.id.botonRegistro)
        val botonIniciar: Button = findViewById(R.id.button)


        // Manejo del inicio de sesión
        botonIniciar.setOnClickListener{
            val emailEditText = findViewById<EditText>(R.id.editTextText)
            val passwordEditText = findViewById<EditText>(R.id.editTextTextPassword)

            if (emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty()) {
                FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(
                        emailEditText.text.toString(),
                        passwordEditText.text.toString()
                    ).addOnCompleteListener {
                        if (it.isSuccessful) {
                            showInicio(it.result?.user?.email ?: "")
                        } else {
                            showAlert()
                        }
                    }
            } else {
                showAlert("Por favor, complete todos los campos.")
            }
        }
    }

    private fun showAlert(message: String = "Se ha producido un error autenticando al usuario") {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage(message)
        builder.setPositiveButton("Aceptar", null)
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun showInicio(email: String) {
        val inicioIntent = Intent(this, Inicio::class.java).apply {
            putExtra("email", email)
        }
        startActivity(inicioIntent)
    }
}