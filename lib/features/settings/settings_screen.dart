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
  TimeOfDay _workStartTime = TimeOfDay(hour: 9, minute: 30);
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
      _workStartTime = parseTimeString(config.workStartTime);
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
        designation: _designationController.text.trim(),
        employeeId: _employeeIdController.text.trim().isEmpty ? null : _employeeIdController.text.trim(),
        email: _emailController.text.trim().isEmpty ? null : _emailController.text.trim(),
        phone: _phoneController.text.trim().isEmpty ? null : _phoneController.text.trim(),
        photoPath: _photoPath,
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
        workStartTime: '${_workStartTime.hour.toString().padLeft(2, '0')}:${_workStartTime.minute.toString().padLeft(2, '0')}',
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
    final notificationSettings = ref.watch(notificationSettingsProvider);
    
    return Scaffold(
      appBar: AppBar(
        title: const Text('Settings'),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              physics: const BouncingScrollPhysics(),
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 100),

              children: [
                // Profile Section
                _buildSectionHeader('Profile'),
                Card(
                  child: Form(
                    key: _formKey,
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        children: [
                          GestureDetector(
                            onTap: () => _showPhotoSourceDialog(),
                            child: CircleAvatar(
                              radius: 50,
                              backgroundColor: AppColors.lightGray,
                              backgroundImage: _photoPath != null ? FileImage(File(_photoPath!)) : null,
                              child: _photoPath == null ? const Icon(Icons.camera_alt, size: 40) : null,
                            ),
                          ),
                          const SizedBox(height: 8),
                          TextButton(
                            onPressed: () => _showPhotoSourceDialog(),
                            child: const Text('Change Photo'),
                          ),
                          TextFormField(
                            controller: _fullNameController,
                            decoration: const InputDecoration(labelText: 'Full Name *'),
                            validator: (v) => v?.trim().isEmpty ?? true ? 'Required' : null,
                          ),
                          const SizedBox(height: 16),
                          TextFormField(
                            controller: _designationController,
                            decoration: const InputDecoration(labelText: 'Designation *'),
                            validator: (v) => v?.trim().isEmpty ?? true ? 'Required' : null,
                          ),
                          const SizedBox(height: 16),
                          TextFormField(
                            controller: _employeeIdController,
                            decoration: const InputDecoration(labelText: 'Employee ID'),
                          ),
                          const SizedBox(height: 16),
                          TextFormField(
                            controller: _emailController,
                            decoration: const InputDecoration(labelText: 'Email'),
                            keyboardType: TextInputType.emailAddress,
                          ),
                          const SizedBox(height: 16),
                          TextFormField(
                            controller: _phoneController,
                            decoration: const InputDecoration(labelText: 'Phone'),
                            keyboardType: TextInputType.phone,
                          ),
                          const SizedBox(height: 16),
                          ElevatedButton(
                            onPressed: _saveProfile,
                            child: const Text('Save Profile'),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
                
                const SizedBox(height: 24),
                
                // Office Config Section
                _buildSectionHeader('Office Configuration'),
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      children: [
                        FutureBuilder<List<String>>(
                          future: WifiService().getKnownSSIDs(db: ref.read(databaseProvider)),
                          builder: (context, snapshot) {
                            final knownList = snapshot.data ?? [];
                            final currentText = _ssidController.text.trim();
                            
                            // Combine options
                            final options = <String>{
                              if (currentText.isNotEmpty) currentText,
                              ...knownList,
                            }.toList();

                            return Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                if (options.isNotEmpty) ...[
                                  const Text(
                                    'Select Connected Wi-Fi Network',
                                    style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold),
                                  ),
                                  const SizedBox(height: 6),
                                  DropdownButtonFormField<String>(
                                    value: options.contains(currentText) ? currentText : (options.firstOrNull),
                                    decoration: const InputDecoration(
                                      prefixIcon: Icon(Icons.wifi),
                                      labelText: 'Saved Wi-Fi Networks',
                                    ),
                                    items: options.map((ssid) => DropdownMenuItem(
                                      value: ssid,
                                      child: Text(ssid),
                                    )).toList(),
                                    onChanged: (val) {
                                      if (val != null) {
                                        setState(() {
                                          _ssidController.text = val;
                                        });
                                      }
                                    },
                                  ),
                                  const SizedBox(height: 12),
                                ],
                                TextFormField(
                                  controller: _ssidController,
                                  decoration: const InputDecoration(
                                    labelText: 'WiFi SSID *',
                                    prefixIcon: Icon(Icons.edit),
                                  ),
                                ),
                              ],
                            );
                          },
                        ),
                        ListTile(
                          title: const Text('Work Start Time'),
                          subtitle: Text(_workStartTime.format(context)),
                          trailing: const Icon(Icons.access_time),
                          onTap: () => _selectTime(_workStartTime, (t) => setState(() => _workStartTime = t)),
                        ),


                        const SizedBox(height: 16),
                        const Text('Working Days:'),

                        Wrap(
                          spacing: 8,
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
                        const SizedBox(height: 16),
                        ElevatedButton(
                          onPressed: _saveOfficeConfig,
                          child: const Text('Save Office Config'),
                        ),
                      ],
                    ),
                  ),
                ),
                


                // Appearance Section
                _buildSectionHeader('Appearance'),
                Card(
                  child: Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text(
                          'Theme Mode',
                          style: TextStyle(fontSize: 16, fontWeight: FontWeight.w500),
                        ),
                        DropdownButton<ThemeMode>(
                          value: ref.watch(themeModeProvider),
                          underline: const SizedBox(),
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
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 16),

                // Background Run & Battery Optimization
                _buildSectionHeader('Background Run & Battery'),
                Card(
                  child: ListTile(
                    leading: const Icon(Icons.battery_saver, color: AppColors.accent),
                    title: const Text('Ignore Battery Optimizations'),
                    subtitle: const Text('Allow PingPin to detect Wi-Fi automatically in background'),
                    trailing: const Icon(Icons.chevron_right),
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

                _buildSectionHeader('Data'),
                Card(
                  child: Column(
                    children: [
                      ListTile(
                        leading: const Icon(Icons.delete_forever, color: Colors.red),
                        title: const Text('Reset All Data'),
                        subtitle: const Text('Clear all attendance records and settings'),
                        onTap: _resetData,
                      ),
                    ],
                  ),
                ),
              ],
            ),
    );
  }
  
  Widget _buildSectionHeader(String title) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Center(
        child: Text(
          title,
          textAlign: TextAlign.center,
          style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
        ),
      ),
    );
  }

  
  Widget _buildDayChip(String label, int bit) {
    final isSelected = _workingDaysMask & bit;
    return FilterChip(
      label: Text(label),
      selected: isSelected != 0,
      onSelected: (_) => _toggleWorkingDay(bit),
      selectedColor: AppColors.green.withOpacity(0.3),
      checkmarkColor: AppColors.green,
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
