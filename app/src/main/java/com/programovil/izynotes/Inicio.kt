package com.programovil.izynotes

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
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
    private lateinit var searchViewNotas: SearchView
    private lateinit var notasAdapter: NotasAdapter
    private val listaNotas = mutableListOf<Nota>()
    private val listaFiltrada = mutableListOf<Nota>() // Lista para el buscador
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
        setupSearchView() // Configurar buscador
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
                R.id.nav_cerrarSesion -> {
                    val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
                    prefs.clear()
                    prefs.apply()

                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this, login::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setupCerrarSesion() {
        val botonCerrar: Button = findViewById(R.id.cerrarSesion)
        botonCerrar.setOnClickListener {
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

        notasAdapter = NotasAdapter(
            listaFiltrada, // Usamos la lista filtrada para mostrar resultados
            { notaId -> eliminarNotaDeFirebase(notaId) },
            { nota -> editarNota(nota) }
        )
        recyclerViewNotas.adapter = notasAdapter
    }

    private fun setupSearchView() {
        searchViewNotas = findViewById(R.id.searchViewNotas)

        // Forzar que el SearchView muestre el campo de búsqueda y el ícono de lupa
        searchViewNotas.isIconified = false
        searchViewNotas.setIconifiedByDefault(false) // No colapsar por defecto

        // Modificar el color del texto dentro del SearchView
        val textView = searchViewNotas.findViewById(androidx.appcompat.R.id.search_src_text) as TextView
        textView.setTextColor(android.graphics.Color.BLACK) // Color del texto
        textView.setHintTextColor(android.graphics.Color.DKGRAY) // Color del hint

        // Cambiar el ícono de la lupa si es necesario
        val searchIcon = searchViewNotas.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)
        searchIcon.setImageResource(R.drawable.ic_search) // Reemplaza por tu ícono

        // Configurar el listener para el buscador
        searchViewNotas.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filtrarNotas(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filtrarNotas(newText)
                return true
            }
        })
    }

    private fun filtrarNotas(query: String?) {
        listaFiltrada.clear()
        if (query.isNullOrBlank()) {
            listaFiltrada.addAll(listaNotas) // Mostrar todas las notas si no hay búsqueda
        } else {
            listaNotas.filter { nota ->
                nota.titulo.contains(query, ignoreCase = true)
            }.let { listaFiltrada.addAll(it) }
        }
        notasAdapter.notifyDataSetChanged()
    }

    private fun editarNota(nota: Nota) {
        val intent = Intent(this, CrearNota::class.java).apply {
            putExtra("notaId", nota.id)
            putExtra("titulo", nota.titulo)
        }
        startActivity(intent)
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

        databaseReference.orderByChild("usuarioId").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                @SuppressLint("NotifyDataSetChanged")
                override fun onDataChange(snapshot: DataSnapshot) {
                    listaNotas.clear()
                    for (notaSnapshot in snapshot.children) {
                        val nota = notaSnapshot.getValue(Nota::class.java)
                        nota?.let {
                            it.id = notaSnapshot.key ?: ""
                            listaNotas.add(it)
                        }
                    }
                    filtrarNotas("") // Inicializa la lista filtrada con todas las notas
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@Inicio, "Error al cargar notas: ${error.message}", Toast.LENGTH_SHORT).show()
                    Log.e("Firebase", "Error: ${error.message}")
                }
            })
    }
}
