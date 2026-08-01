import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// A single floating navigation bar supporting both E-Ink Light & Dark modes seamlessly.
class LiquidGlassNavBar extends StatelessWidget {
  final int currentIndex;
  final ValueChanged<int> onTap;

  const LiquidGlassNavBar({
    super.key,
    required this.currentIndex,
    required this.onTap,
  });

  static const _navItems = [
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
    _NavItem(
      icon: Icons.settings_outlined,
      activeIcon: Icons.settings_rounded,
      label: 'Settings',
    ),
  ];

  void _handleDrag(Offset globalPosition, double totalWidth) {
    final itemWidth = totalWidth / _navItems.length;
    final targetIndex = (globalPosition.dx / itemWidth).floor().clamp(0, _navItems.length - 1);
    if (targetIndex != currentIndex) {
      HapticFeedback.selectionClick();
      onTap(targetIndex);
    }
  }

  @override
  Widget build(BuildContext context) {
    final validIndex = currentIndex.clamp(0, _navItems.length - 1);

    return SafeArea(
      top: false,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 0, 20, 18),
        child: _GlassCapsule(
          child: LayoutBuilder(
            builder: (context, constraints) {
              final itemWidth = constraints.maxWidth / _navItems.length;
              return GestureDetector(
                behavior: HitTestBehavior.opaque,
                onHorizontalDragUpdate: (details) {
                  final RenderBox box = context.findRenderObject() as RenderBox;
                  final localOffset = box.globalToLocal(details.globalPosition);
                  _handleDrag(localOffset, constraints.maxWidth);
                },
                onHorizontalDragDown: (details) {
                  final RenderBox box = context.findRenderObject() as RenderBox;
                  final localOffset = box.globalToLocal(details.globalPosition);
                  _handleDrag(localOffset, constraints.maxWidth);
                },
                child: Stack(
                  fit: StackFit.expand,
                  children: [
                    AnimatedPositioned(
                      duration: const Duration(milliseconds: 220),
                      curve: Curves.fastOutSlowIn,
                      left: validIndex * itemWidth,
                      top: 0,
                      bottom: 0,
                      width: itemWidth,
                      child: const _SelectionPuck(),
                    ),
                    Row(
                      children: List.generate(_navItems.length, (index) {
                        final item = _navItems[index];
                        final selected = index == validIndex;
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
              );
            },
          ),
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
    const radius = BorderRadius.all(Radius.circular(36));

    final bgColor = isDark ? const Color(0xFF1A1A1A) : const Color(0xFFEFECE2);
    final borderColor = isDark ? const Color(0xFF2E2E2E) : const Color(0xFF222222);

    return Container(
      height: 70,
      decoration: BoxDecoration(
        color: bgColor,
        borderRadius: radius,
        border: Border.all(color: borderColor, width: 2.0),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withAlpha(isDark ? 80 : 30),
            blurRadius: 16,
            offset: const Offset(0, 6),
          ),
        ],
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
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 6),
      child: DecoratedBox(
        decoration: BoxDecoration(
          color: puckBg,
          borderRadius: BorderRadius.circular(28),
          border: Border.all(color: puckBorder, width: 1.5),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withAlpha(isDark ? 25 : 40),
              blurRadius: 4,
              offset: const Offset(0, 2),
            ),
          ],
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
    
    // Invert text/icon color on puck when selected
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
        child: SizedBox(
          height: double.infinity,
          child: Center(
            child: selected
                ? AnimatedOpacity(
                    duration: const Duration(milliseconds: 200),
                    opacity: 1.0,
                    child: Text(
                      item.label,
                      style: TextStyle(
                        color: color,
                        fontSize: 13,
                        fontWeight: FontWeight.w800,
                        letterSpacing: 0.4,
                      ),
                    ),
                  )
                : Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(
                        item.icon,
                        size: 20,
                        color: color,
                      ),
                      const SizedBox(height: 2),
                      Text(
                        item.label,
                        style: TextStyle(
                          color: color,
                          fontSize: 10,
                          fontWeight: FontWeight.w500,
                          letterSpacing: 0.1,
                          height: 1.1,
                        ),
                      ),
                    ],
                  ),
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
