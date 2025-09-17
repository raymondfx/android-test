package com.emirates.loyaltypoints.presentation.login

import com.emirates.loyaltypoints.data.network.NetworkMonitor
import com.emirates.loyaltypoints.data.repository.AuthRepository
import com.emirates.loyaltypoints.data.repository.AuthResponse
import com.emirates.loyaltypoints.data.repository.LoginCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @Mock
    private lateinit var authRepository: AuthRepository

    @Mock
    private lateinit var networkMonitor: NetworkMonitor

    private lateinit var viewModel: LoginViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Default mock behaviors
        whenever(networkMonitor.isOnline).thenReturn(flowOf(true))
        whenever(authRepository.shouldRememberUser()).thenReturn(false)
        whenever(authRepository.getAuthToken()).thenReturn(null)

        viewModel = LoginViewModel(authRepository, networkMonitor)
    }

    @Test
    fun `validation enables login button when inputs are valid`() = runTest {
        // Given valid inputs
        viewModel.onEvent(LoginEvent.UsernameChanged("user@emirates.com"))
        viewModel.onEvent(LoginEvent.PasswordChanged("password123"))

        // When
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(viewModel.uiState.value.isLoginButtonEnabled)
    }

    @Test
    fun `validation disables login button when username is invalid`() = runTest {
        // Given invalid username
        viewModel.onEvent(LoginEvent.UsernameChanged("invalid-email"))
        viewModel.onEvent(LoginEvent.PasswordChanged("password123"))

        // When
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.isLoginButtonEnabled)
        assertFalse(viewModel.uiState.value.isUsernameValid)
    }

    @Test
    fun `validation disables login button when password is too short`() = runTest {
        // Given short password
        viewModel.onEvent(LoginEvent.UsernameChanged("user@emirates.com"))
        viewModel.onEvent(LoginEvent.PasswordChanged("123"))

        // When
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(viewModel.uiState.value.isLoginButtonEnabled)
        assertFalse(viewModel.uiState.value.isPasswordValid)
    }

    @Test
    fun `successful login triggers navigation event`() = runTest {
        // Given successful login response
        val successResponse = AuthResponse(isSuccess = true, token = "test_token")
        whenever(authRepository.login(any())).thenReturn(successResponse)

        // Setup valid inputs
        viewModel.onEvent(LoginEvent.UsernameChanged("user@emirates.com"))
        viewModel.onEvent(LoginEvent.PasswordChanged("password123"))
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify button is enabled before clicking
        assertTrue(viewModel.uiState.value.isLoginButtonEnabled)

        // When
        viewModel.onEvent(LoginEvent.LoginClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val navigationEvent = withTimeout(5000) {
            viewModel.navigationEvent.first()
        }
        assertEquals(LoginNavigationEvent.NavigateToHome, navigationEvent)
        assertEquals(0, viewModel.uiState.value.failureCount)
    }

    @Test
    fun `failed login increments failure count`() = runTest {
        // Given failed login response
        val failureResponse = AuthResponse(isSuccess = false, errorMessage = "Invalid credentials")
        whenever(authRepository.login(any())).thenReturn(failureResponse)

        // Setup valid inputs
        viewModel.onEvent(LoginEvent.UsernameChanged("user@emirates.com"))
        viewModel.onEvent(LoginEvent.PasswordChanged("wrongpassword"))

        // When
        viewModel.onEvent(LoginEvent.LoginClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(1, viewModel.uiState.value.failureCount)
        assertTrue(viewModel.uiState.value.errorMessage?.contains("Invalid credentials") == true)
    }

    @Test
    fun `lockout occurs after 3 failed attempts`() = runTest {
        // Given failed login response
        val failureResponse = AuthResponse(isSuccess = false, errorMessage = "Invalid credentials")
        whenever(authRepository.login(any())).thenReturn(failureResponse)

        // Setup valid inputs
        viewModel.onEvent(LoginEvent.UsernameChanged("user@emirates.com"))
        viewModel.onEvent(LoginEvent.PasswordChanged("wrongpassword"))

        // When - attempt login 3 times
        repeat(3) {
            viewModel.onEvent(LoginEvent.LoginClicked)
            testDispatcher.scheduler.advanceUntilIdle()
        }

        // Then
        assertTrue(viewModel.uiState.value.isLockedOut)
        assertEquals(3, viewModel.uiState.value.failureCount)
        assertEquals(LoginUiState.LOCKOUT_DURATION_MS, viewModel.uiState.value.lockoutTimeRemaining)
        assertFalse(viewModel.uiState.value.isLoginButtonEnabled)
    }

    @Test
    fun `lockout timer decreases over time`() = runTest {
        // Given lockout state
        val failureResponse = AuthResponse(isSuccess = false, errorMessage = "Invalid credentials")
        whenever(authRepository.login(any())).thenReturn(failureResponse)

        viewModel.onEvent(LoginEvent.UsernameChanged("user@emirates.com"))
        viewModel.onEvent(LoginEvent.PasswordChanged("wrongpassword"))

        // Trigger lockout
        repeat(3) {
            viewModel.onEvent(LoginEvent.LoginClicked)
            testDispatcher.scheduler.advanceUntilIdle()
        }

        // When - simulate one timer tick (1 second)
        viewModel.onEvent(LoginEvent.LockoutTimerTick)

        // Then
        assertEquals(
            LoginUiState.LOCKOUT_DURATION_MS - 1000,
            viewModel.uiState.value.lockoutTimeRemaining
        )
        assertTrue(viewModel.uiState.value.isLockedOut)
    }

    @Test
    fun `lockout ends after timer expires`() = runTest {
        // Given lockout state
        val failureResponse = AuthResponse(isSuccess = false, errorMessage = "Invalid credentials")
        whenever(authRepository.login(any())).thenReturn(failureResponse)

        viewModel.onEvent(LoginEvent.UsernameChanged("user@emirates.com"))
        viewModel.onEvent(LoginEvent.PasswordChanged("wrongpassword"))

        // Trigger lockout
        repeat(3) {
            viewModel.onEvent(LoginEvent.LoginClicked)
            testDispatcher.scheduler.advanceUntilIdle()
        }

        // When - simulate timer ticks until lockout expires
        val ticksNeeded = (LoginUiState.LOCKOUT_DURATION_MS / 1000).toInt() + 1
        repeat(ticksNeeded) {
            viewModel.onEvent(LoginEvent.LockoutTimerTick)
        }

        // Then
        assertFalse(viewModel.uiState.value.isLockedOut)
        assertEquals(0L, viewModel.uiState.value.lockoutTimeRemaining)
        assertEquals(0, viewModel.uiState.value.failureCount)
    }

    @Test
    fun `offline state prevents login and shows message`() = runTest {
        // Given offline state
        whenever(networkMonitor.isOnline).thenReturn(flowOf(false))
        val offlineViewModel = LoginViewModel(authRepository, networkMonitor)

        // Setup valid inputs
        offlineViewModel.onEvent(LoginEvent.UsernameChanged("user@emirates.com"))
        offlineViewModel.onEvent(LoginEvent.PasswordChanged("password123"))
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        offlineViewModel.onEvent(LoginEvent.LoginClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(offlineViewModel.uiState.value.isOffline)
        assertFalse(offlineViewModel.uiState.value.isLoginButtonEnabled)
        assertTrue(offlineViewModel.uiState.value.errorMessage?.contains("No internet connection") == true)
    }

    @Test
    fun `remember me persists token on successful login`() = runTest {
        // Given successful login response and remember me enabled
        val successResponse = AuthResponse(isSuccess = true, token = "test_token")
        whenever(authRepository.login(any())).thenReturn(successResponse)

        // Setup inputs with remember me
        viewModel.onEvent(LoginEvent.UsernameChanged("user@emirates.com"))
        viewModel.onEvent(LoginEvent.PasswordChanged("password123"))
        viewModel.onEvent(LoginEvent.RememberMeChanged(true))

        // When
        viewModel.onEvent(LoginEvent.LoginClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(authRepository).saveAuthToken("test_token")
        verify(authRepository).saveRememberMe(true)
    }

    @Test
    fun `remember me does not persist token when disabled`() = runTest {
        // Given successful login response and remember me disabled
        val successResponse = AuthResponse(isSuccess = true, token = "test_token")
        whenever(authRepository.login(any())).thenReturn(successResponse)

        // Setup inputs without remember me
        viewModel.onEvent(LoginEvent.UsernameChanged("user@emirates.com"))
        viewModel.onEvent(LoginEvent.PasswordChanged("password123"))
        viewModel.onEvent(LoginEvent.RememberMeChanged(false))

        // When
        viewModel.onEvent(LoginEvent.LoginClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(authRepository).saveRememberMe(false)
        // saveAuthToken should not be called when rememberMe is false
    }

    @Test
    fun `auto navigation occurs when user has valid token and remember me enabled`() = runTest {
        // Given user should be remembered with valid token
        whenever(authRepository.shouldRememberUser()).thenReturn(true)
        whenever(authRepository.getAuthToken()).thenReturn("valid_token")

        // When
        val autoLoginViewModel = LoginViewModel(authRepository, networkMonitor)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val navigationEvent = withTimeout(5000) {
            autoLoginViewModel.navigationEvent.first()
        }
        assertEquals(LoginNavigationEvent.NavigateToHome, navigationEvent)
    }

    @Test
    fun `error message is cleared when inputs change`() = runTest {
        // Given failed login with error message
        val failureResponse = AuthResponse(isSuccess = false, errorMessage = "Invalid credentials")
        whenever(authRepository.login(any())).thenReturn(failureResponse)

        viewModel.onEvent(LoginEvent.UsernameChanged("user@emirates.com"))
        viewModel.onEvent(LoginEvent.PasswordChanged("wrongpassword"))
        viewModel.onEvent(LoginEvent.LoginClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify error exists
        assertTrue(viewModel.uiState.value.errorMessage != null)

        // When
        viewModel.onEvent(LoginEvent.UsernameChanged("user@emirates.com"))

        // Then
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }
}