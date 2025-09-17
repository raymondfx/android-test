package com.emirates.loyaltypoints.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emirates.loyaltypoints.data.network.NetworkMonitor
import com.emirates.loyaltypoints.data.repository.AuthRepository
import com.emirates.loyaltypoints.data.repository.LoginCredentials
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the login screen that handles all authentication logic and state management.
 *
 * This ViewModel implements the MVVM pattern and provides:
 * - Real-time input validation for username/email and password
 * - Authentication with proper error handling
 * - Account lockout mechanism after 3 failed attempts with countdown timer
 * - Offline detection and handling
 * - "Remember Me" functionality with token persistence
 * - Deterministic async operations using Kotlin Coroutines
 * - Unidirectional data flow with UI state and events
 *
 * @param authRepository Repository for handling authentication operations
 * @param networkMonitor Service for monitoring network connectivity
 */
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    /** Private mutable state flow for internal state updates */
    private val _uiState = MutableStateFlow(LoginUiState())

    /** Public read-only state flow for UI to observe login state */
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /** Private shared flow for emitting one-time navigation events */
    private val _navigationEvent = MutableSharedFlow<LoginNavigationEvent>(replay = 1)

    /** Public shared flow for UI to observe navigation events */
    val navigationEvent = _navigationEvent.asSharedFlow()

    /** Job for managing the lockout countdown timer */
    private var lockoutJob: Job? = null

    init {
        // Monitor network connectivity changes and update UI state accordingly
        networkMonitor.isOnline
            .onEach { isOnline ->
                _uiState.value = _uiState.value.copy(isOffline = !isOnline)
                updateLoginButtonState()
            }
            .launchIn(viewModelScope)

        // Initialize "Remember Me" state from stored preferences
        val shouldRemember = authRepository.shouldRememberUser()
        _uiState.value = _uiState.value.copy(rememberMe = shouldRemember)

        // Auto-login if user should be remembered and has valid token
        if (shouldRemember && authRepository.getAuthToken() != null) {
            viewModelScope.launch {
                _navigationEvent.emit(LoginNavigationEvent.NavigateToHome)
            }
        }

        // Initial button state will be updated in onEvent methods
    }

    /**
     * Handles all user interaction events from the UI layer.
     *
     * This method implements the unidirectional data flow pattern where
     * UI events are processed and result in state updates.
     *
     * @param event The login event to process
     */
    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.UsernameChanged -> {
                _uiState.value = _uiState.value.copy(
                    username = event.username,
                    isUsernameValid = isUsernameValid(event.username),
                    errorMessage = null
                )
                updateLoginButtonState()
            }
            is LoginEvent.PasswordChanged -> {
                _uiState.value = _uiState.value.copy(
                    password = event.password,
                    isPasswordValid = isPasswordValid(event.password),
                    errorMessage = null
                )
                updateLoginButtonState()
            }
            is LoginEvent.RememberMeChanged -> {
                _uiState.value = _uiState.value.copy(rememberMe = event.rememberMe)
                authRepository.saveRememberMe(event.rememberMe)
            }
            LoginEvent.LoginClicked -> {
                performLogin()
            }
            LoginEvent.ErrorDismissed -> {
                _uiState.value = _uiState.value.copy(errorMessage = null)
            }
            LoginEvent.LockoutTimerTick -> {
                handleLockoutTimer()
            }
        }
    }

    /**
     * Updates the login button enabled state based on current form validity.
     */
    private fun updateLoginButtonState() {
        val currentState = _uiState.value
        val isFormValid = isUsernameValid(currentState.username) &&
                        isPasswordValid(currentState.password) &&
                        !currentState.isLoading &&
                        !currentState.isLockedOut &&
                        !currentState.isOffline

        _uiState.value = currentState.copy(isLoginButtonEnabled = isFormValid)
    }

    /**
     * Performs the authentication process with comprehensive error handling.
     *
     * This method:
     * - Validates network connectivity before attempting login
     * - Handles loading states and user feedback
     * - Implements lockout logic after failed attempts
     * - Manages token persistence based on "Remember Me" setting
     * - Emits navigation events on successful authentication
     */
    private fun performLogin() {
        val currentState = _uiState.value

        if (currentState.isOffline) {
            _uiState.value = currentState.copy(
                errorMessage = "No internet connection. Please check your network and try again."
            )
            return
        }

        if (currentState.isLockedOut) {
            return
        }

        if (!currentState.isLoginButtonEnabled) {
            return
        }

        _uiState.value = currentState.copy(isLoading = true, errorMessage = null)
        updateLoginButtonState()

        viewModelScope.launch {
            try {
                val credentials = LoginCredentials(
                    username = currentState.username.trim(),
                    password = currentState.password
                )

                val response = authRepository.login(credentials)

                if (response.isSuccess && response.token != null) {
                    // Save token if remember me is checked
                    if (currentState.rememberMe) {
                        authRepository.saveAuthToken(response.token)
                    }

                    // Reset failure count on successful login
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        failureCount = 0,
                        errorMessage = null
                    )
                    updateLoginButtonState()

                    _navigationEvent.emit(LoginNavigationEvent.NavigateToHome)
                } else {
                    handleLoginFailure(response.errorMessage ?: "Login failed")
                }
            } catch (e: Exception) {
                handleLoginFailure("Network error. Please try again.")
            }
        }
    }

    /**
     * Processes login failures and manages the account lockout mechanism.
     *
     * This method:
     * - Increments failure count for each failed attempt
     * - Triggers account lockout after reaching max failures (3)
     * - Starts countdown timer during lockout period
     * - Provides user feedback about remaining attempts or lockout status
     *
     * @param errorMessage The error message to display to the user
     */
    private fun handleLoginFailure(errorMessage: String) {
        val currentState = _uiState.value
        val newFailureCount = currentState.failureCount + 1

        if (newFailureCount >= LoginUiState.MAX_FAILURE_COUNT) {
            // Start lockout
            _uiState.value = currentState.copy(
                isLoading = false,
                failureCount = newFailureCount,
                isLockedOut = true,
                lockoutTimeRemaining = LoginUiState.LOCKOUT_DURATION_MS,
                errorMessage = "Too many failed attempts. Account locked for 30 seconds."
            )
            updateLoginButtonState()
            startLockoutTimer()
        } else {
            val remainingAttempts = LoginUiState.MAX_FAILURE_COUNT - newFailureCount
            _uiState.value = currentState.copy(
                isLoading = false,
                failureCount = newFailureCount,
                errorMessage = "$errorMessage ($remainingAttempts attempts remaining)"
            )
            updateLoginButtonState()
        }
    }

    /**
     * Initiates the account lockout countdown timer.
     *
     * The timer is managed by UI events (LockoutTimerTick) rather than
     * internal coroutines to make it more testable and predictable.
     */
    private fun startLockoutTimer() {
        lockoutJob?.cancel()
        // Timer is now handled by UI sending LockoutTimerTick events
    }

    /**
     * Processes timer tick events during account lockout.
     *
     * Updates the remaining lockout time and resets the lockout state
     * when the countdown reaches zero.
     */
    private fun handleLockoutTimer() {
        val currentState = _uiState.value
        if (currentState.isLockedOut && currentState.lockoutTimeRemaining > 0) {
            val newTimeRemaining = (currentState.lockoutTimeRemaining - 1000).coerceAtLeast(0)

            if (newTimeRemaining == 0L) {
                _uiState.value = currentState.copy(
                    isLockedOut = false,
                    lockoutTimeRemaining = 0,
                    failureCount = 0,
                    errorMessage = null
                )
                updateLoginButtonState()
            } else {
                _uiState.value = currentState.copy(lockoutTimeRemaining = newTimeRemaining)
            }
        }
    }

    /**
     * Validates username input to ensure it follows email format.
     *
     * @param username The username string to validate
     * @return true if username is a valid email format, false otherwise
     */
    private fun isUsernameValid(username: String): Boolean {
        if (username.isBlank()) return false
        val emailPattern = "[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
        return username.matches(emailPattern.toRegex())
    }

    /**
     * Validates password strength requirements.
     *
     * @param password The password string to validate
     * @return true if password meets minimum length requirement (6 chars), false otherwise
     */
    private fun isPasswordValid(password: String): Boolean {
        return password.length >= 6
    }

    /**
     * Cleanup method called when ViewModel is destroyed.
     * Cancels any running lockout timer to prevent memory leaks.
     */
    override fun onCleared() {
        super.onCleared()
        lockoutJob?.cancel()
    }
}