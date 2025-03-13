# react-native-wireguard-vpn

A React Native module for implementing WireGuard VPN functionality in iOS and Android applications. This package provides a native implementation of WireGuard tunneling with a simple JavaScript interface.

## Requirements

- React Native >= 0.72.0
- iOS 12.0 or later
- Android API level 21 (Android 5.0) or later
- CocoaPods for iOS development

## Installation

```bash
# Using npm
npm install react-native-wireguard-vpn --save

# Using yarn
yarn add react-native-wireguard-vpn
```

### iOS Setup

1. Install CocoaPods dependencies:
```bash
cd ios && pod install && cd ..
```

2. Add the following entries to your Info.plist:
```xml
<key>NSLocalNetworkUsageDescription</key>
<string>This app requires access to network features for VPN functionality</string>
<key>UIBackgroundModes</key>
<array>
    <string>network-authentication</string>
    <string>network</string>
</array>
```

3. Add Network Extension capability to your iOS project:
   - Open your project in Xcode
   - Select your target
   - Go to "Signing & Capabilities"
   - Click "+" and add "Network Extensions"
   - Enable "Packet Tunnel" under Network Extensions

### Android Setup

1. Add the following permissions to your AndroidManifest.xml:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.BIND_VPN_SERVICE" />
```

2. Update your app's build.gradle to ensure compatibility:
```gradle
android {
    compileSdkVersion 34
    
    defaultConfig {
        minSdkVersion 21
        targetSdkVersion 34
    }
}
```

## Usage

```typescript
import WireGuardVpnModule, { 
  WireGuardConfig, 
  WireGuardStatus 
} from 'react-native-wireguard-vpn';

// Configuration object
const config: WireGuardConfig = {
  privateKey: 'YOUR_PRIVATE_KEY',
  publicKey: 'SERVER_PUBLIC_KEY',
  serverAddress: 'SERVER_IP',
  serverPort: 51820,
  allowedIPs: ['0.0.0.0/0'],
  dns: ['1.1.1.1'],
  mtu: 1450,
  presharedKey: 'OPTIONAL_PRESHARED_KEY' // Optional
};

// Initialize VPN service
await WireGuardVpnModule.initialize();

// Connect to VPN
try {
  await WireGuardVpnModule.connect(config);
  console.log('Connected successfully');
} catch (error) {
  console.error('Connection failed:', error);
}

// Check VPN status
const status: WireGuardStatus = await WireGuardVpnModule.getStatus();
console.log('VPN Status:', status);

// Disconnect from VPN
await WireGuardVpnModule.disconnect();
```

## API Reference

### Methods

#### `initialize(): Promise<void>`
Initializes the VPN service. Must be called before any other operations.

#### `connect(config: WireGuardConfig): Promise<void>`
Connects to the VPN using the provided configuration.

#### `disconnect(): Promise<void>`
Disconnects from the VPN.

#### `getStatus(): Promise<WireGuardStatus>`
Gets the current VPN connection status.

#### `isSupported(): Promise<boolean>`
Checks if WireGuard VPN is supported on the device.

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
  presharedKey?: string;
}

interface WireGuardStatus {
  isConnected: boolean;
  tunnelState: 'ACTIVE' | 'INACTIVE' | 'ERROR';
  error?: string;
}
```

## Troubleshooting

### Android
- Ensure your app has the necessary permissions granted
- Check logcat for detailed error messages
- Make sure the VPN service is properly declared in AndroidManifest.xml

### iOS
- Verify Network Extension capability is properly configured
- Check Xcode logs for detailed error messages
- Ensure proper signing and provisioning profiles are set up

## Example App

Check out the example app in our [GitHub repository](https://github.com/usama7365/react-native-wireguard-vpn) for a complete implementation.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

MIT

## Support

For bugs and feature requests, please [create an issue](https://github.com/usama7365/react-native-wireguard-vpn/issues) on our GitHub repository. 