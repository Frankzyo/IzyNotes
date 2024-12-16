package com.programovil.izynotes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class login : AppCompatActivity() {

    private val GOOGLE_SING_IN = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val botonRegistrar = findViewById<Button>(R.id.botonRegistro)
        botonRegistrar.setOnClickListener {
            val intent = Intent(this, registro::class.java)
            startActivity(intent)
        }

        setup()
        sesion()

    }

    override fun onStart(){
        super.onStart()

        val ventana = findViewById<LinearLayout>(R.id.authLayout)
        ventana.visibility = View.VISIBLE
    }

    private fun sesion(){
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email = prefs.getString("email", null)

        if(email != null){
            val ventana = findViewById<LinearLayout>(R.id.authLayout)
            ventana.visibility = View.INVISIBLE
            showInicio(email)

        }

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

        val googleBoton= findViewById<ImageButton>(R.id.imageButton3)
        googleBoton.setOnClickListener {
            val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail().build()

            val googleClient = GoogleSignIn.getClient(this, googleConf)
            googleClient.signOut()

            startActivityForResult(googleClient.signInIntent, GOOGLE_SING_IN)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == GOOGLE_SING_IN){
            val  task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                val account = task.getResult(ApiException::class.java)

                if (account != null) {
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                    FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnCompleteListener {

                            if (it.isSuccessful) {
                                showInicio(account.email ?: "")
                            } else {
                                showAlert()
                            }
                        }

                }
            }catch (e: ApiException){
                showAlert()
            }
        }
    }
}