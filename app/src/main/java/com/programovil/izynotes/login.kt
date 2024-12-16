package com.programovil.izynotes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase

class login : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        setupGoogleSignIn()
        setupAuthButtons()
        checkSession()
    }

    private fun setupGoogleSignIn() {
        val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, googleConf)
    }

    private fun setupAuthButtons() {
        val botonRegistrar = findViewById<Button>(R.id.botonRegistro)
        botonRegistrar.setOnClickListener {
            startActivity(Intent(this, registro::class.java))
        }

        val botonIniciar = findViewById<Button>(R.id.button)
        botonIniciar.setOnClickListener {
            val email = findViewById<EditText>(R.id.editTextText).text.toString()
            val password = findViewById<EditText>(R.id.editTextTextPassword).text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            showInicio(email)
                        } else {
                            showAlert()
                        }
                    }
            } else {
                showAlert("Por favor, complete todos los campos.")
            }
        }

        val googleBoton = findViewById<ImageButton>(R.id.imageButton3)
        val googleSignInLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleGoogleSignInResult(task)
            }

        googleBoton.setOnClickListener {
            googleSignInClient.signOut() // Opcional: Limpiar sesión previa.
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun checkSession() {
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email = prefs.getString("email", null)

        if (email != null) {
            findViewById<LinearLayout>(R.id.authLayout).visibility = View.INVISIBLE
            showInicio(email)
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
                            showAlert()
                        }
                    }
            }
        } catch (e: ApiException) {
            showAlert()
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
                        Toast.makeText(this, "Error al guardar datos: ${it.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }


    private fun showInicio(email: String) {
        val inicioIntent = Intent(this, Inicio::class.java).apply {
            putExtra("email", email)
        }
        startActivity(inicioIntent)
    }

    private fun showAlert(message: String = "Se ha producido un error autenticando al usuario") {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("Aceptar", null)
            .create()
            .show()
    }
}
