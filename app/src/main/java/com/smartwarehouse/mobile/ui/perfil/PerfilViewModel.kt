package com.smartwarehouse.mobile.ui.perfil

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.smartwarehouse.mobile.data.model.response.UsuarioResponse
import com.smartwarehouse.mobile.data.repository.AuthRepository
import com.smartwarehouse.mobile.data.repository.UsuarioRepository
import com.smartwarehouse.mobile.utils.NetworkResult
import kotlinx.coroutines.launch

class PerfilViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)
    private val usuarioRepository = UsuarioRepository(application)

    private val _usuario = MutableLiveData<UsuarioResponse>()
    val usuario: LiveData<UsuarioResponse> = _usuario

    private val _actualizarPerfilResult = MutableLiveData<NetworkResult<Boolean>>()
    val actualizarPerfilResult: LiveData<NetworkResult<Boolean>> = _actualizarPerfilResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val userId = authRepository.getUserId()
        val userName = authRepository.getUserName() ?: "Sin nombre"
        val userEmail = authRepository.getUserEmail() ?: "Sin email"
        val userRole = authRepository.getUserRole() ?: "Sin rol"
        val userPhone = getSharedPreferences().getString("telefono", "Sin teléfono") ?: "Sin teléfono"

        _usuario.value = UsuarioResponse(
            idUsuario = userId,
            nombre = userName,
            email = userEmail,
            rol = userRole,
            telefono = userPhone,
            direccionFacturacion = null
        )
    }

    fun actualizarPerfil(nombre: String, telefono: String) {
        // Validaciones
        if (nombre.isBlank()) {
            _actualizarPerfilResult.value = NetworkResult.Error("El nombre es obligatorio")
            return
        }

        if (telefono.isNotBlank() && !isValidPhone(telefono)) {
            _actualizarPerfilResult.value = NetworkResult.Error("Teléfono no válido")
            return
        }

        _isLoading.value = true
        _actualizarPerfilResult.value = NetworkResult.Loading()

        viewModelScope.launch {
            try {
                val userId = authRepository.getUserId()

                // Actualizar en la API
                val result = usuarioRepository.actualizarUsuario(
                    idUsuario = userId,
                    nombre = nombre,
                    telefono = telefono
                )

                when (result) {
                    is NetworkResult.Success -> {
                        // Actualizar SharedPreferences
                        getSharedPreferences().edit()
                            .putString("nombre", nombre)
                            .putString("telefono", telefono)
                            .apply()

                        // Actualizar LiveData
                        loadUserProfile()

                        _actualizarPerfilResult.value = NetworkResult.Success(true)
                    }
                    is NetworkResult.Error -> {
                        _actualizarPerfilResult.value = NetworkResult.Error(
                            result.message ?: "Error al actualizar perfil"
                        )
                    }
                    is NetworkResult.Loading -> {}
                }
            } catch (e: Exception) {
                _actualizarPerfilResult.value = NetworkResult.Error(
                    "Error: ${e.localizedMessage}"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        authRepository.logout()
        getSharedPreferences().edit().clear().apply()
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

    private fun isValidPhone(phone: String): Boolean {
        // Validar formato de teléfono español: 9 dígitos, empieza con 6, 7 o 9
        val regex = Regex("^[679]\\d{8}$")
        return regex.matches(phone.replace(" ", ""))
    }

    private fun getSharedPreferences() =
        getApplication<Application>().getSharedPreferences("user_prefs", Application.MODE_PRIVATE)
}