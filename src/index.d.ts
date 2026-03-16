declare module 'react-native-wireguard-vpn' {
  export interface WireGuardConfig {
    privateKey: string;
    publicKey: string;
    serverAddress: string;
    serverPort: number;
    /** Interface address(es), e.g. "10.64.0.1/32". Optional; defaults to 10.64.0.1/32. Do not use 0.0.0.0/0 or ::/0 here. */
    address?: string | string[];
    allowedIPs: string[];
    dns?: string[];
    mtu?: number;
    presharedKey?: string;
  }

  export interface WireGuardStatus {
    isConnected: boolean;
    tunnelState: 'ACTIVE' | 'INACTIVE' | 'ERROR';
    error?: string;
  }

  export const WireGuardVpnModule: {
    initialize(): Promise<void>;
    connect(config: WireGuardConfig): Promise<void>;
    disconnect(): Promise<void>;
    getStatus(): Promise<WireGuardStatus>;
    isSupported(): Promise<boolean>;
  };
} 