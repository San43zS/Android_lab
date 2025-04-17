package com.example.mobile_lab_android

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.mobile_lab_android.databinding.ActivitySignUpBinding

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    private val authViewModel: Auth by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        binding.btnCreateAccount.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                binding.tvErrorMessage.text = "Пожалуйста, заполните все поля"
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                binding.tvErrorMessage.text = "Пароли не совпадают"
                return@setOnClickListener
            }

            handleSignUp(name, email, password, confirmPassword)
        }

        authViewModel.errorMessage.observe(this) { errorMessage ->
            binding.tvErrorMessage.text = errorMessage
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun handleSignUp(name: String, email: String, password: String, confirmPassword: String) {
        authViewModel.signUp(name, email, password, confirmPassword) { success ->
            if (success) {
                Toast.makeText(this, "Аккаунт создан: $name", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
