package com.smartwarehouse.mobile.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.smartwarehouse.mobile.data.model.response.LoginResponse
import com.smartwarehouse.mobile.data.repository.AuthRepository
import com.smartwarehouse.mobile.utils.NetworkResult
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(application)

    private val _loginResult = MutableLiveData<NetworkResult<LoginResponse>>()
    val loginResult: LiveData<NetworkResult<LoginResponse>> = _loginResult

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginResult.value = NetworkResult.Error("Email y contraseña son obligatorios")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _loginResult.value = NetworkResult.Error("Email no válido")
            return
        }

        _loginResult.value = NetworkResult.Loading()

        viewModelScope.launch {
            _loginResult.value = authRepository.login(email, password)
        }
    }

    fun isLoggedIn(): Boolean = authRepository.isLoggedIn()
}