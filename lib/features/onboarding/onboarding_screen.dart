import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:image_picker/image_picker.dart';
import 'package:go_router/go_router.dart';
import 'package:geolocator/geolocator.dart';
import '../../data/repositories/repositories.dart';
import '../../data/database/app_database.dart';
import '../../core/constants/app_constants.dart';
import '../../services/location_service.dart';
import '../../services/wifi_service.dart';
import 'package:shared_preferences/shared_preferences.dart';
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
  
  // Office config fields
  final _ssidController = TextEditingController();
  final TimeOfDay _lateCutoffTime = const TimeOfDay(hour: 10, minute: 30);
  int _workingDaysMask = WorkingDays.defaultWeekdays;
  
  final _locationService = LocationService();
  final _wifiService = WifiService();
  final _imagePicker = ImagePicker();
  
  @override
  void dispose() {
    _fullNameController.dispose();
    _ssidController.dispose();
    super.dispose();
  }
  
  Future<void> _requestPermissions() async {
    // Request location permission
    await Geolocator.requestPermission();
    
    // Request notification permission (Android 13+)
    await Permission.notification.request();
    
    // Request photos permission
    await Permission.photos.request();
    await Permission.camera.request();
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
      
      // Save office config
      await officeConfigRepo.saveConfig(
        ssid: ssid,
        latitude: 0.0,
        longitude: 0.0,
        radiusMeters: 100,
        lateCutoffTime: '${_lateCutoffTime.hour.toString().padLeft(2, '0')}:${_lateCutoffTime.minute.toString().padLeft(2, '0')}',
        workingDaysMask: _workingDaysMask,
      );
      
      // Mark onboarding as complete persistently
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool(AppKeys.onboardingComplete, true);
      ref.read(onboardingCompleteProvider.notifier).state = true;
      
      // Navigate to home
      if (mounted) {
        context.go('/');
      }

    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error saving configuration: $e')),
      );
    }
  }
  
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Setup PingPin'),
        automaticallyImplyLeading: false,
      ),
      body: Column(
        children: [
          // Progress indicator
          LinearProgressIndicator(value: (_currentStep + 1) / 3),
          
          Expanded(
            child: _buildStep(),
          ),
          
          // Navigation buttons
          Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                if (_currentStep > 0)
                  ElevatedButton(
                    onPressed: () => setState(() => _currentStep--),
                    child: const Text('Back'),
                  )
                else
                  const SizedBox.shrink(),
                ElevatedButton(
                  onPressed: _currentStep == 2 ? _saveConfiguration : () => setState(() => _currentStep++),
                  child: Text(_currentStep == 2 ? 'Finish' : 'Next'),
                ),
              ],
            ),
          ),
        ],
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
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Center(
            child: Image.asset(
              'logo.png',
              height: 90,
              errorBuilder: (context, error, stackTrace) => const Icon(Icons.pin_drop, size: 70),
            ),
          ),
          const SizedBox(height: 16),
          const Center(
            child: Text(
              'Welcome to PingPin',
              style: TextStyle(fontSize: 28, fontWeight: FontWeight.bold),
            ),
          ),
          const SizedBox(height: 16),

          const Text(
            'This app automatically marks attendance whenever you connect to your office Wi-Fi network.',
            style: TextStyle(fontSize: 16),
          ),
          const SizedBox(height: 32),
          const Text(
            'Required Permissions:',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 16),
          _buildPermissionItem(
            Icons.wifi,
            'WiFi',
            'To detect connection to office network',
          ),
          _buildPermissionItem(
            Icons.notifications,
            'Notifications',
            'For automatic attendance confirmations',
          ),
          _buildPermissionItem(
            Icons.camera_alt,
            'Camera/Photos',
            'Optional: For profile picture',
          ),
          const SizedBox(height: 24),
          Center(
            child: ElevatedButton.icon(
              onPressed: () async {
                await _requestPermissions();
                setState(() => _currentStep++);
              },
              icon: const Icon(Icons.check),
              label: const Text('Grant Permissions & Continue'),
            ),
          ),
        ],
      ),
    );
  }

  
  Widget _buildPermissionItem(IconData icon, String title, String description) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          Icon(icon, size: 32),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: const TextStyle(fontWeight: FontWeight.bold)),
                Text(description, style: const TextStyle(fontSize: 12, color: Colors.grey)),
              ],
            ),
          ),
        ],
      ),
    );
  }
  
  Widget _buildProfileStep() {
    return Form(
      key: _formKey,
      child: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          const Text(
            'Your Profile',
            style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 24),
          Center(
            child: GestureDetector(
              onTap: _showPhotoSourceDialog,
              child: CircleAvatar(
                radius: 50,
                backgroundImage: _photoPath != null ? FileImage(File(_photoPath!)) : null,
                child: _photoPath == null
                    ? const Icon(Icons.add_a_photo, size: 40)
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
    return ListView(
      padding: const EdgeInsets.all(24),
      children: [
        const Text(
          'Office WiFi & Schedule',
          style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 16),
        const Text(
          'Enter the name (SSID) of your office WiFi network. Attendance will be automatically recorded whenever connected.',
        ),
        const SizedBox(height: 24),
        FutureBuilder<List<String>>(
          future: _wifiService.getKnownSSIDs(),
          builder: (context, snapshot) {
            final knownList = snapshot.data ?? [];
            if (knownList.isNotEmpty) {
              final currentSelected = _ssidController.text.trim();
              return Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Select Office Wi-Fi Network',
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  DropdownButtonFormField<String>(
                    value: knownList.contains(currentSelected)
                        ? currentSelected
                        : knownList.first,
                    decoration: const InputDecoration(
                      prefixIcon: Icon(Icons.wifi),
                      labelText: 'Connected Wi-Fi History',
                    ),
                    items: knownList.map((ssid) => DropdownMenuItem(
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
                  const SizedBox(height: 16),
                ],
              );
            }
            return const SizedBox.shrink();
          },
        ),
        TextFormField(
          controller: _ssidController,
          decoration: const InputDecoration(
            labelText: 'Office WiFi SSID *',
            prefixIcon: Icon(Icons.edit),
            hintText: 'e.g., Office_WiFi',
          ),
        ),
        const SizedBox(height: 32),
        const Text(
          'Work Schedule',
          style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
        ),
        const SizedBox(height: 12),

        const SizedBox(height: 16),
        const Text('Working Days:', style: TextStyle(fontWeight: FontWeight.bold)),
        const SizedBox(height: 8),
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

