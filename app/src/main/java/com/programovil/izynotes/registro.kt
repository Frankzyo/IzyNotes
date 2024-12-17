package com.programovil.izynotes

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase

class registro : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        // Configurar Google SignIn
        setupGoogleSignIn()

        // Botón para volver a la pantalla de login
        val volver = findViewById<ImageButton>(R.id.imageButton5)
        volver.setOnClickListener {
            val intent = Intent(this, login::class.java)
            startActivity(intent)
        }

        // Configuración del botón para registrar al usuario
        setupRegistro()

        // Configuración del botón "Continuar con Google"
        setupGoogleButton()
    }

    private fun setupGoogleSignIn() {
        val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, googleConf)
    }

    private fun setupGoogleButton() {
        val googleButton = findViewById<ImageButton>(R.id.imageButton7)

        val googleSignInLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleGoogleSignInResult(task)
            }

        googleButton.setOnClickListener {
            googleSignInClient.signOut() // Opcional: Limpiar sesión previa.
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun handleGoogleSignInResult(task: com.google.android.gms.tasks.Task<com.google.android.gms.auth.api.signin.GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            val nombre = account.displayName ?: "Usuario Google"
                            val email = account.email ?: "email_no_disponible@google.com"
                            guardarUsuarioGoogle(email, nombre)
                            showInicio(email)
                        } else {
                            Toast.makeText(this, "Error al iniciar sesión con Google", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarUsuarioGoogle(email: String, nombre: String?) {
        val usuarioId = FirebaseAuth.getInstance().currentUser?.uid
        Log.d("Debug", "User ID: $usuarioId")
        val database = FirebaseDatabase.getInstance().getReference("usuarios")

        val usuario = mapOf(
            "nombre" to (nombre ?: "Usuario Google"),
            "email" to email
        )

        if (usuarioId != null) {
            database.child(usuarioId).setValue(usuario)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        Toast.makeText(this, "Datos guardados exitosamente", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error al guardar datos", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun showInicio(email: String) {
        val inicioIntent = Intent(this, Inicio::class.java).apply {
            putExtra("email", email)
        }
        startActivity(inicioIntent)
        finish()
    }

    private fun setupRegistro() {
        val botonCrearCuenta = findViewById<Button>(R.id.button2)

        botonCrearCuenta.setOnClickListener {
            val nombre = findViewById<EditText>(R.id.editTextText2).text.toString()
            val email = findViewById<EditText>(R.id.editTextText3).text.toString()
            val password = findViewById<EditText>(R.id.editTextTextPassword2).text.toString()

            if (nombre.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
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
