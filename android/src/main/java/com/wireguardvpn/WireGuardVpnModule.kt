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
import com.wireguard.crypto.Key

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
            promise.reject("INIT_ERROR", "Failed to initialize WireGuard: ${e.message}")
        }
    }

    @ReactMethod
    fun connect(config: ReadableMap, promise: Promise) {
        try {
            val interfaceBuilder = Interface.Builder()
            
            // Parse private key
            val privateKey = config.getString("privateKey") ?: throw Exception("Private key is required")
            try {
                interfaceBuilder.parsePrivateKey(privateKey)
            } catch (e: ParseException) {
                throw Exception("Invalid private key format: ${e.message}, Key: $privateKey")
            }
            
            // Parse allowed IPs
            val allowedIPs = config.getArray("allowedIPs")?.toArrayList()
                ?: throw Exception("allowedIPs array is required")
            
            try {
                allowedIPs.forEach { ip ->
                    (ip as? String)?.let { ipString ->
                        interfaceBuilder.addAddress(InetNetwork.parse(ipString))
                    } ?: throw Exception("Invalid allowedIP format")
                }
            } catch (e: ParseException) {
                throw Exception("Invalid allowedIP format: ${e.message}, IPs: $allowedIPs")
            }

            // Parse DNS servers
            if (config.hasKey("dns")) {
                val dnsServers = config.getArray("dns")?.toArrayList()
                try {
                    dnsServers?.forEach { dns ->
                        (dns as? String)?.let { dnsString ->
                            interfaceBuilder.addDnsServer(InetAddress.getByName(dnsString))
                        }
                    }
                } catch (e: Exception) {
                    throw Exception("Invalid DNS server format: ${e.message}, DNS: $dnsServers")
                }
            }

            // Set MTU if provided
            if (config.hasKey("mtu")) {
                val mtu = config.getInt("mtu")
                if (mtu < 1280 || mtu > 65535) {
                    throw Exception("MTU must be between 1280 and 65535, got: $mtu")
                }
                interfaceBuilder.setMtu(mtu)
            }

            val peerBuilder = Peer.Builder()
            
            // Parse public key
            val publicKey = config.getString("publicKey") ?: throw Exception("Public key is required")
            try {
                peerBuilder.parsePublicKey(publicKey)
            } catch (e: ParseException) {
                throw Exception("Invalid public key format: ${e.message}, Key: $publicKey")
            }
            
            // Parse preshared key if provided
            if (config.hasKey("presharedKey")) {
                val presharedKey = config.getString("presharedKey")
                try {
                    presharedKey?.let { keyString ->
                        val key = Key.fromBase64(keyString)
                        peerBuilder.setPreSharedKey(key)
                    }
                } catch (e: Exception) {
                    throw Exception("Invalid preshared key format: ${e.message}, Key: $presharedKey")
                }
            }
            
            // Parse endpoint
            val serverAddress = config.getString("serverAddress") ?: throw Exception("Server address is required")
            val serverPort = config.getInt("serverPort")
            if (serverPort < 1 || serverPort > 65535) {
                throw Exception("Port must be between 1 and 65535, got: $serverPort")
            }
            val endpoint = "$serverAddress:$serverPort"
            try {
                peerBuilder.parseEndpoint(endpoint)
            } catch (e: ParseException) {
                throw Exception("Invalid endpoint format: ${e.message}, Endpoint: $endpoint")
            }

            // Add allowed IPs to peer
            try {
                allowedIPs.forEach { ip ->
                    (ip as? String)?.let { ipString ->
                        peerBuilder.addAllowedIp(InetNetwork.parse(ipString))
                    }
                }
            } catch (e: ParseException) {
                throw Exception("Invalid peer allowedIP format: ${e.message}, IPs: $allowedIPs")
            }

            val configBuilder = Config.Builder()
            configBuilder.setInterface(interfaceBuilder.build())
            configBuilder.addPeer(peerBuilder.build())

            this.config = configBuilder.build()
            this.tunnel = object : Tunnel {
                override fun getName(): String = "WireGuardTunnel"
                override fun onStateChange(newState: Tunnel.State) {
                    // Log state changes
                    println("WireGuard tunnel state changed to: $newState")
                }
            }

            try {
                backend?.setState(tunnel!!, Tunnel.State.UP, this.config!!)
                promise.resolve(null)
            } catch (e: Exception) {
                throw Exception("Failed to set tunnel state: ${e.message}")
            }
        } catch (e: Exception) {
            promise.reject("CONNECT_ERROR", "Failed to connect: ${e.message}")
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
            promise.reject("DISCONNECT_ERROR", "Failed to disconnect: ${e.message}")
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
            val status = Arguments.createMap().apply {
                putBoolean("isConnected", false)
                putString("tunnelState", "ERROR")
                putString("error", e.message)
            }
            promise.resolve(status)
        }
    }

    @ReactMethod
    fun isSupported(promise: Promise) {
        promise.resolve(true)
    }
} 