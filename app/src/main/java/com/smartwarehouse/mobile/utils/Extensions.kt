package com.smartwarehouse.mobile.utils

import android.content.Context
import android.util.Patterns
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

// Extensiones para Context
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.showLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

// Extensiones para Date
fun Date.toFormattedString(pattern: String = "dd/MM/yyyy HH:mm"): String {
    val formatter = SimpleDateFormat(pattern, Locale.getDefault())
    return formatter.format(this)
}

fun String.toDate(pattern: String = "yyyy-MM-dd'T'HH:mm:ss"): Date? {
    return try {
        val formatter = SimpleDateFormat(pattern, Locale.getDefault())
        formatter.parse(this)
    } catch (e: Exception) {
        null
    }
}

// Extensiones para String
fun String.isValidEmail(): Boolean {
    return Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

// Extensiones para validación de campos
fun String?.isNullOrBlankOrEmpty(): Boolean {
    return this == null || this.isBlank() || this.isEmpty()
}