#import "WireGuardVpn.h"
#import <NetworkExtension/NetworkExtension.h>

@implementation WireGuardVpn

RCT_EXPORT_MODULE()

- (NSArray<NSString *> *)supportedEvents
{
  return @[@"vpnStateChanged"];
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
  [NETunnelProviderManager loadAllFromPreferencesWithCompletionHandler:^(NSArray<NETunnelProviderManager *> * _Nullable managers, NSError * _Nullable error) {
    if (error) {
      reject(@"CONNECT_ERROR", error.localizedDescription, error);
      return;
    }
    
    NETunnelProviderManager *manager = managers.firstObject;
    if (!manager) {
      reject(@"CONNECT_ERROR", @"VPN manager not initialized", nil);
      return;
    }
    
    NETunnelProviderProtocol *protocol = (NETunnelProviderProtocol *)manager.protocolConfiguration;
    protocol.serverAddress = config[@"serverAddress"];
    protocol.providerBundleIdentifier = @"com.wireguardvpn.tunnel";
    
    NSMutableDictionary *tunnelConfig = [NSMutableDictionary dictionary];
    tunnelConfig[@"privateKey"] = config[@"privateKey"];
    tunnelConfig[@"publicKey"] = config[@"publicKey"];
    tunnelConfig[@"serverPort"] = config[@"serverPort"];
    tunnelConfig[@"allowedIPs"] = config[@"allowedIPs"];
    tunnelConfig[@"dns"] = config[@"dns"];
    tunnelConfig[@"mtu"] = config[@"mtu"];
    
    protocol.providerConfiguration = tunnelConfig;
    
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
    
    NETunnelProviderManager *manager = managers.firstObject;
    if (!manager) {
      reject(@"DISCONNECT_ERROR", @"VPN manager not initialized", nil);
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
    
    NETunnelProviderManager *manager = managers.firstObject;
    if (!manager) {
      resolve(@{
        @"isConnected": @NO,
        @"tunnelState": @"INACTIVE"
      });
      return;
    }
    
    NEVPNStatus status = manager.connection.status;
    resolve(@{
      @"isConnected": @(status == NEVPNStatusConnected),
      @"tunnelState": [self stringFromVPNStatus:status]
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