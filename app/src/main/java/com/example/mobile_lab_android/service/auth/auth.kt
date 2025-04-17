package com.example.mobile_lab_android

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Auth : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _isAuthenticated = MutableLiveData<Boolean>(false)
    val isAuthenticated: LiveData<Boolean> get() = _isAuthenticated

    private val _userId = MutableLiveData<String?>()
    val userId: LiveData<String?> get() = _userId

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    init {
        checkAuthenticationStatus()
    }

    private fun checkAuthenticationStatus() {
        auth.currentUser?.let {
            _userId.value = it.uid
            _isAuthenticated.value = true
        }
    }

    fun signIn(email: String, password: String, completion: (Boolean) -> Unit) {
        _errorMessage.value = ""

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.let {
                        _userId.value = it.uid
                        _isAuthenticated.value = true
                        completion(true)
                    } ?: run {
                        _errorMessage.value = "Не удалось войти. Проверьте данные."
                        completion(false)
                    }
                } else {
                    _errorMessage.value = "Ошибка: ${task.exception?.localizedMessage}"
                    completion(false)
                }
            }
    }

    fun signUp(name: String, email: String, password: String, confirmPassword: String, completion: (Boolean) -> Unit) {
        _errorMessage.value = ""

        if (!validateFields(name, email, password, confirmPassword)) {
            completion(false)
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    auth.currentUser?.let { user ->
                        saveUserData(user.uid, name, email) { success ->
                            if (success) {
                                _isAuthenticated.value = true
                                _userId.value = user.uid
                                completion(true)
                            } else {
                                completion(false)
                            }
                        }
                    } ?: run {
                        _errorMessage.value = "Не удалось создать аккаунт."
                        completion(false)
                    }
                } else {
                    _errorMessage.value = "Ошибка: ${task.exception?.localizedMessage}"
                    completion(false)
                }
            }
    }

    fun signOut() {
        auth.signOut()
        resetAuthenticationState()
    }

    private fun validateFields(name: String, email: String, password: String, confirmPassword: String): Boolean {
        return when {
            password != confirmPassword -> {
                _errorMessage.value = "Пароли не совпадают"
                false
            }
            email.isEmpty() || password.isEmpty() || name.isEmpty() -> {
                _errorMessage.value = "Все поля обязательны для заполнения"
                false
            }
            else -> true
        }
    }

    private fun saveUserData(userId: String, name: String, email: String, completion: (Boolean) -> Unit) {
        val userData = mapOf(
            "name" to name,
            "email" to email,
            "dateOfBirth" to "-",
            "phoneNumber" to "-",
            "address" to "-",
            "bio" to "-",
            "occupation" to "-",
            "website" to "-",
            "socialMedia" to "-",
            "additionalInfo" to "-"
        )

        db.collection("users").document(userId).set(userData)
            .addOnSuccessListener {
                completion(true)
            }
            .addOnFailureListener { e ->
                _errorMessage.value = "Ошибка сохранения данных: ${e.localizedMessage}"
                completion(false)
            }
    }

    private fun resetAuthenticationState() {
        _isAuthenticated.value = false
        _userId.value = null
    }
}