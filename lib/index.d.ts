export interface WireGuardConfig {
    privateKey: string;
    publicKey: string;
    serverAddress: string;
    serverPort: number;
    /** Interface address(es), e.g. "10.64.0.1/32". If omitted, defaults to 10.64.0.1/32. Do not use 0.0.0.0/0 or ::/0 here (use allowedIPs for routing). */
    address?: string | string[];
    allowedIPs: string[];
    dns?: string[];
    mtu?: number;
    presharedKey?: string;
}
export interface WireGuardStatus {
    isConnected: boolean;
    tunnelState: 'ACTIVE' | 'INACTIVE' | 'CONNECTING' | 'DISCONNECTING' | 'ERROR' | 'UNKNOWN';
    /**
     * Connection status as a simpler string for app UI logic.
     * - CONNECTED when tunnel is up
     * - DISCONNECTED when tunnel is down
     * - CONNECTING/DISCONNECTING/ERROR/UNKNOWN for intermediate states
     */
    status: 'CONNECTED' | 'DISCONNECTED' | 'CONNECTING' | 'DISCONNECTING' | 'ERROR' | 'UNKNOWN';
    error?: string;
}
declare const _default: {
    /**
     * Initialize the VPN service
     */
    initialize(): Promise<void>;
    /**
     * Connect to VPN using provided configuration
     */
    connect(config: WireGuardConfig): Promise<void>;
    /**
     * Disconnect from VPN
     */
    disconnect(): Promise<void>;
    /**
     * Get current VPN status
     */
    getStatus(): Promise<WireGuardStatus>;
    /**
     * Check if VPN is supported on the device
     */
    isSupported(): Promise<boolean>;
};
export default _default;
