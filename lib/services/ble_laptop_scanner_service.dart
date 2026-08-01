import 'dart:async';
import 'package:flutter_blue_plus/flutter_blue_plus.dart';

enum DetectedLaptopOS { windows, macOS, unknown }

class DetectedLaptopDevice {
  final String id;
  final String name;
  final DetectedLaptopOS os;
  final int rssi;
  final DateTime lastSeen;

  DetectedLaptopDevice({
    required this.id,
    required this.name,
    required this.os,
    required this.rssi,
    required this.lastSeen,
  });
}

class BleLaptopScanResult {
  final int totalCount;
  final int windowsCount;
  final int macCount;
  final List<DetectedLaptopDevice> devices;
  final DateTime scannedAt;

  BleLaptopScanResult({
    required this.totalCount,
    required this.windowsCount,
    required this.macCount,
    required this.devices,
    required this.scannedAt,
  });
}

class BleLaptopScannerService {
  // Manufacturer IDs
  static const int microsoftVendorId = 0x0006;
  static const int appleVendorId = 0x004C;

  /// Scans for BLE devices for [scanDuration] and filters for laptop signatures.
  Future<BleLaptopScanResult> scanForLaptops({
    Duration scanDuration = const Duration(seconds: 5),
  }) async {
    // Check if Bluetooth is supported & turned on
    if (!await FlutterBluePlus.isSupported) {
      throw Exception('Bluetooth is not supported on this device.');
    }

    final adapterState = await FlutterBluePlus.adapterState.first;
    if (adapterState != BluetoothAdapterState.on) {
      throw Exception('Bluetooth is turned off. Please turn on Bluetooth to scan.');
    }

    final Map<String, DetectedLaptopDevice> detectedLaptops = {};

    // Listen to scan results
    final subscription = FlutterBluePlus.scanResults.listen((results) {
      for (ScanResult result in results) {
        final deviceId = result.device.remoteId.str;
        final manufacturerData = result.advertisementData.manufacturerData;

        DetectedLaptopOS? detectedOS;
        String defaultName = result.device.platformName;
        if (defaultName.isEmpty) {
          defaultName = result.advertisementData.advName;
        }

        // Filter out weak signals (distant devices / adjacent rooms)
        if (result.rssi < -82) {
          continue;
        }

        // 1. Check for Microsoft Vendor ID (0x0006) - Windows Swift Pair / Nearby Sharing
        if (manufacturerData.containsKey(microsoftVendorId)) {
          final bytes = manufacturerData[microsoftVendorId];
          // Windows Swift Pair sub-types (0x03, 0x08, 0x01)
          if (bytes != null && bytes.isNotEmpty) {
            final subType = bytes[0];
            if (subType == 0x03 || subType == 0x08 || subType == 0x01) {
              detectedOS = DetectedLaptopOS.windows;
              if (defaultName.isEmpty) {
                defaultName = 'Windows Laptop';
              }
            }
          }
        }
        // 2. Check for Apple Vendor ID (0x004C) - macOS Continuity / AirDrop / Handoff
        else if (manufacturerData.containsKey(appleVendorId)) {
          final bytes = manufacturerData[appleVendorId];
          if (bytes != null && bytes.length > 2) {
            final type = bytes[0];
            // Apple AirDrop (0x05) & Handoff (0x0C) are emitted primarily by active laptops/Macs in near range
            if (type == 0x05 || type == 0x0C) {
              detectedOS = DetectedLaptopOS.macOS;
              if (defaultName.isEmpty) {
                defaultName = 'MacBook / Mac';
              }
            }
          }
        }

        if (detectedOS != null) {
          detectedLaptops[deviceId] = DetectedLaptopDevice(
            id: deviceId,
            name: defaultName,
            os: detectedOS,
            rssi: result.rssi,
            lastSeen: DateTime.now(),
          );
        }
      }
    });

    // Start scanning
    await FlutterBluePlus.startScan(
      timeout: scanDuration,
      androidUsesFineLocation: true,
    );

    // Wait for scan to complete
    await Future.delayed(scanDuration);
    await subscription.cancel();

    final rawDevices = detectedLaptops.values.toList();
    final devicesList = _deduplicateDevices(rawDevices);

    final winCount = devicesList.where((d) => d.os == DetectedLaptopOS.windows).length;
    final macCount = devicesList.where((d) => d.os == DetectedLaptopOS.macOS).length;

    return BleLaptopScanResult(
      totalCount: devicesList.length,
      windowsCount: winCount,
      macCount: macCount,
      devices: devicesList,
      scannedAt: DateTime.now(),
    );
  }

  /// Deduplicates virtual MAC addresses originating from the same physical laptop.
  /// Laptops broadcast multiple virtual BLE addresses for different services simultaneously
  /// from the exact same physical distance (similar RSSI within +/- 4 dBm).
  List<DetectedLaptopDevice> _deduplicateDevices(List<DetectedLaptopDevice> rawDevices) {
    final List<DetectedLaptopDevice> deduplicated = [];

    for (final device in rawDevices) {
      final existingIndex = deduplicated.indexWhere((d) =>
          d.os == device.os && (d.rssi - device.rssi).abs() <= 4);

      if (existingIndex == -1) {
        deduplicated.add(device);
      } else {
        if (device.rssi > deduplicated[existingIndex].rssi) {
          deduplicated[existingIndex] = device;
        }
      }
    }

    return deduplicated;
  }
}
