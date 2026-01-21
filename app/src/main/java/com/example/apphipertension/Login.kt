package com.example.apphipertension

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class Login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val createAccountButton = findViewById<Button>(R.id.createAccountButton)
        val goToRegisterText = findViewById<TextView>(R.id.goToRegisterText)
        val forgotPasswordText = findViewById<TextView>(R.id.forgotPasswordText)
        val progressBarForgotPassword = findViewById<ProgressBar>(R.id.progressBarForgotPassword)

        // Iniciar sesión
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            // Guardamos el UID en SharedPreferences
                            val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                            prefs.edit().putString("uid", user.uid).apply()
                        }
                        // Login exitoso, ir a MainActivity
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Error: ${task.exception?.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }

        }

        // Crear cuenta (lleva a Register)
        createAccountButton.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }

        // Link "¿No tienes una cuenta?" (lleva a Register)
        goToRegisterText.setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Olvidé mi contraseña
        forgotPasswordText.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Ingresa tu correo en el campo para recuperar la contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            progressBarForgotPassword.visibility = ProgressBar.VISIBLE
            forgotPasswordText.isEnabled = false

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    progressBarForgotPassword.visibility = ProgressBar.GONE
                    forgotPasswordText.isEnabled = true
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Correo de recuperación enviado a $email", Toast.LENGTH_LONG).show() // Sé más específico
                    } else {
                        var errorMessage = "No se pudo enviar el correo. Verifica el correo ingresado."
                        if (task.exception != null) {
                            errorMessage += " (${task.exception?.localizedMessage})"
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
    // Si el usuario ya está logueado, ir directo a MainActivity
    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}