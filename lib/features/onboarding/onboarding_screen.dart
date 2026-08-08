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
import '../../services/oem_battery_helper.dart';
import '../../providers/providers.dart';
import '../../core/utils/date_utils.dart';

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
  int _wfoDaysMask = WorkingDays.defaultWeekdays;
  
  bool _notificationGranted = false;
  bool _locationGranted = false;
  bool _batteryOptExempted = false;
  bool _isCustomSsid = false;
  
  final _wifiService = WifiService();
  final _imagePicker = ImagePicker();
  
  int _subStep = 0; // 0: Wifi, 1: Portal, 2: Days, 3: WFO Days, 4: Alarms
  bool _isMovingForward = true;

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

  void _toggleWfoDay(int dayBit) {
    setState(() {
      if ((_wfoDaysMask & dayBit) != 0) {
        _wfoDaysMask &= ~dayBit;
      } else {
        _wfoDaysMask |= dayBit;
      }
    });
  }
  
  Future<void> _selectTime(TimeOfDay initialTime, Function(TimeOfDay) onSelected) async {
    final picked = await showTimePicker(
      context: context,
      initialTime: initialTime,
      builder: (context, child) {
        return MediaQuery(
          data: MediaQuery.of(context).copyWith(alwaysUse24HourFormat: true),
          child: child!,
        );
      },
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
      setState(() {
        _currentStep = 2;
        _subStep = 0;
      });
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
        wfoDaysMask: _wfoDaysMask,
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

  void _handleBackNavigation() {
    setState(() {
      _isMovingForward = false;
      if (_currentStep == 2 && _subStep > 0) {
        _subStep--;
      } else if (_currentStep > 0) {
        _currentStep--;
        if (_currentStep == 2) {
          _subStep = 4;
        }
      }
    });
  }

  void _handleNextNavigation() {
    if (_currentStep == 2) {
      if (_subStep == 0 && _ssidController.text.trim().isEmpty) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Please select or enter office Wi-Fi network')),
        );
        return;
      }
      if (_subStep < 4) {
        setState(() {
          _isMovingForward = true;
          _subStep++;
        });
      } else {
        _saveConfiguration();
      }
    } else {
      setState(() {
        _isMovingForward = true;
        _currentStep++;
      });
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

    final isLastSubStep = _currentStep == 2 && _subStep == 4;
    final isFinalButton = _currentStep == 2 ? isLastSubStep : false;

    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, result) {
        if (didPop) return;
        if (_currentStep > 0 || (_currentStep == 2 && _subStep > 0)) {
          _handleBackNavigation();
        }
      },
      child: Scaffold(
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
                    if (_currentStep > 0 || (_currentStep == 2 && _subStep > 0))
                      OutlinedButton.icon(
                        onPressed: _handleBackNavigation,
                        icon: const Icon(Icons.arrow_back, size: 18),
                        label: const Text('BACK'),
                      )
                    else
                      const SizedBox.shrink(),
                    ElevatedButton.icon(
                      onPressed: _handleNextNavigation,
                      label: Text(isFinalButton ? 'FINISH' : 'NEXT'),
                      icon: Icon(isFinalButton ? Icons.check : Icons.arrow_forward, size: 18),
                    ),
                  ],
                ),
              ),
            ],
          ),
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
          const SizedBox(height: 16),
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

          // OEM Autostart Guidance Card (only shown on restricted OEMs)
          if (OemBatteryHelper.getGuidance() != null) ...[  
            const SizedBox(height: 16),
            _buildOemOnboardingCard(colorScheme, OemBatteryHelper.getGuidance()!),
          ],
        ],
      ),
    );
  }
  
  /// OEM autostart guidance card for onboarding — shown only on Xiaomi,
  /// Samsung, OnePlus, Vivo, Oppo, and Huawei devices.
  Widget _buildOemOnboardingCard(ColorScheme colorScheme, OemBatteryGuidance guidance) {
    return Card(
      color: Colors.deepOrange.withValues(alpha: 0.08),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(color: Colors.deepOrange.withValues(alpha: 0.4), width: 1.5),
      ),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.phone_android_rounded, size: 28, color: Colors.deepOrange),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    'Enable Autostart — ${guidance.oemName}',
                    style: GoogleFonts.googleSans(
                      fontSize: 15,
                      fontWeight: FontWeight.bold,
                      color: Colors.deepOrange,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),
            Text(
              '${guidance.oemName} aggressively kills background apps. Without Autostart, alarms will NOT fire when PingPin is closed.',
              style: GoogleFonts.googleSans(
                fontSize: 13,
                color: colorScheme.onSurface.withValues(alpha: 0.85),
              ),
            ),
            const SizedBox(height: 12),
            ...guidance.steps.asMap().entries.map((e) => Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    width: 22,
                    height: 22,
                    margin: const EdgeInsets.only(right: 10, top: 1),
                    decoration: BoxDecoration(
                      color: Colors.deepOrange.withValues(alpha: 0.15),
                      shape: BoxShape.circle,
                    ),
                    child: Center(
                      child: Text(
                        '${e.key + 1}',
                        style: const TextStyle(
                          fontSize: 11,
                          fontWeight: FontWeight.w800,
                          color: Colors.deepOrange,
                        ),
                      ),
                    ),
                  ),
                  Expanded(
                    child: Text(
                      e.value,
                      style: GoogleFonts.googleSans(fontSize: 13),
                    ),
                  ),
                ],
              ),
            )),
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              child: FilledButton.icon(
                style: FilledButton.styleFrom(backgroundColor: Colors.deepOrange),
                icon: const Icon(Icons.open_in_new_rounded, size: 18),
                label: Text(
                  'OPEN ${guidance.oemName.toUpperCase().split('/').first.trim()} SETTINGS',
                  style: GoogleFonts.googleSans(fontWeight: FontWeight.w700),
                ),
                onPressed: () => OemBatteryHelper.launchOemSettings(guidance),
              ),
            ),
          ],
        ),
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

    final subStepTitles = [
      'Office Wi-Fi',
      'Company Portal',
      'Work Days',
      'WFO Schedule',
      'Alarm Schedule',
    ];

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Stack Step Indicator Header
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Step 3: Setup (${_subStep + 1} of 5)',
                style: GoogleFonts.googleSans(
                  fontSize: 13,
                  fontWeight: FontWeight.w700,
                  color: colorScheme.primary,
                  letterSpacing: 0.5,
                ),
              ),
              Row(
                children: List.generate(5, (index) {
                  final isActive = index == _subStep;
                  final isDone = index < _subStep;
                  return Container(
                    margin: const EdgeInsets.only(left: 4),
                    width: isActive ? 20 : 8,
                    height: 8,
                    decoration: BoxDecoration(
                      color: isActive
                          ? colorScheme.primary
                          : (isDone
                              ? colorScheme.primary.withValues(alpha: 0.4)
                              : colorScheme.outlineVariant),
                      borderRadius: BorderRadius.circular(4),
                    ),
                  );
                }),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Text(
            subStepTitles[_subStep],
            style: GoogleFonts.googleSans(
              fontSize: 22,
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 16),

          // Card Stack with Smooth Fade & Slide Transition
          Expanded(
            child: AnimatedSwitcher(
              duration: const Duration(milliseconds: 320),
              switchInCurve: Curves.easeOutCubic,
              switchOutCurve: Curves.easeInCubic,
              transitionBuilder: (Widget child, Animation<double> animation) {
                final inTween = _isMovingForward
                    ? Tween<Offset>(begin: const Offset(0, 0.15), end: Offset.zero)
                    : Tween<Offset>(begin: const Offset(0, -0.15), end: Offset.zero);
                final outTween = _isMovingForward
                    ? Tween<Offset>(begin: Offset.zero, end: const Offset(0, -0.15))
                    : Tween<Offset>(begin: Offset.zero, end: const Offset(0, 0.15));

                if (child.key == ValueKey<int>(_subStep)) {
                  return FadeTransition(
                    opacity: animation,
                    child: SlideTransition(
                      position: animation.drive(inTween),
                      child: child,
                    ),
                  );
                } else {
                  return FadeTransition(
                    opacity: animation,
                    child: SlideTransition(
                      position: animation.drive(outTween),
                      child: child,
                    ),
                  );
                }
              },
              child: SizedBox(
                key: ValueKey<int>(_subStep),
                width: double.infinity,
                child: _buildSubStepCard(_subStep),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSubStepCard(int subStep) {
    switch (subStep) {
      case 0:
        return _buildWifiSubCard();
      case 1:
        return _buildPortalSubCard();
      case 2:
        return _buildWorkDaysSubCard();
      case 3:
        return _buildWfoDaysSubCard();
      case 4:
        return _buildAlarmsSubCard();
      default:
        return const SizedBox.shrink();
    }
  }

  Widget _buildWfoDaysSubCard() {
    final colorScheme = Theme.of(context).colorScheme;

    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: const Color(0xFF3B82F6).withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: const Icon(Icons.corporate_fare_rounded, color: Color(0xFF3B82F6), size: 28),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'WFO Schedule',
                          style: GoogleFonts.googleSans(
                            fontSize: 17,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        Text(
                          'Select days when you work from office (marked on calendar)',
                          style: GoogleFonts.googleSans(
                            fontSize: 12,
                            color: colorScheme.onSurface.withValues(alpha: 0.7),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 24),
              Text(
                'WFO Days:',
                style: GoogleFonts.googleSans(fontSize: 14, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 12),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  _buildWfoDayChip('Mon', WorkingDays.monday),
                  _buildWfoDayChip('Tue', WorkingDays.tuesday),
                  _buildWfoDayChip('Wed', WorkingDays.wednesday),
                  _buildWfoDayChip('Thu', WorkingDays.thursday),
                  _buildWfoDayChip('Fri', WorkingDays.friday),
                  _buildWfoDayChip('Sat', WorkingDays.saturday),
                  _buildWfoDayChip('Sun', WorkingDays.sunday),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildWfoDayChip(String label, int bit) {
    final isSelected = (_wfoDaysMask & bit) != 0;
    final colorScheme = Theme.of(context).colorScheme;
    const activeColor = Color(0xFF3B82F6);

    return FilterChip(
      label: Text(label),
      labelStyle: TextStyle(
        fontSize: 12.5,
        fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
        color: isSelected ? activeColor : colorScheme.onSurfaceVariant,
      ),
      selected: isSelected,
      onSelected: (_) => _toggleWfoDay(bit),
      backgroundColor: colorScheme.surfaceContainerHighest.withValues(alpha: 0.3),
      selectedColor: activeColor.withValues(alpha: 0.15),
      checkmarkColor: activeColor,
      side: BorderSide(
        color: isSelected ? activeColor.withValues(alpha: 0.5) : colorScheme.outlineVariant.withValues(alpha: 0.4),
      ),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
      padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
    );
  }

  Widget _buildWifiSubCard() {
    final colorScheme = Theme.of(context).colorScheme;

    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: colorScheme.primary.withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Icon(Icons.wifi_rounded, color: colorScheme.primary, size: 28),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Connect Office Wi-Fi',
                          style: GoogleFonts.googleSans(
                            fontSize: 17,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        Text(
                          'Used for silent check-in upon arrival',
                          style: GoogleFonts.googleSans(
                            fontSize: 12,
                            color: colorScheme.onSurface.withValues(alpha: 0.7),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 20),

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
                      Icon(Icons.location_on_outlined, color: colorScheme.primary, size: 20),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Text(
                          'Location permission is required to detect Wi-Fi name.',
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

              // Known SSIDs Dropdown
              FutureBuilder<List<String>>(
                future: _wifiService.getKnownSSIDs(includeCurrentLive: false),
                builder: (context, snapshot) {
                  final knownList = snapshot.data ?? [];
                  final currentText = _ssidController.text.trim();

                  final options = <String>{
                    if (currentText.isNotEmpty && !_isCustomSsid) currentText,
                    ...knownList,
                    '+ Enter Custom Wi-Fi SSID',
                  }.toList();

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
                            'Select Office Wi-Fi *',
                            style: GoogleFonts.googleSans(
                              fontSize: 13,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          TextButton.icon(
                            onPressed: _refreshWifi,
                            icon: const Icon(Icons.refresh, size: 16),
                            label: const Text('Refresh', style: TextStyle(fontSize: 12)),
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
                          labelText: 'Network Name (SSID)',
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
                            labelText: 'Custom Network Name (SSID) *',
                            prefixIcon: Icon(Icons.edit_outlined),
                            hintText: 'e.g., Office_WiFi_5G',
                          ),
                        ),
                      ],
                    ],
                  );
                },
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildPortalSubCard() {
    final colorScheme = Theme.of(context).colorScheme;

    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: colorScheme.primary.withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Icon(Icons.language_rounded, color: colorScheme.primary, size: 28),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Company Portal Link',
                          style: GoogleFonts.googleSans(
                            fontSize: 17,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        Text(
                          'Quick access when alarm notification fires',
                          style: GoogleFonts.googleSans(
                            fontSize: 12,
                            color: colorScheme.onSurface.withValues(alpha: 0.7),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 24),
              TextFormField(
                controller: _portalUrlController,
                decoration: const InputDecoration(
                  labelText: 'Attendance Portal Web URL',
                  prefixIcon: Icon(Icons.link),
                  hintText: 'e.g., https://portal.company.com',
                ),
              ),
              const SizedBox(height: 12),
              Text(
                'Optional: PingPin can open your official attendance portal directly from the check-in alarm notification action button.',
                style: GoogleFonts.googleSans(
                  fontSize: 12,
                  color: colorScheme.onSurface.withValues(alpha: 0.6),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildWorkDaysSubCard() {
    final colorScheme = Theme.of(context).colorScheme;

    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: colorScheme.primary.withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Icon(Icons.calendar_today_rounded, color: colorScheme.primary, size: 28),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Working Schedule',
                          style: GoogleFonts.googleSans(
                            fontSize: 17,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        Text(
                          'Select days when alarms and tracking are active',
                          style: GoogleFonts.googleSans(
                            fontSize: 12,
                            color: colorScheme.onSurface.withValues(alpha: 0.7),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 24),
              Text(
                'Active Days:',
                style: GoogleFonts.googleSans(fontSize: 14, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 12),
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
          ),
        ),
      ),
    );
  }

  Widget _buildAlarmsSubCard() {
    final colorScheme = Theme.of(context).colorScheme;

    return Card(
      elevation: 2,
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: colorScheme.primary.withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Icon(Icons.alarm_rounded, color: colorScheme.primary, size: 28),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Check-in & Check-out Alarms',
                          style: GoogleFonts.googleSans(
                            fontSize: 17,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        Text(
                          'Set your preferred reminder alarm times',
                          style: GoogleFonts.googleSans(
                            fontSize: 12,
                            color: colorScheme.onSurface.withValues(alpha: 0.7),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 24),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: () => _selectTime(
                        _checkInTime,
                        (t) => setState(() {
                          _checkInTime = t;
                          _checkOutTime = addHoursAndMinutes(t, 8, 32);
                        }),
                      ),
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
            ],
          ),
        ),
      ),
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

