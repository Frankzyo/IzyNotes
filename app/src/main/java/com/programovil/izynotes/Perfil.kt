package com.programovil.izynotes

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class Perfil : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var auth: FirebaseAuth
    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference
    private lateinit var imagePerfil: ImageView
    private var selectedImageUri: Uri? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        auth = FirebaseAuth.getInstance()

        setupMenu()

        imagePerfil = findViewById(R.id.imagePerfil)
        val editTextNombre = findViewById<EditText>(R.id.editTextNombre)
        val editTextCorreo = findViewById<EditText>(R.id.editTextCorreo)
        val editTextContrasenaAntigua = findViewById<EditText>(R.id.editTextContrasenaAntigua)
        val editTextContrasenaNueva = findViewById<EditText>(R.id.editTextContrasena)
        val botonGuardar = findViewById<Button>(R.id.botonGuardar)

        cargarDatos(editTextNombre, editTextCorreo)

        imagePerfil.setOnClickListener {
            seleccionarImagen()
        }

        botonGuardar.setOnClickListener {
            guardarCambios(
                editTextNombre,
                editTextCorreo,
                editTextContrasenaAntigua,
                editTextContrasenaNueva
            )
        }
    }

    private fun setupMenu() {
        drawerLayout = findViewById(R.id.drawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, findViewById(R.id.toolbar),
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_perfil -> Toast.makeText(this, "Ya estás en Perfil", Toast.LENGTH_SHORT).show()
                R.id.nav_notas -> {
                    val currentActivity = this::class.java.simpleName
                    if (currentActivity != "Inicio") {
                        val intent = Intent(this, Inicio::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this, "Ya estás en Inicio", Toast.LENGTH_SHORT).show()
                    }
                }
                R.id.nav_archivo -> Toast.makeText(this, "Archivo seleccionado", Toast.LENGTH_SHORT).show()
                R.id.nav_papelera -> Toast.makeText(this, "Papelera seleccionada", Toast.LENGTH_SHORT).show()
                R.id.nav_ajustes -> Toast.makeText(this, "Ajustes seleccionados", Toast.LENGTH_SHORT).show()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun cargarDatos(editTextNombre: EditText, editTextCorreo: EditText) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            editTextCorreo.setText(currentUser.email)

            database.child("usuarios").child(currentUser.uid).get().addOnSuccessListener { snapshot ->
                val nombre = snapshot.child("nombre").value.toString()
                val imageUrl = snapshot.child("imagenUrl").value.toString()
                editTextNombre.setText(nombre)

                if (imageUrl.isNotEmpty()) {
                    Glide.with(this).load(imageUrl).into(imagePerfil)
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Error al cargar los datos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun seleccionarImagen() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(Intent.createChooser(intent, "Seleccionar Imagen"), PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data
            imagePerfil.setImageURI(selectedImageUri)
        }
    }

    private fun guardarCambios(
        editTextNombre: EditText,
        editTextCorreo: EditText,
        editTextContrasenaAntigua: EditText,
        editTextContrasenaNueva: EditText
    ) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val nombre = editTextNombre.text.toString()
            val correo = editTextCorreo.text.toString()
            val contrasenaAntigua = editTextContrasenaAntigua.text.toString()
            val contrasenaNueva = editTextContrasenaNueva.text.toString()

            if (nombre.isEmpty() || correo.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos obligatorios", Toast.LENGTH_SHORT).show()
                return
            }

            if (contrasenaAntigua.isNotEmpty() && contrasenaNueva.isNotEmpty()) {
                currentUser.reauthenticate(
                    EmailAuthProvider.getCredential(currentUser.email!!, contrasenaAntigua)
                ).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        currentUser.updatePassword(contrasenaNueva)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Contraseña actualizada", Toast.LENGTH_SHORT).show()
                            }.addOnFailureListener {
                                Toast.makeText(this, "Error al actualizar contraseña", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "La contraseña actual es incorrecta", Toast.LENGTH_SHORT).show()
                    }
                }
            }


            database.child("usuarios").child(currentUser.uid).child("nombre").setValue(nombre)

            selectedImageUri?.let { uri ->
                val filePath = storage.child("usuarios").child(currentUser.uid).child("perfil.jpg")
                filePath.putFile(uri).addOnSuccessListener {
                    filePath.downloadUrl.addOnSuccessListener { downloadUri ->
                        database.child("usuarios").child(currentUser.uid).child("imagenUrl")
                            .setValue(downloadUri.toString())
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
                }
            }

            currentUser.updateEmail(correo).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Cambios guardados correctamente", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Error al guardar los cambios", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
