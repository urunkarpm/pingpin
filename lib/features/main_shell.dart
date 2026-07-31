import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../core/widgets/liquid_glass_nav_bar.dart';
import 'home/home_screen.dart';
import 'insights/insights_screen.dart';
import 'settings/settings_screen.dart';

/// Root shell that provides the liquid glass bottom navigation bar
/// and manages switching between the three main tabs.
class MainShell extends ConsumerStatefulWidget {
  const MainShell({super.key});

  @override
  ConsumerState<MainShell> createState() => _MainShellState();
}

class _MainShellState extends ConsumerState<MainShell> {
  int _currentIndex = 0;

  // Keep tab screens alive with IndexedStack so they don't rebuild on switch.
  final List<Widget> _screens = const [
    HomeScreen(),
    InsightsScreen(),
    SettingsScreen(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      // Use extendBody so the glass nav bar floats over scroll content
      extendBody: true,
      body: IndexedStack(
        index: _currentIndex,
        children: _screens,
      ),
      bottomNavigationBar: LiquidGlassNavBar(
        currentIndex: _currentIndex,
        onTap: (index) {
          if (index != _currentIndex) {
            setState(() => _currentIndex = index);
          }
        },
      ),
    );
  }
}
