import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';
import 'package:go_router/go_router.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../data/repositories/repositories.dart';
import '../../data/database/app_database.dart';
import '../../core/constants/app_constants.dart';
import '../../services/background_service.dart';
import '../../services/notification_service.dart';
import '../../services/wifi_service.dart';
import '../../providers/providers.dart';
import '../../core/utils/date_utils.dart';

class SettingsScreen extends ConsumerStatefulWidget {
  const SettingsScreen({super.key});

  @override
  ConsumerState<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends ConsumerState<SettingsScreen> {
  final _formKey = GlobalKey<FormState>();

  // Profile controllers
  late TextEditingController _fullNameController;
  late TextEditingController _designationController;
  late TextEditingController _employeeIdController;
  late TextEditingController _emailController;
  late TextEditingController _phoneController;
  String? _photoPath;

  // Office config
  late TextEditingController _ssidController;
  late TextEditingController _portalUrlController;
  double _radiusMeters = 100;
  TimeOfDay _lateCutoffTime = const TimeOfDay(hour: 10, minute: 30);
  TimeOfDay _checkInTime = const TimeOfDay(hour: 9, minute: 30);
  TimeOfDay _checkOutTime = const TimeOfDay(hour: 17, minute: 30);
  int _workingDaysMask = WorkingDays.defaultWeekdays;
  int _wfoDaysMask = WorkingDays.defaultWeekdays;
  double? _latitude;
  double? _longitude;

  // Change tracking
  String _initialFullName = '';
  String _initialDesignation = '';
  String? _initialPhotoPath;
  String _initialSsid = '';
  String _initialPortalUrl = '';
  TimeOfDay _initialCheckInTime = const TimeOfDay(hour: 9, minute: 30);
  TimeOfDay _initialCheckOutTime = const TimeOfDay(hour: 17, minute: 30);
  int _initialWorkingDaysMask = WorkingDays.defaultWeekdays;
  int _initialWfoDaysMask = WorkingDays.defaultWeekdays;

  bool _isLoading = true;
  bool _isSaving = false;
  final _imagePicker = ImagePicker();

  @override
  void initState() {
    super.initState();
    _fullNameController = TextEditingController();
    _designationController = TextEditingController();
    _employeeIdController = TextEditingController();
    _emailController = TextEditingController();
    _phoneController = TextEditingController();
    _portalUrlController = TextEditingController();
    _ssidController = TextEditingController();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);

    final profileRepo = ref.read(userProfileRepositoryProvider);
    final officeConfigRepo = ref.read(officeConfigRepositoryProvider);

    final profile = await profileRepo.getProfile();
    final config = await officeConfigRepo.getConfig();

    if (profile != null) {
      _fullNameController.text = profile.fullName;
      _designationController.text = profile.designation;
      _employeeIdController.text = profile.employeeId ?? '';
      _emailController.text = profile.email ?? '';
      _phoneController.text = profile.phone ?? '';
      _photoPath = profile.photoPath;
    }

    if (config != null) {
      _ssidController.text = config.ssid;
      _portalUrlController.text = config.portalUrl;
      _radiusMeters = config.radiusMeters.toDouble();
      _lateCutoffTime = parseTimeString(config.lateCutoffTime);
      _checkInTime = parseTimeString(config.checkInTime);
      _checkOutTime = parseTimeString(config.checkOutTime);
      _workingDaysMask = config.workingDaysMask;
      _wfoDaysMask = config.wfoDaysMask;
      _latitude = config.latitude;
      _longitude = config.longitude;
    }

    _updateInitialValues();

    if (mounted) {
      setState(() => _isLoading = false);
    }
  }

  void _updateInitialValues() {
    _initialFullName = _fullNameController.text.trim();
    _initialDesignation = _designationController.text.trim();
    _initialPhotoPath = _photoPath;
    _initialSsid = _ssidController.text.trim();
    _initialPortalUrl = _portalUrlController.text.trim();
    _initialCheckInTime = _checkInTime;
    _initialCheckOutTime = _checkOutTime;
    _initialWorkingDaysMask = _workingDaysMask;
    _initialWfoDaysMask = _wfoDaysMask;
  }

  bool get _hasChanges {
    if (_isLoading) return false;
    return _fullNameController.text.trim() != _initialFullName ||
        _designationController.text.trim() != _initialDesignation ||
        _photoPath != _initialPhotoPath ||
        _ssidController.text.trim() != _initialSsid ||
        _portalUrlController.text.trim() != _initialPortalUrl ||
        _checkInTime != _initialCheckInTime ||
        _checkOutTime != _initialCheckOutTime ||
        _workingDaysMask != _initialWorkingDaysMask ||
        _wfoDaysMask != _initialWfoDaysMask;
  }

  TimeOfDay parseTimeString(String timeStr) {
    final parts = timeStr.split(':');
    return TimeOfDay(
      hour: int.parse(parts[0]),
      minute: int.parse(parts[1]),
    );
  }

  String _format12Hour(TimeOfDay time) {
    final hour = time.hourOfPeriod == 0 ? 12 : time.hourOfPeriod;
    final period = time.period == DayPeriod.am ? 'AM' : 'PM';
    final minute = time.minute.toString().padLeft(2, '0');
    return '$hour:$minute $period';
  }

  Future<void> _saveAllSettings() async {
    if (!_formKey.currentState!.validate()) return;
    if (_ssidController.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Office Wi-Fi SSID is required')),
      );
      return;
    }

    setState(() => _isSaving = true);

    final profileRepo = ref.read(userProfileRepositoryProvider);
    final officeConfigRepo = ref.read(officeConfigRepositoryProvider);

    try {
      // 1. Save Profile
      final existingProfile = await profileRepo.getProfile();
      await profileRepo.saveProfile(
        id: existingProfile?.id,
        fullName: _fullNameController.text.trim(),
        designation: _designationController.text.trim(),
        employeeId: _employeeIdController.text.trim().isEmpty ? null : _employeeIdController.text.trim(),
        email: _emailController.text.trim().isEmpty ? null : _emailController.text.trim(),
        phone: _phoneController.text.trim().isEmpty ? null : _phoneController.text.trim(),
        photoPath: _photoPath,
      );

      // 2. Save Office Config
      final existingConfig = await officeConfigRepo.getConfig();
      final checkInStr =
          '${_checkInTime.hour.toString().padLeft(2, '0')}:${_checkInTime.minute.toString().padLeft(2, '0')}';
      final checkOutStr =
          '${_checkOutTime.hour.toString().padLeft(2, '0')}:${_checkOutTime.minute.toString().padLeft(2, '0')}';
      final lateCutoffStr =
          '${_lateCutoffTime.hour.toString().padLeft(2, '0')}:${_lateCutoffTime.minute.toString().padLeft(2, '0')}';

      final isWfoChanged = _wfoDaysMask != _initialWfoDaysMask;

      await officeConfigRepo.saveConfig(
        id: existingConfig?.id,
        ssid: _ssidController.text.trim(),
        latitude: _latitude ?? 0,
        longitude: _longitude ?? 0,
        radiusMeters: _radiusMeters.toInt(),
        lateCutoffTime: lateCutoffStr,
        checkInTime: checkInStr,
        checkOutTime: checkOutStr,
        portalUrl: _portalUrlController.text.trim(),
        workingDaysMask: _workingDaysMask,
        wfoDaysMask: _wfoDaysMask,
        updateWfoEffectiveNextWeek: isWfoChanged,
      );

      // 3. Reschedule alarms
      final updatedConfig = await officeConfigRepo.getConfig();
      if (updatedConfig != null) {
        await NotificationService().scheduleAlarmsFromConfig(updatedConfig);
      }

      _updateInitialValues();

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: const Row(
              children: [
                Icon(Icons.check_circle_rounded, color: Colors.white, size: 20),
                SizedBox(width: 10),
                Text('All settings saved & alarms rescheduled'),
              ],
            ),
            backgroundColor: AppColors.accent,
            behavior: SnackBarBehavior.floating,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Error saving settings: $e'),
            backgroundColor: Colors.redAccent,
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _isSaving = false);
      }
    }
  }

  Future<void> _pickPhoto(ImageSource source) async {
    try {
      final pickedFile = await _imagePicker.pickImage(source: source);
      if (pickedFile != null) {
        setState(() => _photoPath = pickedFile.path);
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
        return Theme(
          data: Theme.of(context).copyWith(
            timePickerTheme: TimePickerThemeData(
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
            ),
          ),
          child: child!,
        );
      },
    );
    if (picked != null) {
      onSelected(picked);
    }
  }

  Future<void> _resetData() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: const Row(
          children: [
            Icon(Icons.warning_amber_rounded, color: Colors.redAccent, size: 28),
            SizedBox(width: 10),
            Text('Reset All Data'),
          ],
        ),
        content: const Text(
          'This will permanently delete all attendance records, office configuration, profile details, and alarms, and restart initial setup. This action cannot be undone.',
          style: TextStyle(fontSize: 14, height: 1.4),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            style: FilledButton.styleFrom(
              backgroundColor: Colors.red,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
            ),
            child: const Text('Reset & Restart'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      await NotificationService().cancelAll();
      final db = ref.read(databaseProvider);
      await db.fullReset();
      final prefs = await SharedPreferences.getInstance();
      await prefs.clear();
      ref.read(onboardingCompleteProvider.notifier).state = false;

      if (mounted) {
        context.go('/onboarding');
      }
    }
  }

  @override
  void dispose() {
    _fullNameController.dispose();
    _designationController.dispose();
    _employeeIdController.dispose();
    _emailController.dispose();
    _phoneController.dispose();
    _ssidController.dispose();
    _portalUrlController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final isDark = theme.brightness == Brightness.dark;
    final canSave = _hasChanges && !_isSaving;

    return Scaffold(
      appBar: AppBar(
        title: const Text(
          'Settings',
          style: TextStyle(fontWeight: FontWeight.w700, fontSize: 20),
        ),
        centerTitle: false,
        elevation: 0,
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 16),
            child: FilledButton.icon(
              onPressed: canSave ? _saveAllSettings : null,
              icon: _isSaving
                  ? const SizedBox(
                      width: 14,
                      height: 14,
                      child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                    )
                  : const Icon(Icons.check_rounded, size: 18),
              label: const Text(
                'Save',
                style: TextStyle(fontWeight: FontWeight.w700, fontSize: 13.5),
              ),
              style: FilledButton.styleFrom(
                backgroundColor: canSave ? AppColors.accent : colorScheme.onSurface.withValues(alpha: 0.12),
                foregroundColor: canSave ? Colors.white : colorScheme.onSurface.withValues(alpha: 0.38),
                disabledBackgroundColor: colorScheme.onSurface.withValues(alpha: 0.12),
                disabledForegroundColor: colorScheme.onSurface.withValues(alpha: 0.38),
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                elevation: canSave ? 1 : 0,
              ),
            ),
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : Form(
              key: _formKey,
              child: ListView(
                physics: const BouncingScrollPhysics(),
                padding: const EdgeInsets.fromLTRB(16, 12, 16, 110),
                children: [
                  // 1. Profile Hero Section
                  _buildProfileCard(colorScheme, isDark),

                  const SizedBox(height: 24),

                  // 2. Battery & System Permissions Section (Top Page)
                  _buildSectionHeader('BATTERY & SYSTEM PERMISSIONS', Icons.battery_saver_rounded, colorScheme),
                  const SizedBox(height: 10),
                  _buildPermissionsCard(colorScheme),

                  const SizedBox(height: 24),

                  // 3. Office & Network Section
                  _buildSectionHeader('OFFICE & PORTAL', Icons.wifi_rounded, colorScheme),
                  const SizedBox(height: 10),
                  _buildOfficeCard(colorScheme),

                  const SizedBox(height: 24),

                  // 4. Work Schedule & Alarms
                  _buildSectionHeader('WORK SCHEDULE & ALARMS', Icons.schedule_rounded, colorScheme),
                  const SizedBox(height: 10),
                  _buildScheduleCard(colorScheme, isDark),

                  const SizedBox(height: 24),

                  // 5. Appearance Section
                  _buildSectionHeader('APPEARANCE', Icons.palette_outlined, colorScheme),
                  const SizedBox(height: 10),
                  _buildAppearanceCard(colorScheme),

                  const SizedBox(height: 24),

                  // 6. Danger Zone
                  _buildSectionHeader('DANGER ZONE', Icons.delete_forever_rounded, colorScheme, isDanger: true),
                  const SizedBox(height: 10),
                  _buildDangerZoneCard(colorScheme),
                ],
              ),
            ),
    );
  }

  // --- BUILD HELPERS ---

  Widget _buildSectionHeader(String title, IconData icon, ColorScheme colorScheme, {bool isDanger = false}) {
    final iconColor = isDanger ? Colors.redAccent : colorScheme.primary;
    final textColor = isDanger ? Colors.redAccent : colorScheme.onSurfaceVariant;

    return Padding(
      padding: const EdgeInsets.only(left: 4),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(5),
            decoration: BoxDecoration(
              color: iconColor.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Icon(icon, size: 14, color: iconColor),
          ),
          const SizedBox(width: 8),
          Text(
            title,
            style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w800,
              color: textColor,
              letterSpacing: 1.0,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCardContainer({required Widget child, required ColorScheme colorScheme, EdgeInsetsGeometry? padding}) {
    return Container(
      padding: padding ?? const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: colorScheme.surface,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: colorScheme.outlineVariant.withValues(alpha: 0.4)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.025),
            blurRadius: 10,
            offset: const Offset(0, 3),
          ),
        ],
      ),
      child: child,
    );
  }

  Widget _buildProfileCard(ColorScheme colorScheme, bool isDark) {
    final imageFile = _photoPath != null ? File(_photoPath!) : null;
    final hasPhoto = imageFile != null && imageFile.existsSync();

    return _buildCardContainer(
      colorScheme: colorScheme,
      child: Column(
        children: [
          Row(
            children: [
              GestureDetector(
                onTap: _showPhotoSourceDialog,
                child: Stack(
                  children: [
                    Container(
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        border: Border.all(
                          color: AppColors.accent.withValues(alpha: 0.5),
                          width: 2.5,
                        ),
                      ),
                      child: CircleAvatar(
                        radius: 36,
                        backgroundColor: colorScheme.surfaceContainerHighest,
                        backgroundImage: hasPhoto ? FileImage(imageFile) : null,
                        child: !hasPhoto
                            ? Icon(Icons.person_rounded, size: 40, color: colorScheme.onSurfaceVariant)
                            : null,
                      ),
                    ),
                    Positioned(
                      right: 0,
                      bottom: 0,
                      child: Container(
                        padding: const EdgeInsets.all(6),
                        decoration: BoxDecoration(
                          color: AppColors.accent,
                          shape: BoxShape.circle,
                          border: Border.all(color: Colors.white, width: 2),
                          boxShadow: const [
                            BoxShadow(
                              color: Colors.black26,
                              blurRadius: 4,
                            ),
                          ],
                        ),
                        child: const Icon(Icons.camera_alt_rounded, size: 14, color: Colors.white),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _fullNameController.text.trim().isNotEmpty
                          ? _fullNameController.text.trim()
                          : 'Your Profile',
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      _designationController.text.trim().isNotEmpty
                          ? _designationController.text.trim()
                          : 'Tap camera to upload avatar photo',
                      style: TextStyle(
                        fontSize: 12.5,
                        color: colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 18),
          const Divider(height: 1),
          const SizedBox(height: 16),
          TextFormField(
            controller: _fullNameController,
            onChanged: (_) => setState(() {}),
            decoration: InputDecoration(
              labelText: 'Full Name',
              hintText: 'e.g. Alex Johnson',
              prefixIcon: const Icon(Icons.person_outline_rounded, size: 20),
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: BorderSide(color: colorScheme.outlineVariant),
              ),
              contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
            ),
            validator: (v) => v?.trim().isEmpty ?? true ? 'Name is required' : null,
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: _designationController,
            onChanged: (_) => setState(() {}),
            decoration: InputDecoration(
              labelText: 'Designation / Role',
              hintText: 'e.g. Senior Developer',
              prefixIcon: const Icon(Icons.badge_outlined, size: 20),
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: BorderSide(color: colorScheme.outlineVariant),
              ),
              contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildOfficeCard(ColorScheme colorScheme) {
    return _buildCardContainer(
      colorScheme: colorScheme,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Wi-Fi Dropdown / Text input
          FutureBuilder<List<String>>(
            future: WifiService().getKnownSSIDs(db: ref.read(databaseProvider), includeCurrentLive: false),
            builder: (context, snapshot) {
              final knownList = snapshot.data ?? [];
              final currentText = _ssidController.text.trim();

              final options = <String>{
                if (currentText.isNotEmpty) currentText,
                ...knownList,
              }.toList();

              return Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: options.isNotEmpty
                            ? DropdownButtonFormField<String>(
                                initialValue: options.contains(currentText) ? currentText : options.firstOrNull,
                                decoration: InputDecoration(
                                  labelText: 'Office Wi-Fi SSID',
                                  prefixIcon: Icon(Icons.wifi_rounded, color: colorScheme.primary, size: 20),
                                  contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
                                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                                  enabledBorder: OutlineInputBorder(
                                    borderRadius: BorderRadius.circular(12),
                                    borderSide: BorderSide(color: colorScheme.outlineVariant),
                                  ),
                                ),
                                items: options
                                    .map((ssid) => DropdownMenuItem(
                                          value: ssid,
                                          child: Text(
                                            ssid,
                                            style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
                                          ),
                                        ))
                                    .toList(),
                                onChanged: (val) {
                                  if (val != null) {
                                    setState(() {
                                      _ssidController.text = val;
                                    });
                                  }
                                },
                              )
                            : TextFormField(
                                controller: _ssidController,
                                onChanged: (_) => setState(() {}),
                                decoration: InputDecoration(
                                  labelText: 'Office Wi-Fi SSID',
                                  hintText: 'e.g. Office_5G_Guest',
                                  prefixIcon: const Icon(Icons.wifi_rounded, size: 20),
                                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                                  enabledBorder: OutlineInputBorder(
                                    borderRadius: BorderRadius.circular(12),
                                    borderSide: BorderSide(color: colorScheme.outlineVariant),
                                  ),
                                ),
                              ),
                      ),
                      const SizedBox(width: 8),
                      IconButton.filledTonal(
                        tooltip: 'Fetch Current Connected Wi-Fi',
                        icon: const Icon(Icons.refresh_rounded),
                        onPressed: () async {
                          final currentSsid = await WifiService().getWifiSSID();
                          if (currentSsid != null && currentSsid.isNotEmpty) {
                            setState(() {
                              _ssidController.text = currentSsid;
                            });
                          }
                        },
                      ),
                    ],
                  ),
                ],
              );
            },
          ),
          const SizedBox(height: 14),
          // Portal URL field
          TextFormField(
            controller: _portalUrlController,
            onChanged: (_) => setState(() {}),
            keyboardType: TextInputType.url,
            validator: (value) {
              if (value != null && value.trim().isNotEmpty) {
                final trimmed = value.trim();
                final uri = Uri.tryParse(trimmed.startsWith('http') ? trimmed : 'https://$trimmed');
                if (uri == null || (!uri.isScheme('HTTP') && !uri.isScheme('HTTPS')) || uri.host.isEmpty) {
                  return 'Please enter a valid HTTP or HTTPS web URL';
                }
              }
              return null;
            },
            decoration: InputDecoration(
              labelText: 'Attendance Portal URL',
              hintText: 'https://portal.company.com/checkin',
              prefixIcon: const Icon(Icons.language_rounded, size: 20),
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: BorderSide(color: colorScheme.outlineVariant),
              ),
              contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildScheduleCard(ColorScheme colorScheme, bool isDark) {
    return _buildCardContainer(
      colorScheme: colorScheme,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: _buildTimePickerTile(
                  title: 'Check-in Alarm',
                  timeStr: _format12Hour(_checkInTime),
                  icon: Icons.login_rounded,
                  accentColor: AppColors.accent,
                  colorScheme: colorScheme,
                  onTap: () => _selectTime(
                    _checkInTime,
                    (t) => setState(() {
                      _checkInTime = t;
                      _checkOutTime = addHoursAndMinutes(t, 8, 32);
                    }),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _buildTimePickerTile(
                  title: 'Check-out Alarm',
                  timeStr: _format12Hour(_checkOutTime),
                  icon: Icons.logout_rounded,
                  accentColor: AppColors.blue,
                  colorScheme: colorScheme,
                  onTap: () => _selectTime(
                    _checkOutTime,
                    (t) => setState(() => _checkOutTime = t),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 18),
          const Divider(height: 1),
          const SizedBox(height: 14),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Working Schedule',
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w700,
                  color: colorScheme.onSurface,
                ),
              ),
              Text(
                'Tap to toggle',
                style: TextStyle(
                  fontSize: 11.5,
                  color: colorScheme.onSurfaceVariant,
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Wrap(
            spacing: 6,
            runSpacing: 8,
            children: [
              _buildMonochromeDayChip('Mon', WorkingDays.monday, colorScheme, isDark, isWfo: false),
              _buildMonochromeDayChip('Tue', WorkingDays.tuesday, colorScheme, isDark, isWfo: false),
              _buildMonochromeDayChip('Wed', WorkingDays.wednesday, colorScheme, isDark, isWfo: false),
              _buildMonochromeDayChip('Thu', WorkingDays.thursday, colorScheme, isDark, isWfo: false),
              _buildMonochromeDayChip('Fri', WorkingDays.friday, colorScheme, isDark, isWfo: false),
              _buildMonochromeDayChip('Sat', WorkingDays.saturday, colorScheme, isDark, isWfo: false),
              _buildMonochromeDayChip('Sun', WorkingDays.sunday, colorScheme, isDark, isWfo: false),
            ],
          ),
          const SizedBox(height: 18),
          const Divider(height: 1),
          const SizedBox(height: 14),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  const Icon(Icons.corporate_fare_rounded, size: 16, color: Color(0xFF3B82F6)),
                  const SizedBox(width: 6),
                  Text(
                    'WFO Schedule',
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: FontWeight.w700,
                      color: colorScheme.onSurface,
                    ),
                  ),
                ],
              ),
              Text(
                'Marked on calendar',
                style: TextStyle(
                  fontSize: 11.5,
                  color: colorScheme.onSurfaceVariant,
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Wrap(
            spacing: 6,
            runSpacing: 8,
            children: [
              _buildMonochromeDayChip('Mon', WorkingDays.monday, colorScheme, isDark, isWfo: true),
              _buildMonochromeDayChip('Tue', WorkingDays.tuesday, colorScheme, isDark, isWfo: true),
              _buildMonochromeDayChip('Wed', WorkingDays.wednesday, colorScheme, isDark, isWfo: true),
              _buildMonochromeDayChip('Thu', WorkingDays.thursday, colorScheme, isDark, isWfo: true),
              _buildMonochromeDayChip('Fri', WorkingDays.friday, colorScheme, isDark, isWfo: true),
              _buildMonochromeDayChip('Sat', WorkingDays.saturday, colorScheme, isDark, isWfo: true),
              _buildMonochromeDayChip('Sun', WorkingDays.sunday, colorScheme, isDark, isWfo: true),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildTimePickerTile({
    required String title,
    required String timeStr,
    required IconData icon,
    required Color accentColor,
    required ColorScheme colorScheme,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(14),
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: colorScheme.surfaceContainerHighest.withValues(alpha: 0.35),
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: colorScheme.outlineVariant.withValues(alpha: 0.5)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(icon, size: 16, color: accentColor),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    title,
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                      color: colorScheme.onSurfaceVariant,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  timeStr,
                  style: TextStyle(
                    fontSize: 17,
                    fontWeight: FontWeight.w800,
                    color: colorScheme.onSurface,
                    letterSpacing: -0.2,
                  ),
                ),
                Icon(Icons.edit_rounded, size: 14, color: colorScheme.onSurfaceVariant.withValues(alpha: 0.7)),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMonochromeDayChip(String label, int bit, ColorScheme colorScheme, bool isDark, {bool isWfo = false}) {
    final mask = isWfo ? _wfoDaysMask : _workingDaysMask;
    final isSelected = (mask & bit) != 0;

    final Color selectedBg;
    final Color selectedText;

    if (isWfo) {
      selectedBg = const Color(0xFF3B82F6);
      selectedText = Colors.white;
    } else {
      selectedBg = isDark ? Colors.white : AppColors.primary;
      selectedText = isDark ? AppColors.primary : Colors.white;
    }

    final unselectedBg = colorScheme.surfaceContainerHighest.withValues(alpha: 0.3);
    final unselectedText = colorScheme.onSurfaceVariant;

    return InkWell(
      onTap: () => isWfo ? _toggleWfoDay(bit) : _toggleWorkingDay(bit),
      borderRadius: BorderRadius.circular(10),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 150),
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        decoration: BoxDecoration(
          color: isSelected ? selectedBg : unselectedBg,
          borderRadius: BorderRadius.circular(10),
          border: Border.all(
            color: isSelected ? selectedBg : colorScheme.outlineVariant.withValues(alpha: 0.4),
          ),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 12.5,
            fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
            color: isSelected ? selectedText : unselectedText,
          ),
        ),
      ),
    );
  }

  Widget _buildAppearanceCard(ColorScheme colorScheme) {
    final currentMode = ref.watch(themeModeProvider);

    return _buildCardContainer(
      colorScheme: colorScheme,
      padding: const EdgeInsets.all(12),
      child: Row(
        children: [
          _buildThemeSegment('System', ThemeMode.system, Icons.brightness_auto_rounded, currentMode, colorScheme),
          const SizedBox(width: 8),
          _buildThemeSegment('Light', ThemeMode.light, Icons.light_mode_rounded, currentMode, colorScheme),
          const SizedBox(width: 8),
          _buildThemeSegment('Dark', ThemeMode.dark, Icons.dark_mode_rounded, currentMode, colorScheme),
        ],
      ),
    );
  }

  Widget _buildThemeSegment(
    String label,
    ThemeMode mode,
    IconData icon,
    ThemeMode currentMode,
    ColorScheme colorScheme,
  ) {
    final isSelected = currentMode == mode;

    return Expanded(
      child: InkWell(
        onTap: () => ref.read(themeModeProvider.notifier).setThemeMode(mode),
        borderRadius: BorderRadius.circular(12),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 180),
          padding: const EdgeInsets.symmetric(vertical: 10),
          decoration: BoxDecoration(
            color: isSelected ? colorScheme.primary : Colors.transparent,
            borderRadius: BorderRadius.circular(12),
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                icon,
                size: 20,
                color: isSelected ? colorScheme.onPrimary : colorScheme.onSurfaceVariant,
              ),
              const SizedBox(height: 4),
              Text(
                label,
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
                  color: isSelected ? colorScheme.onPrimary : colorScheme.onSurfaceVariant,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildPermissionsCard(ColorScheme colorScheme) {
    return FutureBuilder<bool>(
      future: BackgroundService.isBatteryOptimizationExempted(),
      builder: (context, snapshot) {
        final isExempted = snapshot.data ?? false;

        return _buildCardContainer(
          colorScheme: colorScheme,
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: (isExempted ? AppColors.accent : Colors.orange).withValues(alpha: 0.12),
                  shape: BoxShape.circle,
                ),
                child: Icon(
                  Icons.battery_saver_rounded,
                  color: isExempted ? AppColors.accent : Colors.orange,
                  size: 22,
                ),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        const Text(
                          'Battery Optimization',
                          style: TextStyle(fontSize: 14.5, fontWeight: FontWeight.w700),
                        ),
                        const SizedBox(width: 8),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                          decoration: BoxDecoration(
                            color: (isExempted ? AppColors.accent : Colors.orange).withValues(alpha: 0.15),
                            borderRadius: BorderRadius.circular(6),
                          ),
                          child: Text(
                            isExempted ? 'EXEMPTED' : 'OPTIMIZED',
                            style: TextStyle(
                              fontSize: 10,
                              fontWeight: FontWeight.w800,
                              color: isExempted ? AppColors.accent : Colors.orange,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 4),
                    Text(
                      isExempted
                          ? 'Background Wi-Fi attendance checks running uninterrupted.'
                          : 'Tap to allow background execution without system kill.',
                      style: TextStyle(fontSize: 12, color: colorScheme.onSurfaceVariant),
                    ),
                  ],
                ),
              ),
              IconButton(
                icon: Icon(
                  isExempted ? Icons.check_circle_rounded : Icons.tune_rounded,
                  color: isExempted ? AppColors.accent : colorScheme.primary,
                ),
                onPressed: () async {
                  final messenger = ScaffoldMessenger.of(context);
                  final isGranted = await BackgroundService.requestBatteryOptimizationExemption();
                  if (!isGranted) {
                    await openAppSettings();
                  }
                  if (mounted) {
                    setState(() {});
                    messenger.showSnackBar(
                      SnackBar(
                        content: Text(
                          isGranted
                              ? 'Battery optimization exemption granted!'
                              : 'Opening System Settings for Battery Optimization...',
                        ),
                        behavior: SnackBarBehavior.floating,
                      ),
                    );
                  }
                },
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildDangerZoneCard(ColorScheme colorScheme) {
    return _buildCardContainer(
      colorScheme: colorScheme,
      padding: EdgeInsets.zero,
      child: ListTile(
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
        leading: Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: Colors.red.withValues(alpha: 0.12),
            shape: BoxShape.circle,
          ),
          child: const Icon(Icons.delete_forever_rounded, color: Colors.redAccent, size: 20),
        ),
        title: const Text(
          'Reset All Data',
          style: TextStyle(fontSize: 14.5, fontWeight: FontWeight.w700, color: Colors.redAccent),
        ),
        subtitle: Text(
          'Delete attendance logs, configurations and restart onboarding',
          style: TextStyle(fontSize: 12, color: colorScheme.onSurfaceVariant),
        ),
        onTap: _resetData,
      ),
    );
  }

  void _showPhotoSourceDialog() {
    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 12),
          child: Wrap(
            children: [
              ListTile(
                leading: const Icon(Icons.photo_camera_rounded),
                title: const Text('Take Photo'),
                onTap: () {
                  Navigator.pop(context);
                  _pickPhoto(ImageSource.camera);
                },
              ),
              ListTile(
                leading: const Icon(Icons.photo_library_rounded),
                title: const Text('Choose from Gallery'),
                onTap: () {
                  Navigator.pop(context);
                  _pickPhoto(ImageSource.gallery);
                },
              ),
              if (_photoPath != null)
                ListTile(
                  leading: const Icon(Icons.delete_outline_rounded, color: Colors.redAccent),
                  title: const Text('Remove Photo', style: TextStyle(color: Colors.redAccent)),
                  onTap: () {
                    Navigator.pop(context);
                    setState(() => _photoPath = null);
                  },
                ),
            ],
          ),
        ),
      ),
    );
  }
}
