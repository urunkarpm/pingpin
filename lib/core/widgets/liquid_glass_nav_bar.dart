import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../constants/app_constants.dart';

/// A split floating navigation bar supporting both E-Ink Light & Dark modes seamlessly.
class LiquidGlassNavBar extends StatelessWidget {
  final int currentIndex;
  final ValueChanged<int> onTap;

  const LiquidGlassNavBar({
    super.key,
    required this.currentIndex,
    required this.onTap,
  });

  static const _primaryItems = [
    _NavItem(
      icon: Icons.calendar_month_outlined,
      activeIcon: Icons.calendar_month_rounded,
      label: 'Calendar',
    ),
    _NavItem(
      icon: Icons.insights_outlined,
      activeIcon: Icons.insights_rounded,
      label: 'Insights',
    ),
  ];

  @override
  Widget build(BuildContext context) {
    final primaryIndex = currentIndex.clamp(0, _primaryItems.length - 1);
    final isSettingsSelected = currentIndex == 2;

    return SafeArea(
      top: false,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 0, 20, 14),
        child: Row(
          children: [
            Expanded(
              child: _GlassCapsule(
                child: Stack(
                  fit: StackFit.expand,
                  children: [
                    LayoutBuilder(
                      builder: (context, constraints) => AnimatedPositioned(
                        duration: const Duration(milliseconds: 220),
                        curve: Curves.fastOutSlowIn,
                        left: primaryIndex *
                            constraints.maxWidth /
                            _primaryItems.length,
                        top: 0,
                        bottom: 0,
                        width: constraints.maxWidth / _primaryItems.length,
                        child: const _SelectionPuck(),
                      ),
                    ),
                    Row(
                      children: List.generate(_primaryItems.length, (index) {
                        final item = _primaryItems[index];
                        final selected =
                            !isSettingsSelected && index == primaryIndex;
                        return Expanded(
                          child: _PillTab(
                            item: item,
                            selected: selected,
                            onTap: () {
                              HapticFeedback.selectionClick();
                              onTap(index);
                            },
                          ),
                        );
                      }),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(width: 12),
            _GlassCircleTab(
              selected: isSettingsSelected,
              onTap: () {
                HapticFeedback.selectionClick();
                onTap(2);
              },
            ),
          ],
        ),
      ),
    );
  }
}

class _GlassCapsule extends StatelessWidget {
  final Widget child;

  const _GlassCapsule({required this.child});

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    const radius = BorderRadius.all(Radius.circular(30));

    final bgColor = isDark ? const Color(0xFF1E1E1E) : const Color(0xFFEFECE2);
    final borderColor = isDark ? const Color(0xFFEFECE2) : const Color(0xFF222222);

    return Container(
      height: 62,
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: radius,
        border: Border.all(color: borderColor, width: 2.0),
      ),
      child: ClipRRect(
        borderRadius: radius,
        child: child,
      ),
    );
  }
}

class _SelectionPuck extends StatelessWidget {
  const _SelectionPuck();

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final puckBg = isDark ? const Color(0xFFF7F4EB) : const Color(0xFF121212);
    final puckBorder = isDark ? const Color(0xFF121212) : const Color(0xFFF7F4EB);

    return Padding(
      padding: const EdgeInsets.all(5),
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: puckBg,
          borderRadius: BorderRadius.circular(25),
          border: Border.all(color: puckBorder, width: 1.5),
        ),
      ),
    );
  }
}

class _PillTab extends StatelessWidget {
  final _NavItem item;
  final bool selected;
  final VoidCallback onTap;

  const _PillTab({
    required this.item,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    
    // Invert text/icon color on puck
    final activeColor = isDark ? const Color(0xFF121212) : const Color(0xFFF7F4EB);
    final inactiveColor = isDark ? const Color(0xFFA0A0A0) : const Color(0xFF555555);

    final color = selected ? activeColor : inactiveColor;

    return Semantics(
      button: true,
      selected: selected,
      label: item.label,
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: onTap,
        child: AnimatedScale(
          scale: selected ? 1.02 : 0.94,
          duration: const Duration(milliseconds: 180),
          curve: Curves.easeOutCubic,
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(selected ? item.activeIcon : item.icon,
                  size: 23, color: color),
              const SizedBox(height: 2),
              AnimatedDefaultTextStyle(
                duration: const Duration(milliseconds: 180),
                style: TextStyle(
                  color: color,
                  fontSize: 11,
                  fontWeight: selected ? FontWeight.bold : FontWeight.w600,
                  letterSpacing: selected ? 0.2 : 0,
                ),
                child: Text(item.label),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _GlassCircleTab extends StatelessWidget {
  final bool selected;
  final VoidCallback onTap;

  const _GlassCircleTab({required this.selected, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;

    final circleBg = selected
        ? (isDark ? const Color(0xFFF7F4EB) : const Color(0xFF121212))
        : (isDark ? const Color(0xFF1E1E1E) : const Color(0xFFEFECE2));

    final iconColor = selected
        ? (isDark ? const Color(0xFF121212) : const Color(0xFFF7F4EB))
        : (isDark ? const Color(0xFFA0A0A0) : const Color(0xFF555555));

    final borderColor = isDark ? const Color(0xFFEFECE2) : const Color(0xFF222222);

    return Semantics(
      button: true,
      selected: selected,
      label: 'Settings',
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: onTap,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 200),
          curve: Curves.easeOutCubic,
          width: 62,
          height: 62,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: circleBg,
            border: Border.all(color: borderColor, width: 2.0),
          ),
          child: Icon(
            selected ? Icons.settings_rounded : Icons.settings_outlined,
            color: iconColor,
            size: 25,
          ),
        ),
      ),
    );
  }
}

class _NavItem {
  final IconData icon;
  final IconData activeIcon;
  final String label;

  const _NavItem({
    required this.icon,
    required this.activeIcon,
    required this.label,
  });
}
