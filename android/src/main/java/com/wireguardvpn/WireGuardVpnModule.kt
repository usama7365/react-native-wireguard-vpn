package com.wireguardvpn

import com.facebook.react.bridge.*
import com.wireguard.android.backend.GoBackend
import com.wireguard.config.Config
import com.wireguard.config.Interface
import com.wireguard.config.Peer
import com.wireguard.android.backend.Tunnel
import java.net.InetAddress
import com.wireguard.config.InetNetwork
import com.wireguard.config.ParseException

class WireGuardVpnModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    private var backend: GoBackend? = null
    private var tunnel: Tunnel? = null
    private var config: Config? = null

    override fun getName() = "WireGuardVpnModule"

    @ReactMethod
    fun initialize(promise: Promise) {
        try {
            backend = GoBackend(reactApplicationContext)
            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("INIT_ERROR", e.message)
        }
    }

    @ReactMethod
    fun connect(config: ReadableMap, promise: Promise) {
        try {
            val interfaceBuilder = Interface.Builder()
            config.getString("privateKey")?.let { privateKey ->
                interfaceBuilder.parsePrivateKey(privateKey)
            } ?: throw Exception("Private key is required")
            
            // Parse allowed IPs
            val allowedIPs = config.getArray("allowedIPs")
            allowedIPs?.toArrayList()?.forEach { ip ->
                (ip as? String)?.let { ipString ->
                    interfaceBuilder.addAddress(InetNetwork.parse(ipString))
                }
            }

            // Parse DNS servers
            val dnsServers = config.getArray("dns")
            dnsServers?.toArrayList()?.forEach { dns ->
                (dns as? String)?.let { dnsString ->
                    interfaceBuilder.addDnsServer(InetAddress.getByName(dnsString))
                }
            }

            // Set MTU if provided
            if (config.hasKey("mtu")) {
                interfaceBuilder.setMtu(config.getInt("mtu"))
            }

            val peerBuilder = Peer.Builder()
            config.getString("publicKey")?.let { publicKey ->
                peerBuilder.parsePublicKey(publicKey)
            } ?: throw Exception("Public key is required")
            
            // Parse endpoint
            val serverAddress = config.getString("serverAddress") ?: throw Exception("Server address is required")
            val serverPort = config.getInt("serverPort")
            val endpoint = "$serverAddress:$serverPort"
            peerBuilder.parseEndpoint(endpoint)

            // Add allowed IPs to peer
            allowedIPs?.toArrayList()?.forEach { ip ->
                (ip as? String)?.let { ipString ->
                    peerBuilder.addAllowedIp(InetNetwork.parse(ipString))
                }
            }

            val configBuilder = Config.Builder()
            configBuilder.setInterface(interfaceBuilder.build())
            configBuilder.addPeer(peerBuilder.build())

            this.config = configBuilder.build()
            this.tunnel = object : Tunnel {
                override fun getName(): String = "WireGuardTunnel"
                override fun onStateChange(newState: Tunnel.State) {}
            }

            backend?.setState(tunnel!!, Tunnel.State.UP, this.config!!)
            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("CONNECT_ERROR", e.message)
        }
    }

    @ReactMethod
    fun disconnect(promise: Promise) {
        try {
            if (tunnel != null && backend != null && config != null) {
                backend?.setState(tunnel!!, Tunnel.State.DOWN, config!!)
                promise.resolve(null)
            } else {
                promise.reject("DISCONNECT_ERROR", "Tunnel not initialized")
            }
        } catch (e: Exception) {
            promise.reject("DISCONNECT_ERROR", e.message)
        }
    }

    @ReactMethod
    fun getStatus(promise: Promise) {
        try {
            val state = if (tunnel != null && backend != null) {
                backend?.getState(tunnel!!)
            } else {
                Tunnel.State.DOWN
            }

            val status = Arguments.createMap().apply {
                putBoolean("isConnected", state == Tunnel.State.UP)
                putString("tunnelState", state?.name ?: "UNKNOWN")
            }
            promise.resolve(status)
        } catch (e: Exception) {
            promise.reject("STATUS_ERROR", e.message)
        }
    }

    @ReactMethod
    fun isSupported(promise: Promise) {
        promise.resolve(true)
    }
} 