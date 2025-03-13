# react-native-wireguard-vpn

A React Native module for implementing WireGuard VPN functionality in iOS and Android applications.

## Installation

```bash
npm install react-native-wireguard-vpn
# or
yarn add react-native-wireguard-vpn
```

### iOS
```bash
cd ios && pod install
```

## Usage

```typescript
import WireGuardVpnModule, { 
  WireGuardConfig, 
  WireGuardStatus 
} from 'react-native-wireguard-vpn';

// Configure WireGuard
const config: WireGuardConfig = {
  privateKey: 'YOUR_PRIVATE_KEY',
  publicKey: 'SERVER_PUBLIC_KEY',
  serverAddress: 'SERVER_IP',
  serverPort: 51820,
  allowedIPs: ['0.0.0.0/0'],
  dns: ['1.1.1.1'],
  mtu: 1450,
};

// Initialize VPN
await WireGuardVpnModule.initialize();

// Connect to VPN
await WireGuardVpnModule.connect(config);

// Check VPN status
const status: WireGuardStatus = await WireGuardVpnModule.getStatus();

// Disconnect from VPN
await WireGuardVpnModule.disconnect();
```

## API Reference

### Methods

- `initialize(): Promise<void>` - Initialize the VPN service
- `connect(config: WireGuardConfig): Promise<void>` - Connect to VPN using provided configuration
- `disconnect(): Promise<void>` - Disconnect from VPN
- `getStatus(): Promise<WireGuardStatus>` - Get current VPN status
- `isSupported(): Promise<boolean>` - Check if VPN is supported on the device

### Types

```typescript
interface WireGuardConfig {
  privateKey: string;
  publicKey: string;
  serverAddress: string;
  serverPort: number;
  allowedIPs: string[];
  dns?: string[];
  mtu?: number;
}

interface WireGuardStatus {
  isConnected: boolean;
  tunnelState: 'ACTIVE' | 'INACTIVE' | 'ERROR';
  error?: string;
}
```

## License

MIT 