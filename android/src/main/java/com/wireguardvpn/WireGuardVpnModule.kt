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
            println("Starting VPN connection process...")
            println("Received config: $config")
            
            if (backend == null) {
                println("Backend is null, initializing...")
                backend = GoBackend(reactApplicationContext)
            }
            
            val interfaceBuilder = Interface.Builder()
            
            // Parse private key
            val privateKey = config.getString("privateKey") ?: throw Exception("Private key is required")
            try {
                println("Parsing private key: $privateKey")
                interfaceBuilder.parsePrivateKey(privateKey)
                println("Private key parsed successfully")
            } catch (e: ParseException) {
                println("Failed to parse private key: ${e.message}")
                throw Exception("Invalid private key format: ${e.message}, Key: $privateKey")
            }
            
            // Parse allowed IPs
            val allowedIPs = config.getArray("allowedIPs")?.toArrayList()
                ?: throw Exception("allowedIPs array is required")
            
            try {
                println("Parsing allowed IPs: $allowedIPs")
                allowedIPs.forEach { ip ->
                    (ip as? String)?.let { ipString ->
                        interfaceBuilder.addAddress(InetNetwork.parse(ipString))
                    } ?: throw Exception("Invalid allowedIP format")
                }
                println("Allowed IPs parsed successfully")
            } catch (e: ParseException) {
                println("Failed to parse allowed IPs: ${e.message}")
                throw Exception("Invalid allowedIP format: ${e.message}, IPs: $allowedIPs")
            }

            // Parse DNS servers
            if (config.hasKey("dns")) {
                val dnsServers = config.getArray("dns")?.toArrayList()
                try {
                    println("Parsing DNS servers: $dnsServers")
                    dnsServers?.forEach { dns ->
                        (dns as? String)?.let { dnsString ->
                            interfaceBuilder.addDnsServer(InetAddress.getByName(dnsString))
                        }
                    }
                    println("DNS servers parsed successfully")
                } catch (e: Exception) {
                    println("Failed to parse DNS servers: ${e.message}")
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
                println("Parsing public key: $publicKey")
                peerBuilder.parsePublicKey(publicKey)
                println("Public key parsed successfully")
            } catch (e: ParseException) {
                println("Failed to parse public key: ${e.message}")
                throw Exception("Invalid public key format: ${e.message}, Key: $publicKey")
            }
            
            // Parse preshared key if provided
            if (config.hasKey("presharedKey")) {
                val presharedKey = config.getString("presharedKey")
                try {
                    println("Parsing preshared key: $presharedKey")
                    presharedKey?.let { keyString ->
                        val key = Key.fromBase64(keyString)
                        peerBuilder.setPreSharedKey(key)
                    }
                    println("Preshared key parsed successfully")
                } catch (e: Exception) {
                    println("Failed to parse preshared key: ${e.message}")
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
                println("Parsing endpoint: $endpoint")
                peerBuilder.parseEndpoint(endpoint)
                println("Endpoint parsed successfully")
            } catch (e: ParseException) {
                println("Failed to parse endpoint: ${e.message}")
                throw Exception("Invalid endpoint format: ${e.message}, Endpoint: $endpoint")
            }

            // Add allowed IPs to peer
            try {
                println("Adding allowed IPs to peer: $allowedIPs")
                allowedIPs.forEach { ip ->
                    (ip as? String)?.let { ipString ->
                        peerBuilder.addAllowedIp(InetNetwork.parse(ipString))
                    }
                }
                println("Allowed IPs added to peer successfully")
            } catch (e: ParseException) {
                println("Failed to add allowed IPs to peer: ${e.message}")
                throw Exception("Invalid peer allowedIP format: ${e.message}, IPs: $allowedIPs")
            }

            println("Building WireGuard config...")
            val configBuilder = Config.Builder()
            configBuilder.setInterface(interfaceBuilder.build())
            configBuilder.addPeer(peerBuilder.build())

            this.config = configBuilder.build()
            println("WireGuard config built successfully")

            this.tunnel = object : Tunnel {
                override fun getName(): String = "WireGuardTunnel"
                override fun onStateChange(newState: Tunnel.State) {
                    println("WireGuard tunnel state changed to: $newState")
                }
            }

            try {
                println("Checking backend and tunnel state...")
                println("Backend initialized: $backend")
                println("Tunnel initialized: $tunnel")
                println("Config ready: $config")

                println("Attempting to set tunnel state to UP...")
                if (backend == null) {
                    throw Exception("Backend is null")
                }
                if (tunnel == null) {
                    throw Exception("Tunnel is null")
                }
                if (this.config == null) {
                    throw Exception("Config is null")
                }
                backend?.setState(tunnel!!, Tunnel.State.UP, this.config!!)
                println("Successfully set tunnel state to UP")
                promise.resolve(null)
            } catch (e: Exception) {
                println("Failed to set tunnel state: ${e.message}")
                println("Exception stack trace:")
                e.printStackTrace()
                println("Backend state: ${backend != null}")
                println("Tunnel state: ${tunnel != null}")
                println("Config state: ${this.config != null}")
                throw Exception("Failed to set tunnel state: ${e.message}")
            }
        } catch (e: Exception) {
            println("Connection failed with error: ${e.message}")
            println("Exception stack trace:")
            e.printStackTrace()
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