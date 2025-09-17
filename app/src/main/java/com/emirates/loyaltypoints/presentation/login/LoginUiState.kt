package com.emirates.loyaltypoints.presentation.login

/**
 * Represents the complete UI state for the login screen.
 *
 * This data class encapsulates all the state needed for the login screen including
 * user inputs, validation states, authentication status, error handling, and lockout mechanism.
 *
 * @property username The entered username/email address
 * @property password The entered password
 * @property isLoading Whether authentication request is in progress
 * @property errorMessage Current error message to display to user, null if no error
 * @property isUsernameValid Whether the current username passes validation (email format)
 * @property isPasswordValid Whether the current password passes validation (min 6 chars)
 * @property failureCount Number of consecutive failed login attempts
 * @property isLockedOut Whether the account is currently locked due to too many failures
 * @property lockoutTimeRemaining Milliseconds remaining in lockout period
 * @property isOffline Whether the device is currently offline/has no network connection
 * @property rememberMe Whether user has opted to remember their login credentials
 * @property isLoginButtonEnabled Whether the login button should be enabled based on current state
 */
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isUsernameValid: Boolean = true,
    val isPasswordValid: Boolean = true,
    val failureCount: Int = 0,
    val isLockedOut: Boolean = false,
    val lockoutTimeRemaining: Long = 0,
    val isOffline: Boolean = false,
    val rememberMe: Boolean = false,
    val isLoginButtonEnabled: Boolean = false
) {
    companion object {
        /** Maximum number of failed login attempts before account lockout */
        const val MAX_FAILURE_COUNT = 3

        /** Duration of account lockout in milliseconds (30 seconds for demo purposes) */
        const val LOCKOUT_DURATION_MS = 30_000L
    }
}

/**
 * Sealed class representing different user interaction events in the login screen.
 *
 * These events are dispatched from the UI to the ViewModel to handle user actions
 * and state changes in a unidirectional data flow pattern.
 */
sealed class LoginEvent {
    /**
     * User has changed the username/email input field.
     * @property username The new username value
     */
    data class UsernameChanged(val username: String) : LoginEvent()

    /**
     * User has changed the password input field.
     * @property password The new password value
     */
    data class PasswordChanged(val password: String) : LoginEvent()

    /**
     * User has toggled the "Remember Me" checkbox.
     * @property rememberMe Whether remember me should be enabled
     */
    data class RememberMeChanged(val rememberMe: Boolean) : LoginEvent()

    /**
     * User has clicked the login button to attempt authentication.
     */
    object LoginClicked : LoginEvent()

    /**
     * User has dismissed/acknowledged the current error message.
     */
    object ErrorDismissed : LoginEvent()

    /**
     * Internal timer tick for updating lockout countdown.
     * This is triggered automatically during lockout periods.
     */
    object LockoutTimerTick : LoginEvent()
}

/**
 * Sealed class representing navigation events that should trigger screen transitions.
 *
 * These events are emitted by the ViewModel when certain conditions are met
 * (like successful authentication) and consumed by the UI to perform navigation.
 */
sealed class LoginNavigationEvent {
    /**
     * Navigation event triggered when user successfully authenticates
     * or when a valid existing session is found.
     */
    object NavigateToHome : LoginNavigationEvent()
}