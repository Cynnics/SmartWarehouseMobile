package com.smartwarehouse.mobile.utils

object Constants {

    // dispositivo físico
    //const val BASE_URL = "http://192.168.1.135:5294/api/"

    // túnel ngrok
    const val BASE_URL = "https://cogent-anderson-alphamerically.ngrok-free.dev/api/"

    const val GOOGLE_MAPS_API_KEY = "AIzaSyDTRdx3MybA_N2l5hbphrz6iNOB2btipQ0"

    // Preferences
    const val PREFS_NAME = "smartwarehouse_prefs"
    const val KEY_TOKEN = "auth_token"
    const val KEY_USER_ID = "user_id"
    const val KEY_USER_EMAIL = "user_email"
    const val KEY_USER_NAME = "user_name"
    const val KEY_USER_ROLE = "user_role"

    // Request Timeouts
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
}