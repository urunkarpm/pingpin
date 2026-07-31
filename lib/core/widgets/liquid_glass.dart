import 'dart:ui';
import 'package:flutter/material.dart';
import '../constants/app_constants.dart';

/// Modern Liquid Glass widget featuring backdrop blur, translucent gradients,
/// and edge reflections.
class LiquidGlassContainer extends StatelessWidget {
  final Widget child;
  final double blur;
  final double opacity;
  final double borderRadius;
  final EdgeInsetsGeometry padding;
  final EdgeInsetsGeometry? margin;
  final Color? tintColor;
  final Border? border;
  final List<BoxShadow>? boxShadow;
  final VoidCallback? onTap;

  const LiquidGlassContainer({
    super.key,
    required this.child,
    this.blur = 20.0,
    this.opacity = 0.15,
    this.borderRadius = 24.0,
    this.padding = const EdgeInsets.all(20.0),
    this.margin,
    this.tintColor,
    this.border,
    this.boxShadow,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final baseColor = tintColor ?? AppColors.primary;
    final radius = BorderRadius.circular(borderRadius);
    final effectiveBorder = border ??
        Border.all(
          color: Colors.white.withValues(alpha: 0.56),
          width: 1.0,
        );

    final glassWidget = RepaintBoundary(
      child: Container(
        margin: margin,
        decoration: BoxDecoration(
          borderRadius: radius,
          boxShadow: boxShadow ??
              [
                BoxShadow(
                  color: AppColors.primaryDark.withValues(alpha: 0.10),
                  blurRadius: 28,
                  spreadRadius: -10,
                  offset: const Offset(0, 14),
                ),
                BoxShadow(
                  color: baseColor.withValues(alpha: 0.12),
                  blurRadius: 18,
                  spreadRadius: -8,
                  offset: const Offset(0, 6),
                ),
              ],
        ),
        child: ClipRRect(
          borderRadius: radius,
          child: BackdropFilter(
            filter: ImageFilter.blur(
                sigmaX: blur, sigmaY: blur, tileMode: TileMode.clamp),
            child: Container(
              padding: padding,
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: [
                    Colors.white.withValues(alpha: (opacity * 0.36) + 0.18),
                    baseColor.withValues(alpha: opacity),
                  ],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: radius,
                border: effectiveBorder,
              ),
              child: Stack(
                children: [
                  Positioned(
                    top: 0,
                    left: 18,
                    right: 18,
                    child: IgnorePointer(
                      child: Container(
                        height: 1,
                        decoration: BoxDecoration(
                          gradient: LinearGradient(
                            colors: [
                              Colors.white.withValues(alpha: 0),
                              Colors.white.withValues(alpha: 0.86),
                              Colors.white.withValues(alpha: 0),
                            ],
                          ),
                        ),
                      ),
                    ),
                  ),
                  child,
                ],
              ),
            ),
          ),
        ),
      ),
    );

    if (onTap != null) {
      return GestureDetector(
        onTap: onTap,
        child: glassWidget,
      );
    }
    return glassWidget;
  }
}

/// Shared ambient colour field that gives glass surfaces something to refract.
class LiquidGlassBackground extends StatelessWidget {
  final Widget child;

  const LiquidGlassBackground({super.key, required this.child});

  @override
  Widget build(BuildContext context) {
    return Stack(
      fit: StackFit.expand,
      children: [
        const ColoredBox(color: Color(0xFFF3F7FF)),
        const Positioned(
          top: -150,
          right: -90,
          child: _AmbientOrb(color: Color(0xFF8CB8FF), size: 330),
        ),
        const Positioned(
          top: 260,
          left: -150,
          child: _AmbientOrb(color: Color(0xFF8BE5C1), size: 310),
        ),
        const Positioned(
          bottom: -180,
          right: -60,
          child: _AmbientOrb(color: Color(0xFFFFC98B), size: 360),
        ),
        child,
      ],
    );
  }
}

class _AmbientOrb extends StatelessWidget {
  final Color color;
  final double size;

  const _AmbientOrb({required this.color, required this.size});

  @override
  Widget build(BuildContext context) {
    return IgnorePointer(
      child: ImageFiltered(
        imageFilter: ImageFilter.blur(sigmaX: 42, sigmaY: 42),
        child: Container(
          width: size,
          height: size,
          decoration: BoxDecoration(
              color: color.withValues(alpha: 0.48), shape: BoxShape.circle),
        ),
      ),
    );
  }
}

/// Liquid Glass Badge component
class LiquidGlassBadge extends StatelessWidget {
  final IconData icon;
  final String label;
  final Color color;

  const LiquidGlassBadge({
    super.key,
    required this.icon,
    required this.label,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(30),
        border: Border.all(color: color.withValues(alpha: 0.4), width: 1.2),
        boxShadow: [
          BoxShadow(
            color: color.withValues(alpha: 0.2),
            blurRadius: 10,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: color),
          const SizedBox(width: 6),
          Text(
            label,
            style: TextStyle(
              color: color,
              fontSize: 12,
              fontWeight: FontWeight.bold,
              letterSpacing: 0.3,
            ),
          ),
        ],
      ),
    );
  }
}
