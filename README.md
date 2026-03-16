# react-native-wireguard-vpn

A React Native module for implementing WireGuard VPN functionality in iOS and Android applications. This package provides a native implementation of WireGuard tunneling with a simple JavaScript interface.

## Requirements

- React Native >= 0.72.0
- iOS 12.0 or later
- Android API level 21 (Android 5.0) or later
- CocoaPods for iOS development
- **Not compatible with Expo Go** — use a development build or bare workflow (e.g. `npx expo run:ios` after prebuild).

## Installation

```bash
# Using npm
npm install react-native-wireguard-vpn --save

# Using yarn
yarn add react-native-wireguard-vpn
```

### iOS Setup

1. **Install CocoaPods dependencies** (required for the native module to link):
```bash
cd ios && pod install && cd ..
```
   Or from project root: `npx pod-install` (if you use `pod-install`).
   After adding the package, you must run `pod install` and **rebuild the app** (e.g. `npx expo run:ios` or build from Xcode).

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

3. Add a **Packet Tunnel** Network Extension to your iOS project:
   - In Xcode: File → New → Target → **Network Extension** → **Packet Tunnel Provider**. Name it e.g. "WireGuardTunnel".
   - Set the extension target’s **Bundle Identifier** to `com.wireguardvpn.tunnel` (or the same value your app uses when starting the tunnel). The library uses `com.wireguardvpn.tunnel` by default in the native code.
   - In your **main app target**: Signing & Capabilities → "+" → add **Network Extensions** and enable **Packet Tunnel**.
   - Use a real device for testing; VPN/Network Extension often does not work in the simulator.

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

// Configuration object (e.g. Mullvad-style). Use "address" for your tunnel IP if your provider gives one.
const config: WireGuardConfig = {
  privateKey: 'YOUR_PRIVATE_KEY',
  publicKey: 'SERVER_PUBLIC_KEY',
  serverAddress: 'SERVER_IP',
  serverPort: 51820,
  address: '10.64.0.1/32',  // optional; interface tunnel IP (defaults to 10.64.0.1/32). Use allowedIPs only for routing.
  allowedIPs: ['0.0.0.0/0', '::/0'],  // use ::/0 for IPv6 default (not ::0/0)
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
  address?: string | string[];  // optional; tunnel IP e.g. "10.64.0.1/32" (defaults to 10.64.0.1/32)
  allowedIPs: string[];         // routing only; use ::/0 for IPv6 default
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

## Linking checklist (if you see "doesn't seem to be linked")

- Run **`pod install`** in the `ios` folder (or `npx pod-install` from the project root).
- **Rebuild the app** after installing the package (e.g. `npx expo run:ios --device` or build from Xcode). A reload is not enough.
- **Do not use Expo Go.** Use a development build (e.g. `npx expo run:ios` or a custom dev client). This library uses native code and is not available in Expo Go.
- **Expo (prebuild):** After `npx expo prebuild`, run `pod install` in `ios/` and then `npx expo run:ios`.

## Troubleshooting

### "The package 'react-native-wireguard-vpn' doesn't seem to be linked"
Follow the [Linking checklist](#linking-checklist-if-you-see-doesnt-seem-to-be-linked) above. Ensure you have run `pod install`, rebuilt the app (not just reloaded), and are not using Expo Go.

### "Failed to set tunnel state: Bad address" (Android)
This usually happens when the **interface address** is wrong. The config needs:
- **`address`** (optional): your tunnel IP in CIDR form, e.g. `"10.64.0.1/32"`. If your provider (e.g. Mullvad) gives an "Address" in the `[Interface]` section, use that. If omitted, the library uses `10.64.0.1/32`.
- **`allowedIPs`**: only for *routing* (what traffic goes through the VPN), e.g. `["0.0.0.0/0", "::/0"]`. Use `::/0` for IPv6 default, not `::0/0`. Do **not** use `0.0.0.0/0` or `::/0` as the interface address.

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