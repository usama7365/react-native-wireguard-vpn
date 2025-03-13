import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-wireguard-vpn' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const WireGuardVpnModule = NativeModules.WireGuardVpnModule
  ? NativeModules.WireGuardVpnModule
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

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

export default {
  /**
   * Initialize the VPN service
   */
  initialize(): Promise<void> {
    return WireGuardVpnModule.initialize();
  },

  /**
   * Connect to VPN using provided configuration
   */
  connect(config: WireGuardConfig): Promise<void> {
    return WireGuardVpnModule.connect(config);
  },

  /**
   * Disconnect from VPN
   */
  disconnect(): Promise<void> {
    return WireGuardVpnModule.disconnect();
  },

  /**
   * Get current VPN status
   */
  getStatus(): Promise<WireGuardStatus> {
    return WireGuardVpnModule.getStatus();
  },

  /**
   * Check if VPN is supported on the device
   */
  isSupported(): Promise<boolean> {
    return WireGuardVpnModule.isSupported();
  },
}; 