package com.programovil.izynotes

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class CrearNota : AppCompatActivity() {

    private lateinit var contenedorNota: FrameLayout
    private lateinit var drawerLayout: DrawerLayout
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioFile: File? = null
    private lateinit var imageUri: Uri

    private val databaseReference = FirebaseDatabase.getInstance().reference
    private val storageReference = FirebaseStorage.getInstance().reference

    private val getImageFromGallery =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { agregarImagen(it) }
        }

    private val getImageFromCamera =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) agregarImagen(imageUri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_nota)

        contenedorNota = findViewById(R.id.contenedorNota)
        setupMenu()

        val notaId = intent.getStringExtra("notaId")
        val tituloNota = intent.getStringExtra("titulo")

        // Si recibimos una notaId, cargamos la nota existente
        if (notaId != null) {
            cargarNotaExistente(notaId, tituloNota)
        }

        findViewById<ImageButton>(R.id.btnAgregarImagen).setOnClickListener { abrirOpcionesImagen() }
        findViewById<ImageButton>(R.id.btnAgregarAudio).setOnClickListener { grabarAudio() }
        findViewById<ImageButton>(R.id.btnAgregarTexto).setOnClickListener { agregarTexto() }
        findViewById<ImageButton>(R.id.btnGuardarNota).setOnClickListener {
            val titulo = findViewById<EditText>(R.id.editTextTituloNota).text.toString().trim()
            if (titulo.isNotEmpty()) {
                if (notaId != null) {
                    actualizarNotaEnFirebase(notaId, titulo)
                } else {
                    guardarNotaEnFirebase(titulo)
                }
            } else {
                Toast.makeText(this, "El título no puede estar vacío", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun cargarNotaExistente(notaId: String, tituloNota: String?) {
        val tituloEditText = findViewById<EditText>(R.id.editTextTituloNota)
        tituloEditText.setText(tituloNota)

        val notaRef = FirebaseDatabase.getInstance().getReference("notas").child(notaId)

        notaRef.get().addOnSuccessListener { snapshot ->
            val elementos = snapshot.child("elementos").value as? List<Map<String, Any>>
            if (elementos == null) {
                Log.e("Firebase", "No hay elementos en la nota")
                return@addOnSuccessListener
            }

            elementos.forEach { elemento ->
                val tipo = elemento["tipo"] as? String ?: return@forEach
                val posicion = elemento["posicion"] as? Map<*, *>
                val x = (posicion?.get("x") as? Double)?.toFloat() ?: 0f
                val y = (posicion?.get("y") as? Double)?.toFloat() ?: 0f

                when (tipo) {
                    "texto" -> {
                        val texto = elemento["contenido"] as? String ?: ""
                        val editText = EditText(this).apply {
                            setText(texto)
                            setTextColor(Color.BLACK)
                            setHintTextColor(Color.BLACK)
                            setBackgroundColor(Color.TRANSPARENT)
                            layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.WRAP_CONTENT,
                                FrameLayout.LayoutParams.WRAP_CONTENT
                            )
                            setOnTouchListener(MovableTouchListener(this))
                        }
                        contenedorNota.addView(editText)

                        // Ajustar la posición después de añadir la vista
                        editText.post {
                            editText.x = x
                            editText.y = y
                        }
                    }
                    "imagen" -> {
                        val url = elemento["url"] as? String ?: return@forEach
                        val imageView = ImageView(this).apply {
                            Glide.with(this@CrearNota).load(url).into(this)
                            layoutParams = FrameLayout.LayoutParams(300, 300)
                            setOnTouchListener(MovableTouchListener(this))
                        }
                        contenedorNota.addView(imageView)

                        // Ajustar la posición
                        imageView.post {
                            imageView.x = x
                            imageView.y = y
                        }
                    }
                    "audio" -> {
                        val url = elemento["url"] as? String ?: return@forEach
                        val button = Button(this).apply {
                            text = "▶ Reproducir Audio"
                            layoutParams = FrameLayout.LayoutParams(300, 100)
                            setOnTouchListener(MovableTouchListener(this))
                            setOnClickListener {
                                mediaPlayer?.release()
                                mediaPlayer = MediaPlayer().apply {
                                    setDataSource(url)
                                    prepare()
                                    start()
                                }
                            }
                        }
                        contenedorNota.addView(button)

                        // Ajustar la posición
                        button.post {
                            button.x = x
                            button.y = y
                        }
                    }
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al cargar la nota", Toast.LENGTH_SHORT).show()
        }
    }

    private fun actualizarNotaEnFirebase(notaId: String, titulo: String) {
        val elementos = mutableListOf<Map<String, Any>>()
        var totalSubidasPendientes = 0
        var subidasCompletadas = 0

        fun verificarSubidasCompletas() {
            if (subidasCompletadas == totalSubidasPendientes) {
                val actualizacion = mapOf(
                    "titulo" to titulo,
                    "elementos" to elementos
                )
                databaseReference.child("notas").child(notaId).updateChildren(actualizacion)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Nota actualizada correctamente", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al actualizar la nota", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        for (i in 0 until contenedorNota.childCount) {
            val view = contenedorNota.getChildAt(i)
            when (view) {
                is EditText -> {
                    val elemento = mapOf(
                        "tipo" to "texto",
                        "contenido" to view.text.toString(),
                        "posicion" to mapOf("x" to view.x, "y" to view.y)
                    )
                    elementos.add(elemento)
                }
                is ImageView, is Button -> {
                    val uri = view.tag as? Uri
                    if (uri != null) {
                        totalSubidasPendientes++
                        subirArchivoAFirebase(uri) { url ->
                            val tipo = if (view is ImageView) "imagen" else "audio"
                            val elemento = mapOf(
                                "tipo" to tipo,
                                "url" to url,
                                "posicion" to mapOf("x" to view.x, "y" to view.y)
                            )
                            elementos.add(elemento)
                            subidasCompletadas++
                            verificarSubidasCompletas()
                        }
                    }
                }
            }
        }

        if (totalSubidasPendientes == 0) {
            verificarSubidasCompletas()
        }
    }

    private fun setupMenu() {
        drawerLayout = findViewById(R.id.drawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, findViewById(R.id.toolbar),
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener {
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun abrirOpcionesImagen() {
        val opciones = arrayOf("Galería", "Cámara")
        AlertDialog.Builder(this)
            .setTitle("Seleccionar Imagen")
            .setItems(opciones) { _, which ->
                if (which == 0) getImageFromGallery.launch("image/*")
                else abrirCamara()
            }.show()
    }

    private fun abrirCamara() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile = File.createTempFile("IMG_${System.currentTimeMillis()}", ".jpg", cacheDir)
        imageUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        getImageFromCamera.launch(intent)
    }

    private fun agregarImagen(uri: Uri) {
        val imageView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(300, 300)
            setImageURI(uri)
            setOnTouchListener(MovableTouchListener(this))
        }
        contenedorNota.addView(imageView)
    }

    private fun grabarAudio() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            return
        }
        audioFile = File.createTempFile("AUDIO_${System.currentTimeMillis()}", ".3gp", cacheDir)
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(audioFile!!.absolutePath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            prepare()
            start()
        }

        val stopButton = Button(this).apply {
            text = "Detener"
            setOnClickListener {
                detenerGrabacion()
                contenedorNota.removeView(this)
            }
        }
        contenedorNota.addView(stopButton)
    }

    private fun detenerGrabacion() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        agregarBotonAudio(Uri.fromFile(audioFile))
    }

    private fun agregarBotonAudio(uri: Uri) {
        val button = Button(this).apply {
            text = "▶ Reproducir Audio"
            layoutParams = FrameLayout.LayoutParams(300, 100)
            setOnClickListener {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer.create(this@CrearNota, uri).apply { start() }
            }
            setOnTouchListener(MovableTouchListener(this))
        }
        contenedorNota.addView(button)
    }


    private fun agregarTexto() {
        val editText = EditText(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            hint = "Escribe aquí"
            setHintTextColor(Color.BLACK) // Color negro para el hint
            setTextColor(Color.BLACK)     // Color negro para el texto
            setBackgroundColor(Color.TRANSPARENT) // Fondo transparente
            setOnTouchListener(MovableTouchListener(this))
        }
        contenedorNota.addView(editText)
    }

    private fun guardarNotaEnFirebase(titulo: String) {
        val usuario = FirebaseAuth.getInstance().currentUser
        if (usuario == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        val elementos = mutableListOf<Map<String, Any>>()
        var totalSubidasPendientes = 0
        var subidasCompletadas = 0

        // Función interna para verificar si todas las subidas finalizaron
        fun verificarSubidasCompletas() {
            if (subidasCompletadas == totalSubidasPendientes) {
                subirNotaCompleta(titulo, elementos, usuario.uid) // Pasa el UID del usuario
            }
        }

        for (i in 0 until contenedorNota.childCount) {
            val view = contenedorNota.getChildAt(i)
            when (view) {
                is EditText -> { // Guardar texto
                    val elemento = mapOf(
                        "tipo" to "texto",
                        "contenido" to view.text.toString(),
                        "color" to "#000000", // Asume color por defecto
                        "posicion" to mapOf("x" to view.x, "y" to view.y)
                    )
                    elementos.add(elemento)
                }

                is ImageView, is Button -> { // Subir imágenes y audios
                    val uri = view.tag as? Uri
                    if (uri != null) {
                        totalSubidasPendientes++
                        subirArchivoAFirebase(uri) { url ->
                            val tipo = if (view is ImageView) "imagen" else "audio"
                            val elemento = mapOf(
                                "tipo" to tipo,
                                "url" to url,
                                "posicion" to mapOf("x" to view.x, "y" to view.y)
                            )
                            elementos.add(elemento)
                            subidasCompletadas++
                            verificarSubidasCompletas()
                        }
                    }
                }
            }
        }

        // Si no hay archivos por subir, guardar la nota directamente
        if (totalSubidasPendientes == 0) {
            subirNotaCompleta(titulo, elementos, usuario.uid)
        }
    }

    private fun subirArchivoAFirebase(uri: Uri, onSuccess: (String) -> Unit) {
        val fileRef = storageReference.child("uploads/${System.currentTimeMillis()}")
        fileRef.putFile(uri)
            .addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { url ->
                    Log.d("FirebaseStorage", "Archivo subido correctamente: $url")
                    onSuccess(url.toString())
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseStorage", "Error al subir archivo: ${exception.message}")
                Toast.makeText(this, "Error al subir archivo: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun subirNotaCompleta(titulo: String, elementos: List<Map<String, Any>>, usuarioId: String) {
        val nuevaNotaRef = databaseReference.child("notas").push() // Referencia con clave única
        val nota = mapOf(
            "id" to nuevaNotaRef.key, // Guardar la clave generada
            "titulo" to titulo,
            "usuarioId" to usuarioId,
            "elementos" to elementos
        )

        nuevaNotaRef.setValue(nota)
            .addOnSuccessListener {
                Toast.makeText(this, "Nota guardada exitosamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar la nota", Toast.LENGTH_SHORT).show()
            }
    }

    inner class MovableTouchListener(private val view: View) : View.OnTouchListener {
        private var dX = 0f
        private var dY = 0f
        private var isLongPress = false
        private val longPressTimeout = 500L
        private var btnEliminar: ImageButton? = null

        private val longPressRunnable = Runnable {
            isLongPress = true
            if (view is EditText) {
                mostrarMenuOpciones(view) // Muestra menú con opción de eliminar
            } else {
                mostrarBotonEliminar(view) // Muestra el botón para otros elementos
            }
        }

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = v.x - event.rawX
                    dY = v.y - event.rawY
                    isLongPress = false
                    v.postDelayed(longPressRunnable, longPressTimeout)
                }
                MotionEvent.ACTION_MOVE -> {
                    v.removeCallbacks(longPressRunnable)
                    if (!isLongPress) {
                        v.x = event.rawX + dX
                        v.y = event.rawY + dY
                        actualizarBotonEliminar(v)
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.removeCallbacks(longPressRunnable)
                    if (!isLongPress) ocultarBotonEliminar()
                }
            }
            return true
        }

        private fun mostrarBotonEliminar(v: View) {
            if (btnEliminar == null) {
                btnEliminar = ImageButton(this@CrearNota).apply {
                    setImageResource(android.R.drawable.ic_menu_delete)
                    setBackgroundColor(Color.TRANSPARENT)
                    layoutParams = FrameLayout.LayoutParams(100, 100)
                    setOnClickListener {
                        contenedorNota.removeView(v)
                        ocultarBotonEliminar()
                    }
                }
                contenedorNota.addView(btnEliminar)
            }
            actualizarBotonEliminar(v)
        }

        private fun actualizarBotonEliminar(v: View) {
            btnEliminar?.x = v.x - 50
            btnEliminar?.y = v.y - 50
        }

        private fun ocultarBotonEliminar() {
            btnEliminar?.let {
                contenedorNota.removeView(it)
                btnEliminar = null
            }
        }

        private fun mostrarMenuOpciones(editText: EditText) {
            val opciones = arrayOf("Color", "Tamaño", "Eliminar")
            AlertDialog.Builder(this@CrearNota)
                .setTitle("Opciones de Texto")
                .setItems(opciones) { _, which ->
                    when (opciones[which]) {
                        "Color" -> mostrarPaletaColores(editText)
                        "Tamaño" -> mostrarTamanosTexto(editText)
                        "Eliminar" -> eliminarElemento(editText)
                    }
                }.show()
        }

        private fun mostrarPaletaColores(editText: EditText) {
            val colores = mapOf(
                "Rojo" to Color.RED,
                "Verde" to Color.GREEN,
                "Azul" to Color.BLUE,
                "Negro" to Color.BLACK
            )
            val nombresColores = colores.keys.toTypedArray()

            AlertDialog.Builder(this@CrearNota)
                .setTitle("Seleccionar Color")
                .setItems(nombresColores) { _, which ->
                    editText.setTextColor(colores[nombresColores[which]] ?: Color.BLACK)
                }.show()
        }

        private fun mostrarTamanosTexto(editText: EditText) {
            val tamanos = arrayOf("14sp", "18sp", "22sp", "26sp")

            AlertDialog.Builder(this@CrearNota)
                .setTitle("Seleccionar Tamaño")
                .setItems(tamanos) { _, which ->
                    editText.textSize = tamanos[which].replace("sp", "").toFloat()
                }.show()
        }

        private fun eliminarElemento(view: View) {
            contenedorNota.removeView(view) // Elimina el elemento del contenedor
        }
    }

}
