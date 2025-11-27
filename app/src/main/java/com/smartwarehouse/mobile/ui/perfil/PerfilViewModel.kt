package com.smartwarehouse.mobile.ui.perfil

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.smartwarehouse.mobile.data.repository.AuthRepository

class PerfilViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)

    private val _userId = MutableLiveData<Int>()
    val userId: LiveData<Int> = _userId

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _userEmail = MutableLiveData<String>()
    val userEmail: LiveData<String> = _userEmail

    private val _userRole = MutableLiveData<String>()
    val userRole: LiveData<String> = _userRole

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        _userId.value = authRepository.getUserId()
        _userName.value = authRepository.getUserName() ?: "Sin nombre"
        _userEmail.value = authRepository.getUserEmail() ?: "Sin email"
        _userRole.value = authRepository.getUserRole() ?: "Sin rol"
    }

    fun logout() {
        authRepository.logout()
    }

    fun getRoleDisplayName(): String {
        return when (authRepository.getUserRole()) {
            "admin" -> "Administrador"
            "empleado" -> "Empleado"
            "repartidor" -> "Repartidor"
            "cliente" -> "Cliente"
            else -> "Sin rol"
        }
    }
}