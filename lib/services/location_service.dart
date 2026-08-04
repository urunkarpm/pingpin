import 'package:flutter/foundation.dart';
import 'package:geolocator/geolocator.dart';
import 'package:permission_handler/permission_handler.dart';

/// Service for location-related operations
class LocationService {
  /// Checks if location permission is granted
  Future<bool> hasLocationPermission() async {
    final status = await Geolocator.checkPermission();
    return status == LocationPermission.whileInUse || 
           status == LocationPermission.always;
  }

  /// Requests location permission
  Future<LocationPermission> requestLocationPermission() async {
    return await Geolocator.requestPermission();
  }

  /// Gets current location
  Future<Position?> getCurrentLocation() async {
    try {
      final hasPerm = await hasLocationPermission();
      if (!hasPerm) {
        final permission = await requestLocationPermission();
        if (permission == LocationPermission.denied || 
            permission == LocationPermission.deniedForever) {
          return null;
        }
      }

      final permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.deniedForever) {
        return null;
      }

      return await Geolocator.getCurrentPosition(
        locationSettings: const LocationSettings(
          accuracy: LocationAccuracy.high,
          timeLimit: Duration(seconds: 10),
        ),
      );
    } catch (e) {
      debugPrint('Error getting location: $e');
      return null;
    }
  }

  /// Calculates distance between current location and target
  Future<double?> getDistanceToTarget(double targetLat, double targetLon) async {
    try {
      final currentLocation = await getCurrentLocation();
      if (currentLocation == null) return null;

      return Geolocator.distanceBetween(
        currentLocation.latitude,
        currentLocation.longitude,
        targetLat,
        targetLon,
      );
    } catch (e) {
      debugPrint('Error calculating distance: $e');
      return null;
    }
  }

  /// Checks if within radius of target location
  Future<bool> isWithinRadius({
    required double targetLat,
    required double targetLon,
    required double radiusMeters,
  }) async {
    final distance = await getDistanceToTarget(targetLat, targetLon);
    if (distance == null) return false;
    return distance <= radiusMeters;
  }

  /// Opens app settings for permission configuration
  Future<bool> openAppSettingsPage() async {
    return await openAppSettings();
  }
}
