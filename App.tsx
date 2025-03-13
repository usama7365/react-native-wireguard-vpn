import React, { useState, useEffect } from 'react';
import {
  SafeAreaView,
  StyleSheet,
  View,
  Text,
  TouchableOpacity,
  Alert,
} from 'react-native';
import { WireGuardVpnModule } from 'react-native-wireguard-vpn';

const App = () => {
  const [isConnected, setIsConnected] = useState(false);
  const [status, setStatus] = useState('');

  useEffect(() => {
    initializeVPN();
  }, []);

  const initializeVPN = async () => {
    try {
      await WireGuardVpnModule.initialize();
      console.log('VPN initialized successfully');
    } catch (error) {
      console.error('VPN initialization failed:', error);
      Alert.alert('Error', 'Failed to initialize VPN');
    }
  };

  const connectVPN = async () => {
    try {
      const config = {
        privateKey: 'YOUR_PRIVATE_KEY', // Replace with your private key
        publicKey: 'YOUR_SERVER_PUBLIC_KEY', // Replace with your server's public key
        serverAddress: 'YOUR_SERVER_ADDRESS', // Replace with your server address
        serverPort: 51820,
        allowedIPs: ['0.0.0.0/0'],
        dns: ['8.8.8.8'],
        mtu: 1420,
      };

      await WireGuardVpnModule.connect(config);
      setIsConnected(true);
      Alert.alert('Success', 'VPN connected successfully');
    } catch (error) {
      console.error('VPN connection failed:', error);
      Alert.alert('Error', 'Failed to connect to VPN');
    }
  };

  const disconnectVPN = async () => {
    try {
      await WireGuardVpnModule.disconnect();
      setIsConnected(false);
      Alert.alert('Success', 'VPN disconnected successfully');
    } catch (error) {
      console.error('VPN disconnection failed:', error);
      Alert.alert('Error', 'Failed to disconnect VPN');
    }
  };

  const checkStatus = async () => {
    try {
      const vpnStatus = await WireGuardVpnModule.getStatus();
      setStatus(JSON.stringify(vpnStatus, null, 2));
      console.log('VPN Status:', vpnStatus);
    } catch (error) {
      console.error('Failed to get VPN status:', error);
      Alert.alert('Error', 'Failed to get VPN status');
    }
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.content}>
        <Text style={styles.title}>WireGuard VPN Test</Text>
        
        <TouchableOpacity
          style={[styles.button, isConnected ? styles.disconnectButton : styles.connectButton]}
          onPress={isConnected ? disconnectVPN : connectVPN}>
          <Text style={styles.buttonText}>
            {isConnected ? 'Disconnect VPN' : 'Connect VPN'}
          </Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.button}
          onPress={checkStatus}>
          <Text style={styles.buttonText}>Check Status</Text>
        </TouchableOpacity>

        {status ? (
          <View style={styles.statusContainer}>
            <Text style={styles.statusTitle}>VPN Status:</Text>
            <Text style={styles.statusText}>{status}</Text>
          </View>
        ) : null}
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  content: {
    flex: 1,
    padding: 20,
    alignItems: 'center',
    justifyContent: 'center',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: 30,
    color: '#333',
  },
  button: {
    backgroundColor: '#007AFF',
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderRadius: 8,
    marginVertical: 10,
    width: '80%',
  },
  connectButton: {
    backgroundColor: '#34C759',
  },
  disconnectButton: {
    backgroundColor: '#FF3B30',
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
    textAlign: 'center',
  },
  statusContainer: {
    marginTop: 20,
    padding: 15,
    backgroundColor: '#fff',
    borderRadius: 8,
    width: '90%',
  },
  statusTitle: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 10,
    color: '#333',
  },
  statusText: {
    fontSize: 14,
    color: '#666',
  },
});

export default App; 