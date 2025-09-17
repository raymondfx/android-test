package com.emirates.loyaltypoints.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class representing user login credentials.
 *
 * @property username The user's email address or username
 * @property password The user's password in plain text (will be securely handled)
 */
data class LoginCredentials(
    val username: String,
    val password: String
)

/**
 * Data class representing the response from an authentication attempt.
 *
 * @property isSuccess Whether the authentication was successful
 * @property token JWT or session token if authentication succeeded, null otherwise
 * @property errorMessage Human-readable error message if authentication failed, null on success
 */
data class AuthResponse(
    val isSuccess: Boolean,
    val token: String? = null,
    val errorMessage: String? = null
)

/**
 * Repository interface for authentication operations and token management.
 *
 * This interface abstracts authentication logic and local storage operations,
 * allowing for easy testing and different implementations (e.g., mock, production).
 */
interface AuthRepository {
    /**
     * Attempts to authenticate user with provided credentials.
     *
     * @param credentials User's login credentials (username/email and password)
     * @return AuthResponse indicating success/failure and containing token or error message
     */
    suspend fun login(credentials: LoginCredentials): AuthResponse

    /**
     * Saves the authentication token to persistent storage.
     *
     * @param token JWT or session token to be stored securely
     */
    fun saveAuthToken(token: String)

    /**
     * Retrieves the stored authentication token.
     *
     * @return Stored authentication token or null if none exists
     */
    fun getAuthToken(): String?

    /**
     * Removes the stored authentication token from persistent storage.
     * Used for logout operations.
     */
    fun clearAuthToken()

    /**
     * Saves the user's "Remember Me" preference.
     *
     * @param remember Whether the user wants to be remembered across app sessions
     */
    fun saveRememberMe(remember: Boolean)

    /**
     * Checks if the user has enabled "Remember Me" functionality.
     *
     * @return true if user should be remembered, false otherwise
     */
    fun shouldRememberUser(): Boolean
}

/**
 * Production implementation of AuthRepository that handles authentication
 * with simulated network calls and SharedPreferences for local storage.
 *
 * This implementation includes:
 * - Simulated authentication with demo credentials
 * - Secure token storage using SharedPreferences
 * - "Remember Me" functionality
 * - Input validation and error handling
 *
 * @param context Android application context for accessing SharedPreferences
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val context: Context
) : AuthRepository {

    /** SharedPreferences instance for storing authentication data */
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(
        "auth_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        /** Key for storing authentication token in SharedPreferences */
        private const val TOKEN_KEY = "auth_token"

        /** Key for storing "Remember Me" preference in SharedPreferences */
        private const val REMEMBER_ME_KEY = "remember_me"

        /** Demo username for testing purposes */
        private const val DEMO_USERNAME = "user@emirates.com"

        /** Demo password for testing purposes */
        private const val DEMO_PASSWORD = "password123"

        /** Demo JWT token returned on successful authentication */
        private const val DEMO_TOKEN = "demo_jwt_token_12345"
    }

    override suspend fun login(credentials: LoginCredentials): AuthResponse {
        // Simulate realistic network delay (1.5 seconds)
        delay(1500)

        // Validate credentials and return appropriate response
        return when {
            credentials.username.isBlank() -> {
                AuthResponse(
                    isSuccess = false,
                    errorMessage = "Username is required"
                )
            }
            credentials.password.isBlank() -> {
                AuthResponse(
                    isSuccess = false,
                    errorMessage = "Password is required"
                )
            }
            credentials.username == DEMO_USERNAME && credentials.password == DEMO_PASSWORD -> {
                // Successful authentication with demo credentials
                AuthResponse(
                    isSuccess = true,
                    token = DEMO_TOKEN
                )
            }
            else -> {
                // Invalid credentials
                AuthResponse(
                    isSuccess = false,
                    errorMessage = "Invalid username or password"
                )
            }
        }
    }

    override fun saveAuthToken(token: String) {
        // Store authentication token securely in SharedPreferences
        sharedPrefs.edit()
            .putString(TOKEN_KEY, token)
            .apply()
    }

    override fun getAuthToken(): String? {
        // Retrieve stored authentication token
        return sharedPrefs.getString(TOKEN_KEY, null)
    }

    override fun clearAuthToken() {
        // Remove authentication token from storage (logout)
        sharedPrefs.edit()
            .remove(TOKEN_KEY)
            .apply()
    }

    override fun saveRememberMe(remember: Boolean) {
        // Store user's "Remember Me" preference
        sharedPrefs.edit()
            .putBoolean(REMEMBER_ME_KEY, remember)
            .apply()
    }

    override fun shouldRememberUser(): Boolean {
        // Check if user should be remembered (defaults to false)
        return sharedPrefs.getBoolean(REMEMBER_ME_KEY, false)
    }
}