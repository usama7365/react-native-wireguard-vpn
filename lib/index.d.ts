export interface WireGuardConfig {
    privateKey: string;
    publicKey: string;
    serverAddress: string;
    serverPort: number;
    allowedIPs: string[];
    dns?: string[];
    mtu?: number;
}
export interface WireGuardStatus {
    isConnected: boolean;
    tunnelState: 'ACTIVE' | 'INACTIVE' | 'ERROR';
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
