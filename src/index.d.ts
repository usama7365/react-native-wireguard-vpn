declare module 'react-native-wireguard-vpn' {
  export interface WireGuardConfig {
    privateKey: string;
    publicKey: string;
    serverAddress: string;
    serverPort: number;
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