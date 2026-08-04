import 'package:flutter/foundation.dart';
import 'package:network_info_plus/network_info_plus.dart';
import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../data/database/app_database.dart';

/// Service to check WiFi connectivity, current SSID, and track historical SSIDs
class WifiService {
  static const String _wifiHistoryKey = 'known_wifi_ssids';
  final NetworkInfo _networkInfo = NetworkInfo();

  /// Gets the current WiFi SSID (returns null if not connected to WiFi or permission missing)
  Future<String?> getWifiSSID() async {
    try {
      // Directly fetch WiFi name from NetworkInfo
      final wifiName = await _networkInfo.getWifiName();

      if (wifiName != null) {
        final cleanSsid = wifiName.replaceAll('"', '').trim();
        if (cleanSsid.isNotEmpty &&
            cleanSsid.toLowerCase() != '<unknown ssid>' &&
            cleanSsid != '0x') {
          await addKnownSSID(cleanSsid);
          return cleanSsid;
        }
      }

      return null;
    } catch (e) {
      debugPrint('Error getting WiFi SSID: $e');
      return null;
    }
  }

  /// Adds an SSID to the list of historical connected Wi-Fi networks
  Future<void> addKnownSSID(String ssid) async {
    if (ssid.isEmpty || ssid == '<unknown ssid>') return;
    final prefs = await SharedPreferences.getInstance();
    final list = prefs.getStringList(_wifiHistoryKey) ?? [];
    if (!list.contains(ssid)) {
      list.add(ssid);
      await prefs.setStringList(_wifiHistoryKey, list);
    }
  }

  /// Retrieves recorded Wi-Fi SSIDs from Office Configs, SharedPreferences, and optionally current live connection
  Future<List<String>> getKnownSSIDs({AppDatabase? db, bool includeCurrentLive = false}) async {
    final resultList = <String>[];

    // 1. Fetch current live connection if explicitly requested
    if (includeCurrentLive) {
      final current = await getWifiSSID();
      if (current != null && current.isNotEmpty) {
        resultList.add(current);
      }
    }

    // 2. Fetch from stored Office Config in DB
    if (db != null) {
      final config = await db.getOfficeConfig();
      if (config != null && config.ssid.isNotEmpty && !resultList.contains(config.ssid)) {
        resultList.add(config.ssid);
      }
    }

    // 3. Fetch from SharedPreferences history
    final prefs = await SharedPreferences.getInstance();
    final prefsList = prefs.getStringList(_wifiHistoryKey) ?? [];
    for (final item in prefsList) {
      if (item.isNotEmpty && !resultList.contains(item)) {
        resultList.add(item);
      }
    }

    return resultList;
  }

  /// Checks if connected to a specific SSID
  Future<bool> isConnectedToSSID(String targetSSID) async {
    final currentSSID = await getWifiSSID();
    if (currentSSID == null) return false;
    
    // Case-insensitive comparison
    return currentSSID.toLowerCase() == targetSSID.toLowerCase();
  }

  /// Stream of Wi-Fi connection events
  Stream<List<ConnectivityResult>> get onConnectivityChanged => Connectivity().onConnectivityChanged;

  /// Checks if device is connected to any WiFi network
  Future<bool> isWiFiConnected() async {
    final connectivityResult = await Connectivity().checkConnectivity();
    return connectivityResult.contains(ConnectivityResult.wifi);
  }
}
