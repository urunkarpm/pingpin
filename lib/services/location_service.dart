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
      // Check permission first
      final permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        final requested = await Geolocator.requestPermission();
        if (requested == LocationPermission.denied) {
          return null;
        }
      }
      
      if (permission == LocationPermission.deniedForever) {
        return null;
      }

      return await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
        timeLimit: const Duration(seconds: 10),
      );
    } catch (e) {
      print('Error getting location: $e');
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
      print('Error calculating distance: $e');
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
