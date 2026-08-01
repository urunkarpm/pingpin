import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';
import 'package:go_router/go_router.dart';
import 'package:permission_handler/permission_handler.dart';
import '../../data/repositories/repositories.dart';

import '../../data/database/app_database.dart';
import '../../core/constants/app_constants.dart';
import '../../services/background_service.dart';
import '../../services/wifi_service.dart';

import '../../providers/providers.dart';

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
  double _radiusMeters = 100;
  TimeOfDay _lateCutoffTime = TimeOfDay(hour: 10, minute: 30);
  int _workingDaysMask = WorkingDays.defaultWeekdays;
  double? _latitude;
  double? _longitude;
  
  bool _isLoading = true;
  final _imagePicker = ImagePicker();
  
  @override
  void initState() {
    super.initState();
    _loadData();
  }
  
  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    
    final profileRepo = ref.read(userProfileRepositoryProvider);
    final officeConfigRepo = ref.read(officeConfigRepositoryProvider);
    
    final profile = await profileRepo.getProfile();
    final config = await officeConfigRepo.getConfig();
    
    if (profile != null) {
      _fullNameController = TextEditingController(text: profile.fullName);
      _designationController = TextEditingController(text: profile.designation);
      _employeeIdController = TextEditingController(text: profile.employeeId ?? '');
      _emailController = TextEditingController(text: profile.email ?? '');
      _phoneController = TextEditingController(text: profile.phone ?? '');
      _photoPath = profile.photoPath;
    } else {
      _fullNameController = TextEditingController();
      _designationController = TextEditingController();
      _employeeIdController = TextEditingController();
      _emailController = TextEditingController();
      _phoneController = TextEditingController();
    }
    
    if (config != null) {
      _ssidController = TextEditingController(text: config.ssid);
      _radiusMeters = config.radiusMeters.toDouble();
      _lateCutoffTime = parseTimeString(config.lateCutoffTime);
      _workingDaysMask = config.workingDaysMask;
      _latitude = config.latitude;
      _longitude = config.longitude;
    } else {
      _ssidController = TextEditingController();
    }
    
    setState(() => _isLoading = false);
  }
  
  TimeOfDay parseTimeString(String timeStr) {
    final parts = timeStr.split(':');
    return TimeOfDay(
      hour: int.parse(parts[0]),
      minute: int.parse(parts[1]),
    );
  }
  
  Future<void> _saveProfile() async {
    if (!_formKey.currentState!.validate()) return;
    
    final profileRepo = ref.read(userProfileRepositoryProvider);
    
    try {
      await profileRepo.saveProfile(
        fullName: _fullNameController.text.trim(),
      );
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Profile saved successfully')),
        );
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error saving profile: $e')),
      );
    }
  }
  
  Future<void> _saveOfficeConfig() async {
    if (_ssidController.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('WiFi SSID is required')),
      );
      return;
    }
    
    final officeConfigRepo = ref.read(officeConfigRepositoryProvider);
    
    try {
      final existingConfig = await officeConfigRepo.getConfig();
      
      await officeConfigRepo.saveConfig(
        id: existingConfig?.id,
        ssid: _ssidController.text.trim(),
        latitude: _latitude ?? 0,
        longitude: _longitude ?? 0,
        radiusMeters: _radiusMeters.toInt(),
        lateCutoffTime: '${_lateCutoffTime.hour.toString().padLeft(2, '0')}:${_lateCutoffTime.minute.toString().padLeft(2, '0')}',
        workingDaysMask: _workingDaysMask,
      );
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Office configuration saved')),
        );
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error saving config: $e')),
      );
    }
  }
  
  Future<void> _pickPhoto(ImageSource source) async {
    try {
      final pickedFile = await _imagePicker.pickImage(source: source);
      if (pickedFile != null) {
        setState(() => _photoPath = pickedFile.path);
      }
    } catch (e) {
      print('Error picking photo: $e');
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
  
  Future<void> _resetData() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Reset All Data'),
        content: const Text('This will delete all attendance records and settings. This action cannot be undone.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(context, true),
            style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
            child: const Text('Reset'),
          ),
        ],
      ),
    );
    
    if (confirmed == true) {
      final db = ref.read(databaseProvider);
      await db.fullReset();
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
    super.dispose();
  }
  
  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    
    return Scaffold(
      appBar: AppBar(
        title: const Text('Settings'),
        centerTitle: false,
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              physics: const BouncingScrollPhysics(),
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 100),
              children: [
                // Profile Section
                _buildSectionHeader('PROFILE'),
                _buildCardGroup(
                  child: Form(
                    key: _formKey,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        TextFormField(
                          controller: _fullNameController,
                          decoration: InputDecoration(
                            labelText: 'Full Name',
                            prefixIcon: const Icon(Icons.person_outline_rounded, size: 20),
                            border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                            enabledBorder: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(12),
                              borderSide: BorderSide(color: colorScheme.outlineVariant),
                            ),
                          ),
                          validator: (v) => v?.trim().isEmpty ?? true ? 'Required' : null,
                        ),
                        const SizedBox(height: 16),
                        Align(
                          alignment: Alignment.centerRight,
                          child: FilledButton.icon(
                            onPressed: _saveProfile,
                            icon: const Icon(Icons.check_rounded, size: 18),
                            label: const Text('Save Profile'),
                            style: FilledButton.styleFrom(
                              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                
                const SizedBox(height: 24),
                
                // Office Config Section
                _buildSectionHeader('OFFICE CONFIGURATION'),
                _buildCardGroup(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      FutureBuilder<List<String>>(
                        future: WifiService().getKnownSSIDs(db: ref.read(databaseProvider)),
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
                              if (options.isNotEmpty) ...[
                                Text(
                                  'Saved Wi-Fi Networks',
                                  style: TextStyle(
                                    fontSize: 12,
                                    fontWeight: FontWeight.w600,
                                    color: colorScheme.onSurfaceVariant,
                                  ),
                                ),
                                const SizedBox(height: 8),
                                DropdownButtonFormField<String>(
                                  value: options.contains(currentText) ? currentText : options.firstOrNull,
                                  decoration: InputDecoration(
                                    prefixIcon: Icon(Icons.wifi_rounded, color: colorScheme.primary, size: 20),
                                    contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 14),
                                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                                    enabledBorder: OutlineInputBorder(
                                      borderRadius: BorderRadius.circular(12),
                                      borderSide: BorderSide(color: colorScheme.outlineVariant),
                                    ),
                                  ),
                                  items: options.map((ssid) => DropdownMenuItem(
                                    value: ssid,
                                    child: Text(ssid, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500)),
                                  )).toList(),
                                  onChanged: (val) {
                                    if (val != null) {
                                      setState(() {
                                        _ssidController.text = val;
                                      });
                                    }
                                  },
                                ),
                                const SizedBox(height: 16),
                              ],
                              TextFormField(
                                controller: _ssidController,
                                decoration: InputDecoration(
                                  labelText: 'Office Wi-Fi SSID',
                                  prefixIcon: const Icon(Icons.edit_outlined, size: 20),
                                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                                  enabledBorder: OutlineInputBorder(
                                    borderRadius: BorderRadius.circular(12),
                                    borderSide: BorderSide(color: colorScheme.outlineVariant),
                                  ),
                                ),
                              ),
                            ],
                          );
                        },
                      ),

                      const SizedBox(height: 20),
                      Text(
                        'Working Days',
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w600,
                          color: colorScheme.onSurfaceVariant,
                        ),
                      ),
                      const SizedBox(height: 10),
                      Wrap(
                        spacing: 6,
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
                      const SizedBox(height: 20),
                      Align(
                        alignment: Alignment.centerRight,
                        child: FilledButton.icon(
                          onPressed: _saveOfficeConfig,
                          icon: const Icon(Icons.save_outlined, size: 18),
                          label: const Text('Save Office Config'),
                          style: FilledButton.styleFrom(
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                
                const SizedBox(height: 24),

                // Appearance Section
                _buildSectionHeader('APPEARANCE'),
                _buildCardGroup(
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Row(
                        children: [
                          Icon(Icons.palette_outlined, color: colorScheme.onSurfaceVariant, size: 20),
                          const SizedBox(width: 12),
                          const Text(
                            'Theme Mode',
                            style: TextStyle(fontSize: 15, fontWeight: FontWeight.w500),
                          ),
                        ],
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 12),
                        decoration: BoxDecoration(
                          color: colorScheme.surfaceContainerHighest.withValues(alpha: 0.4),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: DropdownButtonHideUnderline(
                          child: DropdownButton<ThemeMode>(
                            value: ref.watch(themeModeProvider),
                            style: TextStyle(fontSize: 14, color: colorScheme.onSurface, fontWeight: FontWeight.w500),
                            items: const [
                              DropdownMenuItem(
                                value: ThemeMode.system,
                                child: Text('System Default'),
                              ),
                              DropdownMenuItem(
                                value: ThemeMode.light,
                                child: Text('Light Mode'),
                              ),
                              DropdownMenuItem(
                                value: ThemeMode.dark,
                                child: Text('Dark Mode'),
                              ),
                            ],
                            onChanged: (mode) {
                              if (mode != null) {
                                ref.read(themeModeProvider.notifier).setThemeMode(mode);
                              }
                            },
                          ),
                        ),
                      ),
                    ],
                  ),
                ),

                const SizedBox(height: 24),

                // Background Run & Battery Optimization
                _buildSectionHeader('BACKGROUND & PERMISSIONS'),
                _buildCardGroup(
                  padding: EdgeInsets.zero,
                  child: ListTile(
                    contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                    leading: Container(
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        color: AppColors.accent.withValues(alpha: 0.1),
                        shape: BoxShape.circle,
                      ),
                      child: const Icon(Icons.battery_saver_rounded, color: AppColors.accent, size: 20),
                    ),
                    title: const Text(
                      'Ignore Battery Optimizations',
                      style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600),
                    ),
                    subtitle: Text(
                      'Allows background Wi-Fi detection without system kill',
                      style: TextStyle(fontSize: 12, color: colorScheme.onSurfaceVariant),
                    ),
                    trailing: const Icon(Icons.chevron_right_rounded),
                    onTap: () async {
                      final isGranted = await BackgroundService.requestBatteryOptimizationExemption();
                      if (!isGranted) {
                        await openAppSettings();
                      }
                      if (context.mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(
                            content: Text(
                              isGranted
                                  ? 'Battery optimization exemption granted!'
                                  : 'Opening System Settings for Battery Optimization...',
                            ),
                          ),
                        );
                      }
                    },
                  ),
                ),
                
                const SizedBox(height: 24),
                
                // Data Management
                _buildSectionHeader('DATA MANAGEMENT'),
                _buildCardGroup(
                  padding: EdgeInsets.zero,
                  child: ListTile(
                    contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                    leading: Container(
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        color: Colors.red.withValues(alpha: 0.1),
                        shape: BoxShape.circle,
                      ),
                      child: const Icon(Icons.delete_forever_rounded, color: Colors.red, size: 20),
                    ),
                    title: const Text(
                      'Reset All Data',
                      style: TextStyle(fontSize: 15, fontWeight: FontWeight.w600, color: Colors.red),
                    ),
                    subtitle: Text(
                      'Permanently delete attendance history and configs',
                      style: TextStyle(fontSize: 12, color: colorScheme.onSurfaceVariant),
                    ),
                    onTap: _resetData,
                  ),
                ),
              ],
            ),
    );
  }
  
  Widget _buildSectionHeader(String title) {
    final colorScheme = Theme.of(context).colorScheme;
    return Padding(
      padding: const EdgeInsets.only(left: 4, bottom: 8),
      child: Text(
        title,
        style: TextStyle(
          fontSize: 11.5,
          fontWeight: FontWeight.w700,
          color: colorScheme.onSurfaceVariant,
          letterSpacing: 1.1,
        ),
      ),
    );
  }

  Widget _buildCardGroup({required Widget child, EdgeInsetsGeometry padding = const EdgeInsets.all(16)}) {
    final colorScheme = Theme.of(context).colorScheme;
    return Container(
      padding: padding,
      decoration: BoxDecoration(
        color: colorScheme.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: colorScheme.outlineVariant.withValues(alpha: 0.4)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.02),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: child,
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
}

