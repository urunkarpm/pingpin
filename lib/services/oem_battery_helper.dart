import 'package:flutter/services.dart';

/// Detects OEM-specific battery restriction mechanisms and provides
/// deep-link intents to open the relevant settings screens directly.
///
/// Supported OEMs: Xiaomi/MIUI, Samsung OneUI, OnePlus/OxygenOS,
/// Vivo/FuntouchOS, Oppo/ColorOS, Huawei/EMUI.
class OemBatteryHelper {
  static const MethodChannel _channel =
      MethodChannel('com.urunkarpm.pingpin/oem_battery');

  static final Map<String, String> _cached = {};

  /// Call once at app start to cache device manufacturer/brand info.
  static Future<void> init() async {
    try {
      final Map result = await _channel.invokeMethod('getDeviceInfo');
      _cached['manufacturer'] =
          (result['manufacturer'] as String? ?? '').toLowerCase();
      _cached['brand'] =
          (result['brand'] as String? ?? '').toLowerCase();
    } catch (_) {
      // Non-critical — detectOem() will return null on failure
    }
  }

  /// Returns the detected OEM name, or null if stock Android / unknown.
  static String? detectOem() {
    final manufacturer = _cached['manufacturer'] ?? '';
    final brand = _cached['brand'] ?? '';

    if (manufacturer.contains('xiaomi') ||
        brand.contains('xiaomi') ||
        brand.contains('redmi') ||
        brand.contains('poco')) {
      return 'Xiaomi';
    }
    if (manufacturer.contains('samsung') || brand.contains('samsung')) {
      return 'Samsung';
    }
    if (manufacturer.contains('oneplus') || brand.contains('oneplus')) {
      return 'OnePlus';
    }
    if (manufacturer.contains('vivo') || brand.contains('vivo')) {
      return 'Vivo';
    }
    if (manufacturer.contains('oppo') ||
        brand.contains('oppo') ||
        brand.contains('realme')) {
      return 'Oppo/Realme';
    }
    if (manufacturer.contains('huawei') ||
        manufacturer.contains('honor') ||
        brand.contains('huawei') ||
        brand.contains('honor')) {
      return 'Huawei/Honor';
    }
    return null;
  }

  /// Returns OEM-specific step-by-step guidance, or null on stock Android.
  static OemBatteryGuidance? getGuidance() {
    final oem = detectOem();
    switch (oem) {
      case 'Xiaomi':
        return const OemBatteryGuidance(
          oemName: 'Xiaomi / MIUI',
          steps: [
            'Open Settings → Apps → Manage apps',
            'Search for "PingPin" and tap it',
            'Tap "Battery saver" → select "No restrictions"',
            'Go back → tap "Autostart" → toggle ON',
          ],
          intentAction: 'miui.intent.action.APP_PERM_EDITOR',
          intentPackage: 'com.miui.securitycenter',
        );
      case 'Samsung':
        return const OemBatteryGuidance(
          oemName: 'Samsung OneUI',
          steps: [
            'Open Settings → Battery',
            'Tap "Background usage limits"',
            'Tap "Never sleeping apps"',
            'Tap + and add "PingPin"',
          ],
          intentAction: 'android.intent.action.MAIN',
          intentPackage: 'com.samsung.android.lool',
          intentClass:
              'com.samsung.android.sm.battery.ui.BatteryActivity',
        );
      case 'OnePlus':
        return const OemBatteryGuidance(
          oemName: 'OnePlus / OxygenOS',
          steps: [
            'Open Settings → Battery → Battery Optimization',
            'Tap the dropdown → select "All apps"',
            'Find "PingPin" → tap → select "Don\'t optimize"',
          ],
          intentAction:
              'android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS',
        );
      case 'Vivo':
        return const OemBatteryGuidance(
          oemName: 'Vivo / FuntouchOS',
          steps: [
            'Open Settings → Battery → High background power consumption',
            'Enable "PingPin"',
            'Also: Settings → Apps → PingPin → Battery → "No restrictions"',
          ],
          intentAction: 'android.settings.APPLICATION_DETAILS_SETTINGS',
        );
      case 'Oppo/Realme':
        return const OemBatteryGuidance(
          oemName: 'Oppo / Realme / ColorOS',
          steps: [
            'Open Settings → Battery → "App quick freeze"',
            'Disable "PingPin" from the frozen list',
            'Settings → Apps → PingPin → Battery → "Allow background activity"',
          ],
          intentAction: 'android.settings.APPLICATION_DETAILS_SETTINGS',
        );
      case 'Huawei/Honor':
        return const OemBatteryGuidance(
          oemName: 'Huawei / Honor / EMUI',
          steps: [
            'Open Settings → Apps → Apps → PingPin',
            'Tap "Battery" → select "Run in background"',
            'Phone Manager → Protected apps → enable "PingPin"',
          ],
          intentAction: 'android.settings.APPLICATION_DETAILS_SETTINGS',
        );
      default:
        return null; // Stock Android — no extra guidance needed
    }
  }

  /// Attempts to launch the OEM-specific settings screen.
  /// Falls back to standard battery settings if the deep-link fails.
  static Future<void> launchOemSettings(OemBatteryGuidance guidance) async {
    try {
      await _channel.invokeMethod('launchOemSettings', {
        'action': guidance.intentAction,
        'package': guidance.intentPackage,
        'class': guidance.intentClass,
      });
    } catch (_) {
      // Fallback is handled on the native (Kotlin) side
    }
  }
}

class OemBatteryGuidance {
  final String oemName;
  final List<String> steps;
  final String intentAction;
  final String? intentPackage;
  final String? intentClass;

  const OemBatteryGuidance({
    required this.oemName,
    required this.steps,
    required this.intentAction,
    this.intentPackage,
    this.intentClass,
  });
}
