package com.emirates.loyaltypoints.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthRepositoryTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var sharedPreferences: SharedPreferences

    @Mock
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var authRepository: AuthRepositoryImpl

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        whenever(context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE))
            .thenReturn(sharedPreferences)
        whenever(sharedPreferences.edit()).thenReturn(editor)
        whenever(editor.putString(any(), any())).thenReturn(editor)
        whenever(editor.putBoolean(any(), any())).thenReturn(editor)
        whenever(editor.remove(any())).thenReturn(editor)

        authRepository = AuthRepositoryImpl(context)
    }

    @Test
    fun `login with valid credentials returns success`() = runTest {
        // Given valid credentials
        val credentials = LoginCredentials("user@emirates.com", "password123")

        // When
        val result = authRepository.login(credentials)

        // Then
        assertTrue(result.isSuccess)
        assertEquals("demo_jwt_token_12345", result.token)
        assertNull(result.errorMessage)
    }

    @Test
    fun `login with invalid username returns failure`() = runTest {
        // Given invalid credentials
        val credentials = LoginCredentials("wrong@email.com", "password123")

        // When
        val result = authRepository.login(credentials)

        // Then
        assertFalse(result.isSuccess)
        assertNull(result.token)
        assertEquals("Invalid username or password", result.errorMessage)
    }

    @Test
    fun `login with invalid password returns failure`() = runTest {
        // Given invalid credentials
        val credentials = LoginCredentials("user@emirates.com", "wrongpassword")

        // When
        val result = authRepository.login(credentials)

        // Then
        assertFalse(result.isSuccess)
        assertNull(result.token)
        assertEquals("Invalid username or password", result.errorMessage)
    }

    @Test
    fun `login with blank username returns failure`() = runTest {
        // Given blank username
        val credentials = LoginCredentials("", "password123")

        // When
        val result = authRepository.login(credentials)

        // Then
        assertFalse(result.isSuccess)
        assertNull(result.token)
        assertEquals("Username is required", result.errorMessage)
    }

    @Test
    fun `login with blank password returns failure`() = runTest {
        // Given blank password
        val credentials = LoginCredentials("user@emirates.com", "")

        // When
        val result = authRepository.login(credentials)

        // Then
        assertFalse(result.isSuccess)
        assertNull(result.token)
        assertEquals("Password is required", result.errorMessage)
    }

    @Test
    fun `saveAuthToken stores token in SharedPreferences`() {
        // Given
        val token = "test_token"

        // When
        authRepository.saveAuthToken(token)

        // Then
        verify(editor).putString("auth_token", token)
        verify(editor).apply()
    }

    @Test
    fun `getAuthToken retrieves token from SharedPreferences`() {
        // Given
        val expectedToken = "stored_token"
        whenever(sharedPreferences.getString("auth_token", null))
            .thenReturn(expectedToken)

        // When
        val token = authRepository.getAuthToken()

        // Then
        assertEquals(expectedToken, token)
    }

    @Test
    fun `getAuthToken returns null when no token stored`() {
        // Given
        whenever(sharedPreferences.getString("auth_token", null))
            .thenReturn(null)

        // When
        val token = authRepository.getAuthToken()

        // Then
        assertNull(token)
    }

    @Test
    fun `clearAuthToken removes token from SharedPreferences`() {
        // When
        authRepository.clearAuthToken()

        // Then
        verify(editor).remove("auth_token")
        verify(editor).apply()
    }

    @Test
    fun `saveRememberMe stores preference in SharedPreferences`() {
        // Given
        val rememberMe = true

        // When
        authRepository.saveRememberMe(rememberMe)

        // Then
        verify(editor).putBoolean("remember_me", rememberMe)
        verify(editor).apply()
    }

    @Test
    fun `shouldRememberUser retrieves preference from SharedPreferences`() {
        // Given
        whenever(sharedPreferences.getBoolean("remember_me", false))
            .thenReturn(true)

        // When
        val shouldRemember = authRepository.shouldRememberUser()

        // Then
        assertTrue(shouldRemember)
    }

    @Test
    fun `shouldRememberUser returns false when no preference stored`() {
        // Given
        whenever(sharedPreferences.getBoolean("remember_me", false))
            .thenReturn(false)

        // When
        val shouldRemember = authRepository.shouldRememberUser()

        // Then
        assertFalse(shouldRemember)
    }
}