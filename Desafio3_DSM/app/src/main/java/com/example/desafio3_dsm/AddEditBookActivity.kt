package com.example.desafio3_dsm

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.desafio3_dsm.database.DatabaseHelper
import com.example.desafio3_dsm.databinding.ActivityAddEditBookBinding
import com.example.desafio3_dsm.model.Book
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

class AddEditBookActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditBookBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var auth: FirebaseAuth

    private var currentBookId: Long = -1L
    private var existingBook: Book? = null

    companion object {
        const val EXTRA_BOOK_ID = "extra_book_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditBookBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)
        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            finish()
            return
        }

        currentBookId = intent.getLongExtra(EXTRA_BOOK_ID, -1L)

        setupToolbar()
        setupSpinners()
        setupStatusListener()
        setupPageWatcher()

        if (currentBookId != -1L) {
            loadExistingBook(currentUser.uid)
        } else {
            binding.toolbar.title = "Nuevo Libro"
            binding.btnSave.text = "Guardar Libro"
            binding.btnDelete.visibility = View.GONE
            updateRatingState("Por leer")
        }

        binding.btnSave.setOnClickListener {
            validateAndSaveBook(currentUser.uid)
        }

        binding.btnDelete.setOnClickListener {
            showDeleteConfirmationDialog(currentUser.uid)
        }
    }

    private fun setupPageWatcher() {
        val watcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateLiveProgress()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        }
        binding.etTotalPages.addTextChangedListener(watcher)
        binding.etReadPages.addTextChangedListener(watcher)
    }

    private fun updateLiveProgress() {
        val total = binding.etTotalPages.text?.toString()?.toIntOrNull() ?: 0
        val read = binding.etReadPages.text?.toString()?.toIntOrNull() ?: 0

        val pct = if (total > 0) {
            ((read.toDouble() / total.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
        binding.tvLivePercentage.text = "$pct%"
        binding.pbLiveProgress.progress = pct
    }


    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupSpinners() {
        // 4 Géneros obligatorios según documento
        val genres = resources.getStringArray(R.array.book_genres)
        val genreAdapter = ArrayAdapter(this, R.layout.item_spinner_selected, genres).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown)
        }
        binding.spGenre.adapter = genreAdapter

        // Estados sugeridos
        val statuses = resources.getStringArray(R.array.book_statuses)
        val statusAdapter = ArrayAdapter(this, R.layout.item_spinner_selected, statuses).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown)
        }
        binding.spStatus.adapter = statusAdapter
    }

    private fun setupStatusListener() {
        binding.spStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedStatus = parent?.getItemAtPosition(position).toString()
                updateRatingState(selectedStatus)

                // Si selecciona Terminado y las páginas leídas están vacías o son menores, podemos sugerir el total
                if (selectedStatus == "Terminado") {
                    val totalStr = binding.etTotalPages.text?.toString()?.trim().orEmpty()
                    val readStr = binding.etReadPages.text?.toString()?.trim().orEmpty()
                    if (totalStr.isNotEmpty() && (readStr.isEmpty() || readStr == "0")) {
                        binding.etReadPages.setText(totalStr)
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateRatingState(status: String) {
        val isFinished = status.equals("Terminado", ignoreCase = true)
        binding.ratingBar.setIsIndicator(!isFinished)
        binding.ratingBar.isEnabled = isFinished
        if (isFinished) {
            binding.cardRating.alpha = 1.0f
            binding.tvRatingHelp.text = "Califica este libro de 1 a 5 estrellas (obligatorio)"
            binding.tvRatingHelp.setTextColor(getColor(R.color.primary))
            if (binding.ratingBar.rating < 1f) {
                binding.ratingBar.rating = 5f // Valor inicial sugerido para terminado
            }
        } else {
            binding.cardRating.alpha = 0.5f
            binding.tvRatingHelp.text = "Solo disponible cuando el estado sea 'Terminado'"
            binding.tvRatingHelp.setTextColor(getColor(R.color.text_hint))
            binding.ratingBar.rating = 0f
        }
    }

    private fun loadExistingBook(userId: String) {
        binding.toolbar.title = "Editar Libro"
        binding.btnSave.text = "Actualizar Libro"
        binding.btnDelete.visibility = View.VISIBLE

        existingBook = dbHelper.getBookById(currentBookId, userId)
        existingBook?.let { book ->
            binding.etTitle.setText(book.title)
            binding.etAuthor.setText(book.author)
            binding.etTotalPages.setText(book.totalPages.toString())
            binding.etReadPages.setText(book.readPages.toString())

            // Seleccionar género
            val genres = resources.getStringArray(R.array.book_genres)
            val genreIndex = genres.indexOf(book.genre)
            if (genreIndex >= 0) {
                binding.spGenre.setSelection(genreIndex)
            }

            // Seleccionar estado
            val statuses = resources.getStringArray(R.array.book_statuses)
            val statusIndex = statuses.indexOf(book.status)
            if (statusIndex >= 0) {
                binding.spStatus.setSelection(statusIndex)
            }

            updateRatingState(book.status)
            if (book.isFinished) {
                binding.ratingBar.rating = book.rating
            }
            updateLiveProgress()
        } ?: run {
            Toast.makeText(this, "Libro no encontrado", Toast.LENGTH_SHORT).show()
            finish()
        }

    }

    private fun validateAndSaveBook(userId: String) {
        // Limpiar errores previos
        binding.tilTitle.error = null
        binding.tilAuthor.error = null
        binding.tilTotalPages.error = null
        binding.tilReadPages.error = null

        val title = binding.etTitle.text?.toString()?.trim().orEmpty()
        val author = binding.etAuthor.text?.toString()?.trim().orEmpty()
        val totalPagesStr = binding.etTotalPages.text?.toString()?.trim().orEmpty()
        val readPagesStr = binding.etReadPages.text?.toString()?.trim().orEmpty()
        val genre = binding.spGenre.selectedItem?.toString().orEmpty()
        val status = binding.spStatus.selectedItem?.toString().orEmpty()
        val rating = binding.ratingBar.rating

        var isValid = true
        val errorMessages = mutableListOf<String>()

        // 1. El título es obligatorio y debe tener entre 2 y 60 caracteres
        if (title.isEmpty()) {
            val err = "El título es obligatorio"
            binding.tilTitle.error = err
            errorMessages.add(err)
            isValid = false
        } else if (title.length < 2 || title.length > 60) {
            val err = "El título debe tener entre 2 y 60 caracteres (actual: ${title.length})"
            binding.tilTitle.error = err
            errorMessages.add(err)
            isValid = false
        }

        // 2. El autor es obligatorio y debe tener entre 2 y 60 caracteres
        if (author.isEmpty()) {
            val err = "El autor es obligatorio"
            binding.tilAuthor.error = err
            errorMessages.add(err)
            isValid = false
        } else if (author.length < 2 || author.length > 60) {
            val err = "El autor debe tener entre 2 y 60 caracteres (actual: ${author.length})"
            binding.tilAuthor.error = err
            errorMessages.add(err)
            isValid = false
        }

        // 3. El total de páginas debe ser un número entero mayor que cero
        val totalPages = totalPagesStr.toIntOrNull()
        if (totalPages == null) {
            val err = "El total de páginas debe ser un número entero válido"
            binding.tilTotalPages.error = err
            errorMessages.add(err)
            isValid = false
        } else if (totalPages <= 0) {
            val err = "El total de páginas debe ser mayor que cero"
            binding.tilTotalPages.error = err
            errorMessages.add(err)
            isValid = false
        }

        // 4. Las páginas leídas deben ser un número entero entre 0 y el total de páginas
        val readPages = readPagesStr.toIntOrNull()
        if (readPages == null) {
            val err = "Las páginas leídas deben ser un número entero válido"
            binding.tilReadPages.error = err
            errorMessages.add(err)
            isValid = false
        } else if (readPages < 0) {
            val err = "Las páginas leídas no pueden ser negativas"
            binding.tilReadPages.error = err
            errorMessages.add(err)
            isValid = false
        } else if (totalPages != null && totalPages > 0 && readPages > totalPages) {
            val err = "Las páginas leídas ($readPages) no pueden superar el total de páginas ($totalPages)"
            binding.tilReadPages.error = err
            errorMessages.add(err)
            isValid = false
        }

        // 5. La calificación (de 1 a 5 estrellas) solo puede asignarse si el estado es Terminado, y en ese caso es obligatoria
        val isFinished = status.equals("Terminado", ignoreCase = true)
        if (isFinished) {
            if (rating < 1.0f || rating > 5.0f) {
                val err = "Para libros con estado 'Terminado', la calificación (1 a 5 estrellas) es obligatoria"
                errorMessages.add(err)
                isValid = false
            }
        }

        if (!isValid) {
            val summary = errorMessages.joinToString("\n• ", prefix = "Corrige los siguientes errores:\n• ")
            AlertDialog.Builder(this)
                .setTitle("Datos inválidos")
                .setMessage(summary)
                .setPositiveButton("Entendido", null)
                .show()
            return
        }

        // Determinar fecha y año de finalización
        val now = System.currentTimeMillis()
        val currentCalendar = Calendar.getInstance()
        var finishDate: Long? = null
        var finishYear: Int? = null

        if (isFinished) {
            if (existingBook != null && existingBook!!.isFinished && existingBook!!.finishDate != null) {
                finishDate = existingBook!!.finishDate
                finishYear = existingBook!!.finishYear
            } else {
                finishDate = now
                finishYear = currentCalendar.get(Calendar.YEAR)
            }
        }

        val bookToSave = Book(
            id = if (currentBookId != -1L) currentBookId else 0L,
            userId = userId,
            title = title,
            author = author,
            genre = genre,
            totalPages = totalPages!!,
            readPages = readPages!!,
            status = status,
            rating = if (isFinished) rating else 0f,
            finishDate = finishDate,
            finishYear = finishYear
        )

        if (currentBookId == -1L) {
            val newId = dbHelper.insertBook(bookToSave)
            if (newId != -1L) {
                Toast.makeText(this, "¡Libro registrado exitosamente!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Error al guardar en la base de datos", Toast.LENGTH_SHORT).show()
            }
        } else {
            val rows = dbHelper.updateBook(bookToSave)
            if (rows > 0) {
                Toast.makeText(this, "¡Libro actualizado exitosamente!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Error al actualizar el libro", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmationDialog(userId: String) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar libro")
            .setMessage("¿Estás seguro de que deseas eliminar este libro? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                val rows = dbHelper.deleteBook(currentBookId, userId)
                if (rows > 0) {
                    Toast.makeText(this, "Libro eliminado", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Error al eliminar el libro", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
