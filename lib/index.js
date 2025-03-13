"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const react_native_1 = require("react-native");
const LINKING_ERROR = `The package 'react-native-wireguard-vpn' doesn't seem to be linked. Make sure: \n\n` +
    react_native_1.Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
    '- You rebuilt the app after installing the package\n' +
    '- You are not using Expo Go\n';
const WireGuardVpnModule = react_native_1.NativeModules.WireGuardVpnModule
    ? react_native_1.NativeModules.WireGuardVpnModule
    : new Proxy({}, {
        get() {
            throw new Error(LINKING_ERROR);
        },
    });
exports.default = {
    /**
     * Initialize the VPN service
     */
    initialize() {
        return WireGuardVpnModule.initialize();
    },
    /**
     * Connect to VPN using provided configuration
     */
    connect(config) {
        return WireGuardVpnModule.connect(config);
    },
    /**
     * Disconnect from VPN
     */
    disconnect() {
        return WireGuardVpnModule.disconnect();
    },
    /**
     * Get current VPN status
     */
    getStatus() {
        return WireGuardVpnModule.getStatus();
    },
    /**
     * Check if VPN is supported on the device
     */
    isSupported() {
        return WireGuardVpnModule.isSupported();
    },
};
//# sourceMappingURL=index.js.map