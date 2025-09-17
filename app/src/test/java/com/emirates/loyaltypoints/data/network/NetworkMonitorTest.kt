package com.emirates.loyaltypoints.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NetworkMonitorTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var connectivityManager: ConnectivityManager

    @Mock
    private lateinit var network: Network

    @Mock
    private lateinit var networkCapabilities: NetworkCapabilities

    private lateinit var networkMonitor: NetworkMonitorImpl

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        whenever(context.getSystemService(Context.CONNECTIVITY_SERVICE))
            .thenReturn(connectivityManager)

        networkMonitor = NetworkMonitorImpl(context)
    }

    @Test
    fun `isCurrentlyOnline returns true when network has internet capability`() {
        // Given
        whenever(connectivityManager.activeNetwork).thenReturn(network)
        whenever(connectivityManager.getNetworkCapabilities(network))
            .thenReturn(networkCapabilities)
        whenever(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            .thenReturn(true)

        // When
        val isOnline = networkMonitor.isCurrentlyOnline()

        // Then
        assertTrue(isOnline)
    }

    @Test
    fun `isCurrentlyOnline returns false when no active network`() {
        // Given
        whenever(connectivityManager.activeNetwork).thenReturn(null)

        // When
        val isOnline = networkMonitor.isCurrentlyOnline()

        // Then
        assertFalse(isOnline)
    }

    @Test
    fun `isCurrentlyOnline returns false when network has no internet capability`() {
        // Given
        whenever(connectivityManager.activeNetwork).thenReturn(network)
        whenever(connectivityManager.getNetworkCapabilities(network))
            .thenReturn(networkCapabilities)
        whenever(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            .thenReturn(false)

        // When
        val isOnline = networkMonitor.isCurrentlyOnline()

        // Then
        assertFalse(isOnline)
    }

    @Test
    fun `isCurrentlyOnline returns false when network capabilities are null`() {
        // Given
        whenever(connectivityManager.activeNetwork).thenReturn(network)
        whenever(connectivityManager.getNetworkCapabilities(network))
            .thenReturn(null)

        // When
        val isOnline = networkMonitor.isCurrentlyOnline()

        // Then
        assertFalse(isOnline)
    }
}