package com.example.desafio3_dsm

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.desafio3_dsm.adapter.BookAdapter
import com.example.desafio3_dsm.database.DatabaseHelper
import com.example.desafio3_dsm.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var auth: FirebaseAuth
    private lateinit var readingAdapter: BookAdapter

    private var currentYear: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        // Si por alguna razón no hay sesión activa, redirigir a Login
        if (currentUser == null) {
            goToLogin()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)
        currentYear = Calendar.getInstance().get(Calendar.YEAR)

        setupToolbar()
        setupGreeting()
        setupRecyclerView()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                showLogoutConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupGreeting() {
        val user = auth.currentUser ?: return
        val displayName = user.displayName
        val email = user.email ?: "usuario"

        val greetingName = if (!displayName.isNullOrBlank()) {
            displayName
        } else {
            email.substringBefore('@')
        }

        binding.tvUserGreeting.text = "¡Hola, $greetingName!"
        binding.tvUserEmail.text = email
        binding.tvAnnualGoalTitle.text = "Meta de lectura $currentYear"

        val initial = greetingName.firstOrNull()?.uppercaseChar()?.toString() ?: "L"
        binding.tvAvatarInitial.text = initial
    }


    private fun setupRecyclerView() {
        readingAdapter = BookAdapter(emptyList()) { book ->
            val intent = Intent(this, AddEditBookActivity::class.java).apply {
                putExtra(AddEditBookActivity.EXTRA_BOOK_ID, book.id)
            }
            startActivity(intent)
        }
        binding.rvCurrentlyReading.layoutManager = LinearLayoutManager(this)
        binding.rvCurrentlyReading.adapter = readingAdapter
    }

    private fun setupListeners() {
        // Modificar meta anual
        binding.btnEditGoal.setOnClickListener {
            showEditGoalDialog()
        }

        // Acciones rápidas
        binding.cardActionAdd.setOnClickListener {
            startActivity(Intent(this, AddEditBookActivity::class.java))
        }

        binding.cardActionLibrary.setOnClickListener {
            startActivity(Intent(this, LibraryActivity::class.java))
        }

        binding.cardActionStats.setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }
    }

    private fun loadDashboardData() {
        val user = auth.currentUser ?: return
        val userId = user.uid

        // 1. Cargar Meta Anual
        val goal = dbHelper.getAnnualGoal(userId, currentYear)
        val finishedCount = dbHelper.getFinishedBooksCountInYear(userId, currentYear)
        val remaining = maxOf(0, goal - finishedCount)

        binding.tvGoalTotal.text = goal.toString()
        binding.tvGoalFinished.text = finishedCount.toString()
        binding.tvGoalRemaining.text = remaining.toString()

        val progressPercentage = if (goal > 0) {
            ((finishedCount.toDouble() / goal.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
        binding.pbAnnualGoal.progress = progressPercentage

        if (finishedCount >= goal) {
            binding.tvGoalMessage.text = "¡Felicidades! 🎉 Has alcanzado tu meta de $goal libros en $currentYear"
        } else {
            binding.tvGoalMessage.text = "$progressPercentage% de tu meta anual alcanzada ($remaining restantes)"
        }

        // 2. Cargar libros que está leyendo actualmente
        val currentlyReading = dbHelper.getCurrentlyReadingBooks(userId)
        readingAdapter.updateBooks(currentlyReading)

        binding.tvCurrentlyReadingCount.text = "${currentlyReading.size} ${if (currentlyReading.size == 1) "libro" else "libros"}"

        if (currentlyReading.isEmpty()) {
            binding.cardEmptyReading.visibility = View.VISIBLE
            binding.rvCurrentlyReading.visibility = View.GONE
        } else {
            binding.cardEmptyReading.visibility = View.GONE
            binding.rvCurrentlyReading.visibility = View.VISIBLE
        }
    }

    private fun showEditGoalDialog() {
        val user = auth.currentUser ?: return
        val currentGoal = dbHelper.getAnnualGoal(user.uid, currentYear)

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(currentGoal.toString())
            setSelection(text.length)
            hint = "Meta de libros para $currentYear"
            setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.input_text_dark))
            setHintTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.input_hint_color))
        }

        val container = FrameLayout(this).apply {
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, (8 * resources.displayMetrics.density).toInt(), padding, 0)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle("Definir Meta Anual ($currentYear)")
            .setMessage("Ingresa la cantidad de libros que deseas leer este año (debe ser un número entero mayor que cero):")
            .setView(container)
            .setPositiveButton("Guardar") { _, _ ->
                val text = input.text.toString().trim()
                val newGoal = text.toIntOrNull()

                if (newGoal == null || newGoal <= 0) {
                    Toast.makeText(this, "La meta debe ser un número entero mayor que cero", Toast.LENGTH_LONG).show()
                } else {
                    dbHelper.setAnnualGoal(user.uid, currentYear, newGoal)
                    Toast.makeText(this, "Meta anual actualizada a $newGoal libros", Toast.LENGTH_SHORT).show()
                    loadDashboardData()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro de que deseas salir de tu cuenta?")
            .setPositiveButton("Sí, cerrar sesión") { _, _ ->
                auth.signOut()
                goToLogin()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}