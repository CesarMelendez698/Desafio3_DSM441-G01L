package com.example.desafio3_dsm

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.desafio3_dsm.adapter.BookAdapter
import com.example.desafio3_dsm.database.DatabaseHelper
import com.example.desafio3_dsm.databinding.ActivityLibraryBinding
import com.example.desafio3_dsm.model.Book
import com.google.firebase.auth.FirebaseAuth

class LibraryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLibraryBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: BookAdapter

    private var allBooks: List<Book> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLibraryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)
        auth = FirebaseAuth.getInstance()

        setupToolbar()
        setupRecyclerView()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadBooks()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = BookAdapter(emptyList()) { book ->
            val intent = Intent(this, AddEditBookActivity::class.java).apply {
                putExtra(AddEditBookActivity.EXTRA_BOOK_ID, book.id)
            }
            startActivity(intent)
        }
        binding.rvBooks.layoutManager = LinearLayoutManager(this)
        binding.rvBooks.adapter = adapter
    }

    private fun setupListeners() {
        binding.fabAddBook.setOnClickListener {
            startActivity(Intent(this, AddEditBookActivity::class.java))
        }

        binding.btnEmptyAdd.setOnClickListener {
            startActivity(Intent(this, AddEditBookActivity::class.java))
        }

        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            filterBooks()
        }

        binding.etSearchBook.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterBooks()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    private fun loadBooks() {
        val user = auth.currentUser
        if (user == null) {
            finish()
            return
        }

        allBooks = dbHelper.getAllBooks(user.uid)
        filterBooks()
    }

    private fun filterBooks() {
        val checkedChipId = binding.chipGroupFilters.checkedChipId
        val searchQuery = binding.etSearchBook.text?.toString()?.trim()?.lowercase().orEmpty()

        var filtered = when (checkedChipId) {
            R.id.chipReading -> allBooks.filter { it.status.equals("Leyendo", ignoreCase = true) }
            R.id.chipFinished -> allBooks.filter { it.status.equals("Terminado", ignoreCase = true) }
            R.id.chipToRead -> allBooks.filter { it.status.equals("Por leer", ignoreCase = true) }
            else -> allBooks
        }

        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.title.lowercase().contains(searchQuery) || it.author.lowercase().contains(searchQuery)
            }
        }

        adapter.updateBooks(filtered)

        if (filtered.isEmpty()) {
            binding.layoutEmpty.visibility = View.VISIBLE
            binding.rvBooks.visibility = View.GONE
            binding.tvEmptyMessage.text = if (allBooks.isEmpty()) {
                "Agrega un libro para comenzar a llevar el registro de tus lecturas."
            } else if (searchQuery.isNotEmpty()) {
                "No se encontraron libros que coincidan con \"$searchQuery\"."
            } else {
                "No hay libros con el filtro seleccionado."
            }
        } else {
            binding.layoutEmpty.visibility = View.GONE
            binding.rvBooks.visibility = View.VISIBLE
        }
    }
}
