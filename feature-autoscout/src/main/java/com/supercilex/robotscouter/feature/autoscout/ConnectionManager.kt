package com.supercilex.robotscouter.feature.autoscout

import android.arch.lifecycle.DefaultLifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.google.firebase.auth.FirebaseAuth
import com.supercilex.robotscouter.core.RobotScouter
import com.supercilex.robotscouter.core.data.device
import com.supercilex.robotscouter.core.data.uid
import com.supercilex.robotscouter.core.logFailures
import kotlinx.coroutines.experimental.async

internal class ConnectionManager(owner: LifecycleOwner) : DefaultLifecycleObserver,
        FirebaseAuth.AuthStateListener {
    private val _statues = MutableLiveData<List<DeviceStatus>>()
    val statues: LiveData<List<DeviceStatus>> = _statues

    private val client: ConnectionsClient = Nearby.getConnectionsClient(RobotScouter)

    private val serviceId get() = "${RobotScouter.packageName}.$uid"
    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            TODO("Send to device status UI - \"Connecting\"")
        }

        override fun onEndpointLost(endpointId: String) {
            TODO("Send to device status UI - \"Unknown\"")
        }
    }
    private val lifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            client.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            if (resolution.status.isSuccess) {
                TODO("Send to device status UI - \"Connected\"")
            } else {
                TODO("Send to device status UI - \"Failed\"")
            }
        }

        override fun onDisconnected(endpointId: String) {
            TODO("Send to device status UI - \"Unknown\"")
        }
    }
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            TODO()
        }

        override fun onPayloadTransferUpdate(
                endpointId: String,
                update: PayloadTransferUpdate
        ) = Unit
    }

    init {
        owner.lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        FirebaseAuth.getInstance().addAuthStateListener(this)
    }

    fun enslave() {
        async {
            client.startAdvertising(
                    device.name ?: "Unnamed device",
                    serviceId,
                    lifecycleCallback,
                    AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
            ).logFailures(serviceId)
        }.logFailures()
    }

    fun findMaster() {
        client.startDiscovery(
                serviceId,
                discoveryCallback,
                DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build()
        ).logFailures(serviceId)
    }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        if (auth.currentUser == null) client.stopAllEndpoints()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        client.stopAllEndpoints()
        FirebaseAuth.getInstance().removeAuthStateListener(this)
    }
}
