// Expo config plugin that updates iOS Info.plist + entitlements for Network Extension.
// This does NOT create the Packet Tunnel extension target; that still must be created in Xcode.

const getConfigPlugins = () => {
  // Prefer expo's re-export first, but support older setups.
  try {
    return require('expo/config-plugins');
  } catch (e) {
    return require('@expo/config-plugins');
  }
};

const configPlugins = getConfigPlugins();
const { withInfoPlist, withEntitlementsPlist, withAndroidManifest } = configPlugins;

/**
 * @type {(config: import('@expo/config-plugins').ConfigPluginProps) => any}
 */
function withWireGuardVpn(config) {
  // 1) iOS Info.plist
  config = withInfoPlist(config, (cfg) => {
    cfg.modResults.NSLocalNetworkUsageDescription =
      cfg.modResults.NSLocalNetworkUsageDescription ||
      'This app requires access to network features for VPN functionality';

    const existingModes = cfg.modResults.UIBackgroundModes;
    const modes = Array.isArray(existingModes) ? existingModes.slice() : [];

    if (!modes.includes('network-authentication')) modes.push('network-authentication');
    if (!modes.includes('network')) modes.push('network');

    cfg.modResults.UIBackgroundModes = modes;
    return cfg;
  });

  // 2) iOS entitlements (.entitlements plist)
  config = withEntitlementsPlist(config, (cfg) => {
    const entitlements = cfg.modResults || {};
    const key = 'com.apple.developer.networking.networkextension';

    const current = entitlements[key];
    const values = Array.isArray(current)
      ? current.slice()
      : current
        ? [current]
        : [];

    // This entitlement is required for NEPacketTunnelProvider.
    // If your app/provisioning already contains it, this won't duplicate values.
    if (!values.includes('packet-tunnel-provider')) values.push('packet-tunnel-provider');

    entitlements[key] = values;
    cfg.modResults = entitlements;
    return cfg;
  });

  // 3) Android: ensure required VPN permissions exist in the app manifest.
  // RN autolinking usually merges the library manifest automatically, but for Expo/prebuild
  // we add them defensively to prevent "missing permission" issues.
  config = withAndroidManifest(config, (cfg) => {
    const manifest = cfg.modResults.manifest;
    if (!manifest) return cfg;

    const perms = Array.isArray(manifest['uses-permission'])
      ? manifest['uses-permission']
      : [];

    const ensurePermission = (name) => {
      const exists = perms.some(
        (p) => p && p.$ && p.$['android:name'] && p.$['android:name'] === name
      );
      if (!exists) {
        perms.push({ $: { 'android:name': name } });
      }
    };

    ensurePermission('android.permission.INTERNET');
    ensurePermission('android.permission.FOREGROUND_SERVICE');
    ensurePermission('android.permission.BIND_VPN_SERVICE');

    manifest['uses-permission'] = perms;

    // networkSecurityConfig reference: only set if it doesn't exist yet.
    try {
      const application = Array.isArray(manifest.application)
        ? manifest.application[0]
        : manifest.application;
      if (application && application.$) {
        const existing = application.$['android:networkSecurityConfig'];
        if (!existing) {
          application.$['android:networkSecurityConfig'] = '@xml/network_security_config';
        }
      }
    } catch (e) {
      // ignore; manifest structure can vary across Expo/RN versions
    }

    return cfg;
  });

  return config;
}

module.exports = withWireGuardVpn;

