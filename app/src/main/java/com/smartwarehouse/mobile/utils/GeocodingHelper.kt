package com.smartwarehouse.mobile.utils

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Helper para convertir direcciones en coordenadas usando Geocoding API
 */
object GeocodingHelper {

    private val client = OkHttpClient()

    /**
     * Convierte una dirección de texto en coordenadas LatLng
     *
     * @param direccion Dirección completa (ej: "Calle Gran Vía 28, Madrid, España")
     * @return LatLng o null si no se encuentra
     */
    suspend fun getCoordinatesFromAddress(direccion: String): LatLng? {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = Constants.GOOGLE_MAPS_API_KEY

                // URL encode de la dirección
                val encodedAddress = URLEncoder.encode(direccion, "UTF-8")

                val url = "https://maps.googleapis.com/maps/api/geocode/json?" +
                        "address=$encodedAddress" +
                        "&key=$apiKey"

                val request = Request.Builder()
                    .url(url)
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val jsonString = response.body?.string() ?: ""
                    val json = JSONObject(jsonString)

                    val status = json.getString("status")
                    if (status == "OK") {
                        val results = json.getJSONArray("results")
                        if (results.length() > 0) {
                            val location = results.getJSONObject(0)
                                .getJSONObject("geometry")
                                .getJSONObject("location")

                            val lat = location.getDouble("lat")
                            val lng = location.getDouble("lng")

                            return@withContext LatLng(lat, lng)
                        }
                    } else {
                        Log.e("Geocoding", "Status: $status, response: $jsonString")
                    }
                }

                null
            } catch (e: Exception) {
                Log.e("Geocoding", "Error", e)
                null
            }
        }
    }
    /**
     * Valida si una dirección es válida antes de geocodificar
     */
    fun isValidAddress(direccion: String?): Boolean {
        return !direccion.isNullOrBlank() && direccion.length > 5
    }

    /**
     * Normaliza una dirección para mejorar resultados de geocoding
     * Añade "España" o la ciudad si no está presente
     */
    fun normalizeAddress(direccion: String, ciudad: String = "Madrid", pais: String = "España"): String {
        var normalized = direccion.trim()

        // Si no contiene ciudad, añadirla
        if (!normalized.contains(ciudad, ignoreCase = true)) {
            normalized += ", $ciudad"
        }

        // Si no contiene país, añadirlo
        if (!normalized.contains(pais, ignoreCase = true)) {
            normalized += ", $pais"
        }

        return normalized
    }

    /**
     * Cache simple en memoria para evitar llamadas repetidas
     */
    private val geocodingCache = mutableMapOf<String, LatLng>()

    suspend fun getCoordinatesFromAddressWithCache(direccion: String): LatLng? {

        val normalized = normalizeAddress(direccion)
        geocodingCache[normalized]?.let { return it }
        // Buscar en cache
        //geocodingCache[direccion]?.let { return it }

        // Si no está en cache, geocodificar
        val coordinates = getCoordinatesFromAddress(normalized)
        // Guardar en cache
        coordinates?.let { geocodingCache[normalized] = it }

        return coordinates
    }

    /**
     * Limpia la cache de geocoding
     */
    fun clearCache() {
        geocodingCache.clear()
    }
}