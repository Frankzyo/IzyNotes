package com.programovil.izynotes

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import androidx.drawerlayout.widget.DrawerLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class Inicio : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var recyclerViewNotas: RecyclerView
    private lateinit var notasAdapter: NotasAdapter
    private val listaNotas = mutableListOf<Nota>()
    private lateinit var databaseReference: DatabaseReference
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio)

        val bundle = intent.extras
        val email = bundle?.getString("email")

        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
        prefs.putString("email", email)
        prefs.apply()

        setupMenu()
        setupCerrarSesion()
        setupFloatingActionButton()
        setupRecyclerView()
        cargarNotasDesdeFirebase()
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
                R.id.nav_perfil -> startActivity(Intent(this, Perfil::class.java))
                R.id.nav_notas -> Toast.makeText(this, "Notas seleccionadas", Toast.LENGTH_SHORT).show()
                R.id.nav_archivo -> Toast.makeText(this, "Archivo seleccionado", Toast.LENGTH_SHORT).show()
                R.id.nav_papelera -> Toast.makeText(this, "Papelera seleccionada", Toast.LENGTH_SHORT).show()
                R.id.nav_ajustes -> Toast.makeText(this, "Ajustes seleccionados", Toast.LENGTH_SHORT).show()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setupCerrarSesion() {
        val botonCerrar: Button = findViewById(R.id.cerrarSesion)
        botonCerrar.setOnClickListener {

            //Borado de datos
            val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
            prefs.clear()
            prefs.apply()

            FirebaseAuth.getInstance().signOut()
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, login::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            })
            finish()
        }
    }

    private fun setupFloatingActionButton() {
        val fabAgregarNota: FloatingActionButton = findViewById(R.id.fabAgregarNota)
        fabAgregarNota.setOnClickListener {
            startActivity(Intent(this, CrearNota::class.java))
        }
    }

    private fun setupRecyclerView() {
        recyclerViewNotas = findViewById(R.id.recyclerViewNotas)
        recyclerViewNotas.layoutManager = GridLayoutManager(this, 2)

        notasAdapter = NotasAdapter(listaNotas) { notaId ->
            eliminarNotaDeFirebase(notaId)
        }
        recyclerViewNotas.adapter = notasAdapter
    }
    private fun eliminarNotaDeFirebase(notaId: String) {
        databaseReference.child(notaId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Nota eliminada correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al eliminar nota: ${exception.message}", Toast.LENGTH_SHORT).show()
                Log.e("Firebase", "Error: ${exception.message}")
            }
    }
    private fun cargarNotasDesdeFirebase() {
        val usuario = FirebaseAuth.getInstance().currentUser
        if (usuario == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }
        val userId = usuario.uid
        databaseReference = FirebaseDatabase.getInstance().getReference("notas")

        databaseReference.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                listaNotas.clear()
                for (notaSnapshot in snapshot.children) {
                    val nota = notaSnapshot.getValue(Nota::class.java)
                    nota?.let {
                        it.id = notaSnapshot.key ?: "" // Asigna el ID de la nota desde Firebase
                        listaNotas.add(it)
                    }
                }
                notasAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Inicio, "Error al cargar notas: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("Firebase", "Error: ${error.message}")
            }
        })
    }

}
