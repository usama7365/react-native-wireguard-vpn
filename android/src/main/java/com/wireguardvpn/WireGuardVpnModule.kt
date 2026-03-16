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
            
            // Interface address: client's VPN tunnel IP (e.g. 10.64.0.1/32). Do NOT use allowedIPs here
            // or the Go backend can return "Bad address" (0.0.0.0/0 or ::/0 are invalid for interface).
            val rawAddress = when {
                config.hasKey("address") && config.getType("address") == ReadableType.Array ->
                    config.getArray("address")?.toArrayList().orEmpty()
                config.hasKey("address") && config.getString("address") != null ->
                    listOf(config.getString("address")!!)
                else -> listOf("10.64.0.1/32")
            }
            val interfaceAddresses = rawAddress.filterIsInstance<String>().map { it.trim() }.filter { it.isNotBlank() }
                .ifEmpty { listOf("10.64.0.1/32") }
            try {
                interfaceAddresses.forEach { addr ->
                    interfaceBuilder.addAddress(InetNetwork.parse(addr))
                }
            } catch (e: ParseException) {
                throw Exception("Invalid interface address format: ${e.message}. Use CIDR like 10.64.0.1/32, not 0.0.0.0/0. Addresses: $interfaceAddresses")
            }

            // Peer allowed IPs: which traffic to route through VPN (e.g. 0.0.0.0/0, ::/0)
            val allowedIPs = config.getArray("allowedIPs")?.toArrayList()
                ?: throw Exception("allowedIPs array is required")
            val normalizedAllowedIPs = allowedIPs.mapNotNull { ip ->
                (ip as? String)?.trim()?.takeIf { it.isNotBlank() }
                    ?.replace("::0/0", "::/0") // normalize IPv6 default route (e.g. Mullvad)
            }
            if (normalizedAllowedIPs.isEmpty()) throw Exception("allowedIPs must contain at least one CIDR")

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

            // Add allowed IPs to peer (routing; do not use for interface address)
            try {
                normalizedAllowedIPs.forEach { ipString ->
                    peerBuilder.addAllowedIp(InetNetwork.parse(ipString))
                }
            } catch (e: ParseException) {
                throw Exception("Invalid peer allowedIP format: ${e.message}. Use ::/0 for IPv6 default, not ::0/0. IPs: $normalizedAllowedIPs")
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
                val msg = e.message ?: e.toString()
                val causeMsg = e.cause?.message
                println("Failed to set tunnel state: $msg")
                if (causeMsg != null) println("Cause: $causeMsg")
                e.printStackTrace()
                throw Exception(if (causeMsg != null) "$msg: $causeMsg" else msg)
            }
        } catch (e: Exception) {
            val msg = e.message ?: e.toString()
            val causeMsg = e.cause?.message
            val fullMsg = buildString {
                append(msg)
                if (causeMsg != null) append(" (cause: $causeMsg)")
                if (msg.contains("Bad address", ignoreCase = true) || msg.contains("BackendException", ignoreCase = true)) {
                    append(". Tip: ensure 'address' is a tunnel CIDR like 10.64.0.1/32, not 0.0.0.0/0; use allowedIPs only for routing.")
                }
            }
            println("Connection failed: $fullMsg")
            e.printStackTrace()
            promise.reject("CONNECT_ERROR", fullMsg)
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