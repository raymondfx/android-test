package com.emirates.loyaltypoints.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface for monitoring network connectivity changes and current network state.
 *
 * This abstraction allows for dependency injection and testing while providing
 * reactive network state monitoring capabilities.
 */
interface NetworkMonitor {
    /**
     * Flow that emits the current network connectivity state.
     * Emits true when device has internet connectivity, false otherwise.
     * Only emits when the connectivity state changes.
     */
    val isOnline: Flow<Boolean>

    /**
     * Synchronously checks the current network connectivity state.
     * @return true if device currently has internet connectivity, false otherwise
     */
    fun isCurrentlyOnline(): Boolean
}

/**
 * Implementation of NetworkMonitor that uses Android's ConnectivityManager
 * to monitor network connectivity changes in real-time.
 *
 * This implementation:
 * - Registers network callbacks to detect connectivity changes
 * - Tracks multiple networks (WiFi, cellular, etc.)
 * - Ensures internet capability is available, not just network connection
 * - Provides both reactive (Flow) and synchronous connectivity checking
 *
 * @param context Android application context for accessing system services
 */
@Singleton
class NetworkMonitorImpl @Inject constructor(
    private val context: Context
) : NetworkMonitor {

    /** Android's connectivity manager for monitoring network state */
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override val isOnline: Flow<Boolean> = callbackFlow {
        /**
         * Network callback that tracks available networks with internet capability.
         * Maintains a set of currently available networks and emits connectivity state changes.
         */
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            /** Set of currently available networks with internet capability */
            private val networks = mutableSetOf<Network>()

            /**
             * Called when a new network becomes available.
             * Adds the network to our tracking set and emits updated connectivity state.
             */
            override fun onAvailable(network: Network) {
                networks.add(network)
                trySend(networks.isNotEmpty())
            }

            /**
             * Called when a network is lost or becomes unavailable.
             * Removes the network from our tracking set and emits updated connectivity state.
             */
            override fun onLost(network: Network) {
                networks.remove(network)
                trySend(networks.isNotEmpty())
            }
        }

        /**
         * Network request specifying the types of networks we want to monitor.
         * Only monitors networks that have internet capability and use WiFi or cellular transport.
         */
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        // Register callback to receive network state changes
        connectivityManager.registerNetworkCallback(request, networkCallback)

        // Emit initial connectivity state immediately
        trySend(isCurrentlyOnline())

        // Cleanup when Flow is cancelled
        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }.distinctUntilChanged() // Only emit when connectivity state actually changes

    override fun isCurrentlyOnline(): Boolean {
        // Get the currently active network
        val activeNetwork = connectivityManager.activeNetwork

        // Get capabilities of the active network
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

        // Check if the network has internet capability
        // Returns true only if network exists and has internet access
        return networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}