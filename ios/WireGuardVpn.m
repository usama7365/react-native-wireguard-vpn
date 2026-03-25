#import "WireGuardVpn.h"
#import <NetworkExtension/NetworkExtension.h>

@implementation WireGuardVpn {
  id _neStatusObserver;
}

RCT_EXPORT_MODULE(WireGuardVpnModule)

- (NSArray<NSString *> *)supportedEvents
{
  return @[@"vpnStateChanged"];
}

// RN will call these when JS starts/stops listening to events.
- (void)startObserving
{
  if (_neStatusObserver != nil) return;

  __weak typeof(self) weakSelf = self;
  _neStatusObserver = [[NSNotificationCenter defaultCenter]
      addObserverForName:NEVPNStatusDidChangeNotification
                  object:nil
                   queue:[NSOperationQueue mainQueue]
              usingBlock:^(NSNotification * _Nonnull note) {
                [weakSelf emitVpnStateChanged];
              }];
}

- (void)stopObserving
{
  if (_neStatusObserver == nil) return;
  [[NSNotificationCenter defaultCenter] removeObserver:_neStatusObserver];
  _neStatusObserver = nil;
}

- (void)emitVpnStateChanged
{
  if (![self hasListeners]) return;

  NSString *providerBundleIdentifier = @"com.wireguardvpn.tunnel";
  [NETunnelProviderManager loadAllFromPreferencesWithCompletionHandler:^(NSArray<NETunnelProviderManager *> * _Nullable managers, NSError * _Nullable error) {
    if (error || managers == nil) return;

    NETunnelProviderManager *manager = nil;
    for (NETunnelProviderManager *m in managers) {
      if (![m.protocolConfiguration isKindOfClass:[NETunnelProviderProtocol class]]) continue;
      NETunnelProviderProtocol *p = (NETunnelProviderProtocol *)m.protocolConfiguration;
      if ([p.providerBundleIdentifier isEqualToString:providerBundleIdentifier]) {
        manager = m;
        break;
      }
    }

    if (!manager) return;

    NEVPNStatus status = manager.connection.status;
    NSString *tunnelState = [self stringFromVPNStatus:status];

    NSString *simpleStatus = nil;
    if (status == NEVPNStatusConnected) simpleStatus = @"CONNECTED";
    else if (status == NEVPNStatusDisconnected) simpleStatus = @"DISCONNECTED";
    else if (status == NEVPNStatusConnecting) simpleStatus = @"CONNECTING";
    else if (status == NEVPNStatusDisconnecting) simpleStatus = @"DISCONNECTING";
    else if (status == NEVPNStatusInvalid) simpleStatus = @"ERROR";
    else simpleStatus = @"UNKNOWN";

    NSDictionary *body = @{
      @"isConnected": @((status == NEVPNStatusConnected) ? YES : NO),
      @"tunnelState": tunnelState ?: @"UNKNOWN",
      @"status": simpleStatus ?: @"UNKNOWN"
    };

    [self sendEventWithName:@"vpnStateChanged" body:body];
  }];
}

RCT_EXPORT_METHOD(initialize:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  [NETunnelProviderManager loadAllFromPreferencesWithCompletionHandler:^(NSArray<NETunnelProviderManager *> * _Nullable managers, NSError * _Nullable error) {
    if (error) {
      reject(@"INIT_ERROR", error.localizedDescription, error);
      return;
    }
    
    NETunnelProviderManager *manager = managers.firstObject ?: [[NETunnelProviderManager alloc] init];
    manager.localizedDescription = @"WireGuard VPN";
    manager.protocolConfiguration = [[NETunnelProviderProtocol alloc] init];
    
    [manager saveToPreferencesWithCompletionHandler:^(NSError * _Nullable error) {
      if (error) {
        reject(@"INIT_ERROR", error.localizedDescription, error);
        return;
      }
      resolve(nil);
    }];
  }];
}

RCT_EXPORT_METHOD(connect:(NSDictionary *)config
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  // Validate required config early for clearer errors.
  NSString *serverAddress = config[@"serverAddress"];
  id serverPort = config[@"serverPort"];
  NSString *privateKey = config[@"privateKey"];
  NSString *publicKey = config[@"publicKey"];
  id allowedIPs = config[@"allowedIPs"];

  BOOL hasRequired =
      (serverAddress && ![serverAddress isKindOfClass:[NSNull class]]) &&
      (privateKey && ![privateKey isKindOfClass:[NSNull class]]) &&
      (publicKey && ![publicKey isKindOfClass:[NSNull class]]) &&
      serverPort &&
      (allowedIPs && [allowedIPs isKindOfClass:[NSArray class]] && [(NSArray *)allowedIPs count] > 0);

  if (!hasRequired) {
    reject(@"CONNECT_ERROR",
           @"Missing required config: serverAddress, serverPort, privateKey, publicKey, allowedIPs (non-empty array)",
           nil);
    return;
  }

  [NETunnelProviderManager loadAllFromPreferencesWithCompletionHandler:^(NSArray<NETunnelProviderManager *> * _Nullable managers, NSError * _Nullable error) {
    if (error) {
      reject(@"CONNECT_ERROR", error.localizedDescription, error);
      return;
    }
    
    NSString *providerBundleIdentifier = @"com.wireguardvpn.tunnel";

    // Pick the correct NETunnelProviderManager (do not assume firstObject).
    NETunnelProviderManager *manager = nil;
    for (NETunnelProviderManager *m in managers) {
      if (![m.protocolConfiguration isKindOfClass:[NETunnelProviderProtocol class]]) {
        continue;
      }
      NETunnelProviderProtocol *p = (NETunnelProviderProtocol *)m.protocolConfiguration;
      if ([p.providerBundleIdentifier isEqualToString:providerBundleIdentifier]) {
        manager = m;
        break;
      }
    }
    if (!manager) {
      manager = [[NETunnelProviderManager alloc] init];
    }

    manager.enabled = YES;

    NETunnelProviderProtocol *protocol = nil;
    if ([manager.protocolConfiguration isKindOfClass:[NETunnelProviderProtocol class]]) {
      protocol = (NETunnelProviderProtocol *)manager.protocolConfiguration;
    } else {
      protocol = [[NETunnelProviderProtocol alloc] init];
    }

    protocol.serverAddress = config[@"serverAddress"];
    protocol.providerBundleIdentifier = providerBundleIdentifier;
    
    NSMutableDictionary *tunnelConfig = [NSMutableDictionary dictionary];
    tunnelConfig[@"privateKey"] = config[@"privateKey"];
    tunnelConfig[@"publicKey"] = config[@"publicKey"];
    tunnelConfig[@"serverPort"] = config[@"serverPort"];
    tunnelConfig[@"allowedIPs"] = config[@"allowedIPs"];
    tunnelConfig[@"dns"] = config[@"dns"];
    tunnelConfig[@"mtu"] = config[@"mtu"];
    
    protocol.providerConfiguration = tunnelConfig;

    manager.protocolConfiguration = protocol;

    [manager saveToPreferencesWithCompletionHandler:^(NSError * _Nullable error) {
      if (error) {
        reject(@"CONNECT_ERROR", error.localizedDescription, error);
        return;
      }
      
      NSError *startError;
      [manager.connection startVPNTunnelAndReturnError:&startError];
      
      if (startError) {
        reject(@"CONNECT_ERROR", startError.localizedDescription, startError);
        return;
      }
      
      resolve(nil);
    }];
  }];
}

RCT_EXPORT_METHOD(disconnect:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  [NETunnelProviderManager loadAllFromPreferencesWithCompletionHandler:^(NSArray<NETunnelProviderManager *> * _Nullable managers, NSError * _Nullable error) {
    if (error) {
      reject(@"DISCONNECT_ERROR", error.localizedDescription, error);
      return;
    }
    
    NSString *providerBundleIdentifier = @"com.wireguardvpn.tunnel";
    NETunnelProviderManager *manager = nil;
    for (NETunnelProviderManager *m in managers) {
      if (![m.protocolConfiguration isKindOfClass:[NETunnelProviderProtocol class]]) continue;
      NETunnelProviderProtocol *p = (NETunnelProviderProtocol *)m.protocolConfiguration;
      if ([p.providerBundleIdentifier isEqualToString:providerBundleIdentifier]) {
        manager = m;
        break;
      }
    }
    
    if (!manager) {
      resolve(nil);
      return;
    }

    [manager.connection stopVPNTunnel];
    resolve(nil);
  }];
}

RCT_EXPORT_METHOD(getStatus:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  [NETunnelProviderManager loadAllFromPreferencesWithCompletionHandler:^(NSArray<NETunnelProviderManager *> * _Nullable managers, NSError * _Nullable error) {
    if (error) {
      reject(@"STATUS_ERROR", error.localizedDescription, error);
      return;
    }
    
    NSString *providerBundleIdentifier = @"com.wireguardvpn.tunnel";
    NETunnelProviderManager *manager = nil;
    for (NETunnelProviderManager *m in managers) {
      if (![m.protocolConfiguration isKindOfClass:[NETunnelProviderProtocol class]]) continue;
      NETunnelProviderProtocol *p = (NETunnelProviderProtocol *)m.protocolConfiguration;
      if ([p.providerBundleIdentifier isEqualToString:providerBundleIdentifier]) {
        manager = m;
        break;
      }
    }

    if (!manager) {
      resolve(@{
        @"isConnected": @NO,
        @"tunnelState": @"INACTIVE",
        @"status": @"DISCONNECTED",
      });
      return;
    }

    NEVPNStatus status = manager.connection.status;
    NSString *tunnelState = [self stringFromVPNStatus:status];
    NSString *simpleStatus = nil;
    if (status == NEVPNStatusConnected) simpleStatus = @"CONNECTED";
    else if (status == NEVPNStatusDisconnected) simpleStatus = @"DISCONNECTED";
    else if (status == NEVPNStatusConnecting) simpleStatus = @"CONNECTING";
    else if (status == NEVPNStatusDisconnecting) simpleStatus = @"DISCONNECTING";
    else if (status == NEVPNStatusInvalid) simpleStatus = @"ERROR";
    else simpleStatus = @"UNKNOWN";

    resolve(@{
      @"isConnected": @(status == NEVPNStatusConnected),
      @"tunnelState": tunnelState,
      @"status": simpleStatus
    });
  }];
}

RCT_EXPORT_METHOD(isSupported:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
  resolve(@YES);
}

- (NSString *)stringFromVPNStatus:(NEVPNStatus)status
{
  switch (status) {
    case NEVPNStatusConnected:
      return @"ACTIVE";
    case NEVPNStatusConnecting:
      return @"CONNECTING";
    case NEVPNStatusDisconnecting:
      return @"DISCONNECTING";
    case NEVPNStatusDisconnected:
      return @"INACTIVE";
    case NEVPNStatusInvalid:
      return @"ERROR";
    default:
      return @"UNKNOWN";
  }
}

@end 