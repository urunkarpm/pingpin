import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:permission_handler/permission_handler.dart';
import '../services/ble_laptop_scanner_service.dart';

class BleLaptopScanState {
  final bool isScanning;
  final BleLaptopScanResult? result;
  final String? errorMessage;

  BleLaptopScanState({
    this.isScanning = false,
    this.result,
    this.errorMessage,
  });

  BleLaptopScanState copyWith({
    bool? isScanning,
    BleLaptopScanResult? result,
    String? errorMessage,
  }) {
    return BleLaptopScanState(
      isScanning: isScanning ?? this.isScanning,
      result: result ?? this.result,
      errorMessage: errorMessage,
    );
  }
}

class BleLaptopNotifier extends StateNotifier<BleLaptopScanState> {
  final BleLaptopScannerService _service;

  BleLaptopNotifier(this._service) : super(BleLaptopScanState());

  Future<void> startScan() async {
    state = state.copyWith(isScanning: true, errorMessage: null);

    try {
      // Request Bluetooth & Location permissions
      final bluetoothScanStatus = await Permission.bluetoothScan.request();
      final bluetoothConnectStatus = await Permission.bluetoothConnect.request();
      final locationStatus = await Permission.locationWhenInUse.request();

      if (bluetoothScanStatus.isDenied || bluetoothConnectStatus.isDenied || locationStatus.isDenied) {
        state = state.copyWith(
          isScanning: false,
          errorMessage: 'Bluetooth & Location permissions are required to scan for laptops.',
        );
        return;
      }

      final result = await _service.scanForLaptops();
      state = state.copyWith(
        isScanning: false,
        result: result,
      );
    } catch (e) {
      state = state.copyWith(
        isScanning: false,
        errorMessage: e.toString().replaceAll('Exception: ', ''),
      );
    }
  }
}

final bleLaptopScannerServiceProvider = Provider<BleLaptopScannerService>((ref) {
  return BleLaptopScannerService();
});

final bleLaptopNotifierProvider =
    StateNotifierProvider<BleLaptopNotifier, BleLaptopScanState>((ref) {
  final service = ref.watch(bleLaptopScannerServiceProvider);
  return BleLaptopNotifier(service);
});
