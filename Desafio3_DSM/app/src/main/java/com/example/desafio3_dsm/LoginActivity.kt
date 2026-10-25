package com.example.desafio3_dsm

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.desafio3_dsm.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    companion object {
        private const val PREFS_NAME = "login_prefs"
        private const val KEY_REMEMBER_EMAIL = "key_remember_email"
        private const val KEY_SAVED_EMAIL = "key_saved_email"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Sesión persistente: si el usuario ya está autenticado, pasa directo a la pantalla principal
        if (auth.currentUser != null) {
            goToMainActivity()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadSavedEmailPreference()
        setupListeners()
    }

    private fun loadSavedEmailPreference() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val shouldRemember = prefs.getBoolean(KEY_REMEMBER_EMAIL, false)
        val savedEmail = prefs.getString(KEY_SAVED_EMAIL, "")

        if (shouldRemember && !savedEmail.isNullOrBlank()) {
            binding.etEmail.setText(savedEmail)
            binding.cbRememberMe.isChecked = true
            binding.etPassword.requestFocus()
        }
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            attemptLogin()
        }

        binding.tvGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun attemptLogin() {
        // Limpiar errores previos
        binding.tilEmail.error = null
        binding.tilPassword.error = null

        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()

        var isValid = true

        // 1. Validar correo
        if (email.isEmpty()) {
            binding.tilEmail.error = "El correo electrónico es obligatorio"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Ingresa un formato de correo válido (ej: usuario@correo.com)"
            isValid = false
        }

        // 2. Validar contraseña
        if (password.isEmpty()) {
            binding.tilPassword.error = "La contraseña es obligatoria"
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "La contraseña debe tener al menos 6 caracteres"
            isValid = false
        }

        if (!isValid) return

        showLoading(true)

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    saveEmailPreference(email)
                    Toast.makeText(this, "¡Bienvenido/a!", Toast.LENGTH_SHORT).show()
                    goToMainActivity()
                } else {
                    val exception = task.exception
                    val errorMessage = when (exception) {
                        is FirebaseAuthInvalidUserException -> "No existe una cuenta registrada con este correo"
                        is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos"
                        else -> exception?.localizedMessage ?: "Error al iniciar sesión. Intenta nuevamente."
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun saveEmailPreference(email: String) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        with(prefs.edit()) {
            if (binding.cbRememberMe.isChecked) {
                putBoolean(KEY_REMEMBER_EMAIL, true)
                putString(KEY_SAVED_EMAIL, email)
            } else {
                putBoolean(KEY_REMEMBER_EMAIL, false)
                putString(KEY_SAVED_EMAIL, "")
            }
            apply()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.tvGoToRegister.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
    }

    private fun goToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
