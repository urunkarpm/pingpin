import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:image_picker/image_picker.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../data/repositories/repositories.dart';
import '../../data/database/app_database.dart';
import '../../core/constants/app_constants.dart';
import '../../services/wifi_service.dart';
import '../../services/notification_service.dart';
import '../../services/background_service.dart';
import '../../providers/providers.dart';

class OnboardingScreen extends ConsumerStatefulWidget {
  const OnboardingScreen({super.key});

  @override
  ConsumerState<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends ConsumerState<OnboardingScreen> {
  int _currentStep = 0;
  final _formKey = GlobalKey<FormState>();
  
  // Profile fields
  final _fullNameController = TextEditingController();
  String? _photoPath;
  
  // Office config & Alarms fields
  final _ssidController = TextEditingController();
  final _portalUrlController = TextEditingController();
  TimeOfDay _checkInTime = const TimeOfDay(hour: 9, minute: 30);
  TimeOfDay _checkOutTime = const TimeOfDay(hour: 17, minute: 30);
  int _workingDaysMask = WorkingDays.defaultWeekdays;
  
  bool _notificationGranted = false;
  bool _locationGranted = false;
  bool _batteryOptExempted = false;
  bool _isCustomSsid = false;
  
  final _wifiService = WifiService();
  final _imagePicker = ImagePicker();
  
  @override
  void initState() {
    super.initState();
    _checkPermissions();
    _refreshWifi();
  }

  Future<void> _checkPermissions() async {
    final notifStatus = await Permission.notification.status;
    final locStatus = await Permission.locationWhenInUse.status;
    final batteryExempt = await BackgroundService.isBatteryOptimizationExempted();
    if (mounted) {
      setState(() {
        _notificationGranted = notifStatus.isGranted;
        _locationGranted = locStatus.isGranted;
        _batteryOptExempted = batteryExempt;
      });
    }
  }

  Future<void> _requestBatteryOptimizationPermission() async {
    final isGranted = await BackgroundService.requestBatteryOptimizationExemption();
    if (mounted) {
      setState(() {
        _batteryOptExempted = isGranted;
      });
    }
  }

  Future<void> _requestLocationPermission() async {
    final status = await Permission.locationWhenInUse.request();
    if (mounted) {
      setState(() {
        _locationGranted = status.isGranted;
      });
    }
    await _refreshWifi();
  }

  Future<void> _refreshWifi() async {
    final locStatus = await Permission.locationWhenInUse.status;
    if (!locStatus.isGranted) {
      await Permission.locationWhenInUse.request();
    }
    final ssid = await _wifiService.getWifiSSID();
    if (ssid != null && ssid.isNotEmpty && mounted) {
      setState(() {
        _ssidController.text = ssid;
        _isCustomSsid = false;
      });
    } else if (mounted) {
      setState(() {});
    }
  }

  @override
  void dispose() {
    _fullNameController.dispose();
    _ssidController.dispose();
    _portalUrlController.dispose();
    super.dispose();
  }

  Future<void> _requestNotificationPermission() async {
    final status = await Permission.notification.request();
    setState(() {
      _notificationGranted = status.isGranted;
    });
  }
  
  Future<void> _pickPhoto(ImageSource source) async {
    try {
      final pickedFile = await _imagePicker.pickImage(source: source);
      if (pickedFile != null) {
        setState(() {
          _photoPath = pickedFile.path;
        });
      }
    } catch (e) {
      debugPrint('Error picking photo: $e');
    }
  }
  
  void _toggleWorkingDay(int dayBit) {
    setState(() {
      if ((_workingDaysMask & dayBit) != 0) {
        _workingDaysMask &= ~dayBit;
      } else {
        _workingDaysMask |= dayBit;
      }
    });
  }
  
  Future<void> _selectTime(TimeOfDay initialTime, Function(TimeOfDay) onSelected) async {
    final picked = await showTimePicker(
      context: context,
      initialTime: initialTime,
    );
    if (picked != null) {
      onSelected(picked);
    }
  }
  
  Future<void> _saveConfiguration() async {
    final fullName = _fullNameController.text.trim();
    if (fullName.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please enter your full name in Profile step')),
      );
      setState(() => _currentStep = 1);
      return;
    }

    final ssid = _ssidController.text.trim();
    if (ssid.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please enter office WiFi SSID')),
      );
      setState(() => _currentStep = 2);
      return;
    }
    
    final portalUrl = _portalUrlController.text.trim();
    final checkInStr = '${_checkInTime.hour.toString().padLeft(2, '0')}:${_checkInTime.minute.toString().padLeft(2, '0')}';
    final checkOutStr = '${_checkOutTime.hour.toString().padLeft(2, '0')}:${_checkOutTime.minute.toString().padLeft(2, '0')}';

    try {
      final profileRepo = ref.read(userProfileRepositoryProvider);
      final officeConfigRepo = ref.read(officeConfigRepositoryProvider);
      
      // Save profile
      await profileRepo.saveProfile(
        fullName: fullName,
        designation: '',
        employeeId: null,
        email: null,
        phone: null,
        photoPath: _photoPath,
      );
      
      // Save office config & alarms
      await officeConfigRepo.saveConfig(
        ssid: ssid,
        latitude: 0.0,
        longitude: 0.0,
        radiusMeters: 100,
        lateCutoffTime: checkInStr,
        checkInTime: checkInStr,
        checkOutTime: checkOutStr,
        portalUrl: portalUrl,
        workingDaysMask: _workingDaysMask,
      );
      
      // Save portal URL and schedule check-in and check-out alarms
      final notifService = NotificationService();
      await notifService.initialize(portalUrl: portalUrl);
      await notifService.scheduleCheckInAlarm(checkInTimeStr: checkInStr, portalUrl: portalUrl);
      await notifService.scheduleCheckOutAlarm(checkOutTimeStr: checkOutStr, portalUrl: portalUrl);

      // Mark onboarding as complete persistently
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool(AppKeys.onboardingComplete, true);
      ref.read(onboardingCompleteProvider.notifier).state = true;
      
      // Navigate to home
      if (mounted) {
        context.go('/');
      }

    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error saving configuration: $e')),
        );
      }
    }
  }
  
  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final themeMode = ref.watch(themeModeProvider);
    final isDark = themeMode == ThemeMode.dark ||
        (themeMode == ThemeMode.system &&
            MediaQuery.of(context).platformBrightness == Brightness.dark);

    return Scaffold(
      appBar: AppBar(
        title: Text(
          'SETUP PINGPIN',
          style: GoogleFonts.googleSans(
            fontWeight: FontWeight.w800,
            letterSpacing: 1.2,
            fontSize: 18,
          ),
        ),
        automaticallyImplyLeading: false,
        centerTitle: true,
        actions: [
          IconButton(
            icon: Icon(
              isDark ? Icons.light_mode_rounded : Icons.dark_mode_rounded,
            ),
            tooltip: isDark ? 'Switch to Light Mode' : 'Switch to Dark Mode',
            onPressed: () {
              ref.read(themeModeProvider.notifier).setThemeMode(
                    isDark ? ThemeMode.light : ThemeMode.dark,
                  );
            },
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: SafeArea(
        child: Column(
          children: [
            // E-Ink Custom Segmented Progress Header
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
              child: Row(
                children: [
                  _buildStepSegment(0, '01 WELCOME'),
                  const SizedBox(width: 8),
                  _buildStepSegment(1, '02 PROFILE'),
                  const SizedBox(width: 8),
                  _buildStepSegment(2, '03 SETUP'),
                ],
              ),
            ),
            
            Expanded(
              child: _buildStep(),
            ),
            
            // Navigation Bar
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                border: Border(
                  top: BorderSide(
                    color: colorScheme.onSurface.withValues(alpha: 0.2),
                    width: 1.5,
                  ),
                ),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  if (_currentStep > 0)
                    OutlinedButton.icon(
                      onPressed: () => setState(() => _currentStep--),
                      icon: const Icon(Icons.arrow_back, size: 18),
                      label: const Text('BACK'),
                    )
                  else
                    const SizedBox.shrink(),
                  ElevatedButton.icon(
                    onPressed: _currentStep == 2
                        ? _saveConfiguration
                        : () => setState(() => _currentStep++),
                    label: Text(_currentStep == 2 ? 'FINISH' : 'NEXT'),
                    icon: Icon(_currentStep == 2 ? Icons.check : Icons.arrow_forward, size: 18),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
  
  Widget _buildStepSegment(int stepIndex, String title) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final isActive = _currentStep == stepIndex;
    final isDone = _currentStep > stepIndex;

    return Expanded(
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 8),
        decoration: BoxDecoration(
          color: isActive 
              ? colorScheme.primary 
              : (isDone 
                  ? colorScheme.surfaceContainerHighest 
                  : colorScheme.surface),
          borderRadius: BorderRadius.circular(8),
          border: Border.all(
            color: colorScheme.onSurface,
            width: 1.5,
          ),
        ),
        child: Text(
          title,
          textAlign: TextAlign.center,
          style: GoogleFonts.googleSans(
            fontSize: 11,
            fontWeight: FontWeight.w700,
            color: isActive 
                ? colorScheme.onPrimary 
                : colorScheme.onSurface,
          ),
        ),
      ),
    );
  }

  Widget _buildStep() {
    switch (_currentStep) {
      case 0:
        return _buildPermissionsStep();
      case 1:
        return _buildProfileStep();
      case 2:
        return _buildOfficeWifiStep();
      default:
        return const SizedBox.shrink();
    }
  }
  
  Widget _buildPermissionsStep() {
    final colorScheme = Theme.of(context).colorScheme;
    final themeMode = ref.watch(themeModeProvider);
    final isDark = themeMode == ThemeMode.dark ||
        (themeMode == ThemeMode.system &&
            MediaQuery.of(context).platformBrightness == Brightness.dark);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          // Top right theme mode toggle chip
          Align(
            alignment: Alignment.centerRight,
            child: InkWell(
              onTap: () {
                ref.read(themeModeProvider.notifier).setThemeMode(
                      isDark ? ThemeMode.light : ThemeMode.dark,
                    );
              },
              borderRadius: BorderRadius.circular(20),
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                decoration: BoxDecoration(
                  color: colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(color: colorScheme.outlineVariant),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(
                      isDark ? Icons.light_mode_rounded : Icons.dark_mode_rounded,
                      size: 16,
                      color: colorScheme.primary,
                    ),
                    const SizedBox(width: 6),
                    Text(
                      isDark ? 'Light Mode' : 'Dark Mode',
                      style: GoogleFonts.googleSans(
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                        color: colorScheme.onSurface,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
          const SizedBox(height: 8),
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              border: Border.all(color: colorScheme.onSurface, width: 2),
            ),
            child: Icon(Icons.pin_drop, size: 48, color: colorScheme.onSurface),
          ),
          const SizedBox(height: 20),
          Text(
            'Welcome to PingPin',
            style: GoogleFonts.googleSans(
              fontSize: 26,
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            'Automatic office attendance tracking & portal check-in alarms.',
            textAlign: TextAlign.center,
            style: GoogleFonts.googleSans(
              fontSize: 14,
              color: colorScheme.onSurface.withValues(alpha: 0.7),
            ),
          ),
          const SizedBox(height: 32),

          // E-Ink Notification Permission Card
          Card(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(Icons.notifications_active, size: 28, color: colorScheme.primary),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          'Notification Permission',
                          style: GoogleFonts.googleSans(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                        decoration: BoxDecoration(
                          color: _notificationGranted 
                              ? colorScheme.primary.withValues(alpha: 0.15) 
                              : colorScheme.surfaceContainerHighest,
                          borderRadius: BorderRadius.circular(6),
                          border: Border.all(color: colorScheme.onSurface, width: 1),
                        ),
                        child: Text(
                          _notificationGranted ? 'GRANTED' : 'REQUIRED',
                          style: GoogleFonts.googleSans(
                            fontSize: 10,
                            fontWeight: FontWeight.w800,
                            color: colorScheme.onSurface,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Text(
                    'Used for silent Wi-Fi check-in alerts and interactive Check-In / Check-Out alarm reminders.',
                    style: GoogleFonts.googleSans(
                      fontSize: 13,
                      color: colorScheme.onSurface.withValues(alpha: 0.8),
                    ),
                  ),
                  const SizedBox(height: 16),
                  SizedBox(
                    width: double.infinity,
                    child: OutlinedButton.icon(
                      onPressed: _requestNotificationPermission,
                      icon: Icon(_notificationGranted ? Icons.check_circle : Icons.shield, size: 18),
                      label: Text(_notificationGranted ? 'PERMISSION GRANTED' : 'GRANT NOTIFICATION ACCESS'),
                    ),
                  ),
                ],
              ),
            ),
          ),
          // E-Ink Location & Wi-Fi Permission Card
          const SizedBox(height: 16),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(Icons.wifi_find_rounded, size: 28, color: colorScheme.primary),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          'Location & Wi-Fi Access',
                          style: GoogleFonts.googleSans(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                        decoration: BoxDecoration(
                          color: _locationGranted
                              ? colorScheme.primary.withValues(alpha: 0.15)
                              : colorScheme.surfaceContainerHighest,
                          borderRadius: BorderRadius.circular(6),
                          border: Border.all(color: colorScheme.onSurface, width: 1),
                        ),
                        child: Text(
                          _locationGranted ? 'GRANTED' : 'REQUIRED',
                          style: GoogleFonts.googleSans(
                            fontSize: 10,
                            fontWeight: FontWeight.w800,
                            color: colorScheme.onSurface,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Text(
                    'Required by Android OS to automatically detect your connected Office Wi-Fi network name (SSID).',
                    style: GoogleFonts.googleSans(
                      fontSize: 13,
                      color: colorScheme.onSurface.withValues(alpha: 0.8),
                    ),
                  ),
                  const SizedBox(height: 16),
                  SizedBox(
                    width: double.infinity,
                    child: OutlinedButton.icon(
                      onPressed: _requestLocationPermission,
                      icon: Icon(_locationGranted ? Icons.check_circle : Icons.location_on_outlined, size: 18),
                      label: Text(_locationGranted ? 'LOCATION ACCESS GRANTED' : 'GRANT LOCATION ACCESS'),
                    ),
                  ),
                ],
              ),
            ),
          ),
          // E-Ink Battery Optimization Card
          const SizedBox(height: 16),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(Icons.battery_saver_rounded, size: 28, color: colorScheme.primary),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          'Battery Optimization Exemption',
                          style: GoogleFonts.googleSans(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                        decoration: BoxDecoration(
                          color: _batteryOptExempted
                              ? colorScheme.primary.withValues(alpha: 0.15)
                              : colorScheme.surfaceContainerHighest,
                          borderRadius: BorderRadius.circular(6),
                          border: Border.all(color: colorScheme.onSurface, width: 1),
                        ),
                        child: Text(
                          _batteryOptExempted ? 'EXEMPTED' : 'RECOMMENDED',
                          style: GoogleFonts.googleSans(
                            fontSize: 10,
                            fontWeight: FontWeight.w800,
                            color: colorScheme.onSurface,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Text(
                    'Prevents Android OS from stopping background office Wi-Fi attendance checks when your device is idle.',
                    style: GoogleFonts.googleSans(
                      fontSize: 13,
                      color: colorScheme.onSurface.withValues(alpha: 0.8),
                    ),
                  ),
                  const SizedBox(height: 16),
                  SizedBox(
                    width: double.infinity,
                    child: OutlinedButton.icon(
                      onPressed: _requestBatteryOptimizationPermission,
                      icon: Icon(_batteryOptExempted ? Icons.check_circle : Icons.battery_charging_full_rounded, size: 18),
                      label: Text(_batteryOptExempted ? 'BATTERY OPTIMIZATION EXEMPTED' : 'ALLOW UNRESTRICTED BATTERY'),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
  
  Widget _buildProfileStep() {
    final colorScheme = Theme.of(context).colorScheme;

    return Form(
      key: _formKey,
      child: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          Text(
            'Your Profile',
            style: GoogleFonts.googleSans(
              fontSize: 22,
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            'Set up your name and optional profile picture.',
            style: GoogleFonts.googleSans(
              fontSize: 13,
              color: colorScheme.onSurface.withValues(alpha: 0.7),
            ),
          ),
          const SizedBox(height: 28),
          Center(
            child: GestureDetector(
              onTap: _showPhotoSourceDialog,
              child: Container(
                width: 100,
                height: 100,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: colorScheme.surface,
                  border: Border.all(color: colorScheme.onSurface, width: 2),
                  image: _photoPath != null 
                      ? DecorationImage(image: FileImage(File(_photoPath!)), fit: BoxFit.cover) 
                      : null,
                ),
                child: _photoPath == null
                    ? Icon(Icons.add_a_photo, size: 36, color: colorScheme.onSurface)
                    : null,
              ),
            ),
          ),
          const SizedBox(height: 24),
          TextFormField(
            controller: _fullNameController,
            decoration: const InputDecoration(
              labelText: 'Full Name *',
              prefixIcon: Icon(Icons.person),
            ),
            validator: (v) => v?.trim().isEmpty ?? true ? 'Required' : null,
          ),
        ],
      ),
    );
  }

  void _showPhotoSourceDialog() {
    showModalBottomSheet(
      context: context,
      builder: (context) => SafeArea(
        child: Wrap(
          children: [
            ListTile(
              leading: const Icon(Icons.camera),
              title: const Text('Take Photo'),
              onTap: () {
                Navigator.pop(context);
                _pickPhoto(ImageSource.camera);
              },
            ),
            ListTile(
              leading: const Icon(Icons.photo_library),
              title: const Text('Choose from Gallery'),
              onTap: () {
                Navigator.pop(context);
                _pickPhoto(ImageSource.gallery);
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildOfficeWifiStep() {
    final colorScheme = Theme.of(context).colorScheme;

    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        Text(
          'Office, Alarms & Portal',
          style: GoogleFonts.googleSans(
            fontSize: 22,
            fontWeight: FontWeight.w800,
          ),
        ),
        const SizedBox(height: 8),
        Text(
          'Configure your office network, daily alarm times, and portal link.',
          style: GoogleFonts.googleSans(
            fontSize: 13,
            color: colorScheme.onSurface.withValues(alpha: 0.7),
          ),
        ),
        const SizedBox(height: 16),

        if (!_locationGranted) ...[
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: colorScheme.primary.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: colorScheme.primary.withValues(alpha: 0.3)),
            ),
            child: Row(
              children: [
                Icon(Icons.location_on_outlined, color: colorScheme.primary, size: 22),
                const SizedBox(width: 10),
                Expanded(
                  child: Text(
                    'Location permission is required to auto-detect Wi-Fi name.',
                    style: GoogleFonts.googleSans(fontSize: 12, fontWeight: FontWeight.w500),
                  ),
                ),
                TextButton(
                  onPressed: _requestLocationPermission,
                  child: const Text('ALLOW'),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
        ],

        // Office Wi-Fi Network Dropdown
        FutureBuilder<List<String>>(
          future: _wifiService.getKnownSSIDs(),
          builder: (context, snapshot) {
            final knownList = snapshot.data ?? [];
            final currentText = _ssidController.text.trim();

            final options = <String>{
              if (currentText.isNotEmpty && !_isCustomSsid) currentText,
              ...knownList,
              '+ Enter Custom Wi-Fi SSID',
            }.toList();

            // Set default SSID if empty and knownList available
            if (_ssidController.text.isEmpty && knownList.isNotEmpty && !_isCustomSsid) {
              _ssidController.text = knownList.first;
            }

            final selectedValue = _isCustomSsid
                ? '+ Enter Custom Wi-Fi SSID'
                : (options.contains(_ssidController.text.trim())
                    ? _ssidController.text.trim()
                    : options.first);

            return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      'Office Wi-Fi Network *',
                      style: GoogleFonts.googleSans(
                        fontSize: 13,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    TextButton.icon(
                      onPressed: _refreshWifi,
                      icon: const Icon(Icons.refresh, size: 16),
                      label: const Text('Refresh Wi-Fi', style: TextStyle(fontSize: 12)),
                      style: TextButton.styleFrom(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                        minimumSize: Size.zero,
                        tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                DropdownButtonFormField<String>(
                  initialValue: options.contains(selectedValue) ? selectedValue : options.first,
                  decoration: const InputDecoration(
                    prefixIcon: Icon(Icons.wifi),
                    labelText: 'Select Office Wi-Fi Network',
                  ),
                  items: options
                      .map((ssid) => DropdownMenuItem(
                            value: ssid,
                            child: Text(
                              ssid,
                              style: TextStyle(
                                fontWeight: ssid == selectedValue
                                    ? FontWeight.bold
                                    : FontWeight.normal,
                              ),
                            ),
                          ))
                      .toList(),
                  onChanged: (val) {
                    if (val == null) return;
                    if (val == '+ Enter Custom Wi-Fi SSID') {
                      setState(() {
                        _isCustomSsid = true;
                        _ssidController.clear();
                      });
                    } else {
                      setState(() {
                        _isCustomSsid = false;
                        _ssidController.text = val;
                      });
                    }
                  },
                ),
                if (_isCustomSsid) ...[
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: _ssidController,
                    autofocus: true,
                    decoration: const InputDecoration(
                      labelText: 'Custom Wi-Fi Network Name (SSID) *',
                      prefixIcon: Icon(Icons.edit_outlined),
                      hintText: 'e.g., Office_WiFi_5G',
                    ),
                  ),
                ],
              ],
            );
          },
        ),
        const SizedBox(height: 20),

        // Portal URL
        TextFormField(
          controller: _portalUrlController,
          decoration: const InputDecoration(
            labelText: 'Company Portal URL',
            prefixIcon: Icon(Icons.language),
            hintText: 'e.g., https://portal.office.com',
          ),
        ),
        const SizedBox(height: 24),

        // Check-in and Check-out Times
        Text(
          'Alarm Times',
          style: GoogleFonts.googleSans(fontSize: 16, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(
              child: OutlinedButton.icon(
                onPressed: () => _selectTime(_checkInTime, (t) => setState(() => _checkInTime = t)),
                icon: const Icon(Icons.login, size: 18),
                label: Text('Check-in: ${_checkInTime.format(context)}'),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: OutlinedButton.icon(
                onPressed: () => _selectTime(_checkOutTime, (t) => setState(() => _checkOutTime = t)),
                icon: const Icon(Icons.logout, size: 18),
                label: Text('Check-out: ${_checkOutTime.format(context)}'),
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        SizedBox(
          width: double.infinity,
          child: OutlinedButton.icon(
            onPressed: () async {
              final portalUrl = _portalUrlController.text.trim();
              await NotificationService().testCheckInAlarmNow(portalUrl: portalUrl);
              if (mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(content: Text('⏰ Test alarm scheduled! It will ring in 5 seconds.')),
                );
              }
            },
            icon: const Icon(Icons.alarm_on, size: 18),
            label: const Text('TEST ALARM (RINGS IN 5 SEC)'),
          ),
        ),
        const SizedBox(height: 24),

        // Working Days Chips
        Text(
          'Working Days:',
          style: GoogleFonts.googleSans(fontSize: 14, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 10),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            _buildDayChip('Mon', WorkingDays.monday),
            _buildDayChip('Tue', WorkingDays.tuesday),
            _buildDayChip('Wed', WorkingDays.wednesday),
            _buildDayChip('Thu', WorkingDays.thursday),
            _buildDayChip('Fri', WorkingDays.friday),
            _buildDayChip('Sat', WorkingDays.saturday),
            _buildDayChip('Sun', WorkingDays.sunday),
          ],
        ),
      ],
    );
  }

  Widget _buildDayChip(String label, int bit) {
    final isSelected = (_workingDaysMask & bit) != 0;
    final colorScheme = Theme.of(context).colorScheme;

    return FilterChip(
      label: Text(label),
      labelStyle: TextStyle(
        fontSize: 12.5,
        fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
        color: isSelected ? colorScheme.primary : colorScheme.onSurfaceVariant,
      ),
      selected: isSelected,
      onSelected: (_) => _toggleWorkingDay(bit),
      backgroundColor: colorScheme.surfaceContainerHighest.withValues(alpha: 0.3),
      selectedColor: colorScheme.primary.withValues(alpha: 0.15),
      checkmarkColor: colorScheme.primary,
      side: BorderSide(
        color: isSelected ? colorScheme.primary.withValues(alpha: 0.4) : colorScheme.outlineVariant.withValues(alpha: 0.4),
      ),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
      padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
    );
  }
}
