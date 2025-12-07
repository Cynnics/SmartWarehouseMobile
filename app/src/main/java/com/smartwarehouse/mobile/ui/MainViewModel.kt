package com.smartwarehouse.mobile.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.smartwarehouse.mobile.data.repository.AuthRepository

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _userRole = MutableLiveData<String>()
    val userRole: LiveData<String> = _userRole

    init {
        loadUserInfo()
    }

    private fun loadUserInfo() {
        _userName.value = authRepository.getUserName() ?: "Usuario"
        _userRole.value = authRepository.getUserRole() ?: "Sin rol"
    }

    fun isRepartidor(): Boolean {
        return authRepository.getUserRole() == "repartidor"
    }

    fun isCliente(): Boolean {
        return authRepository.getUserRole() == "cliente"
    }

    fun logout() {
        authRepository.logout()
    }
}