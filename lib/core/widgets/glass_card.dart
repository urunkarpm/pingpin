import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// A reusable glassmorphic container with micro-shimmer line, ambient soft drop shadow,
/// dynamic surface border, and optional tactile haptic response.
class GlassCard extends StatelessWidget {
  final Widget child;
  final EdgeInsetsGeometry padding;
  final EdgeInsetsGeometry margin;
  final VoidCallback? onTap;
  final double borderRadius;
  final Color? borderColor;
  final Color? backgroundColor;

  const GlassCard({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.all(20),
    this.margin = EdgeInsets.zero,
    this.onTap,
    this.borderRadius = 22.0,
    this.borderColor,
    this.backgroundColor,
  });

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;

    final defaultBg = backgroundColor ??
        (isDark ? const Color(0xFF1E1E1E) : theme.colorScheme.surface);

    final defaultBorder = borderColor ??
        (isDark
            ? Colors.white.withValues(alpha: 0.08)
            : theme.colorScheme.onSurface.withValues(alpha: 0.08));

    final cardWidget = AnimatedContainer(
      duration: const Duration(milliseconds: 200),
      curve: Curves.easeOut,
      margin: margin,
      padding: padding,
      decoration: BoxDecoration(
        color: defaultBg,
        borderRadius: BorderRadius.circular(borderRadius),
        border: Border.all(color: defaultBorder, width: 1.2),
        boxShadow: [
          BoxShadow(
            color: isDark
                ? Colors.black.withValues(alpha: 0.35)
                : Colors.black.withValues(alpha: 0.04),
            blurRadius: 18,
            spreadRadius: 0,
            offset: const Offset(0, 8),
          ),
          if (!isDark)
            BoxShadow(
              color: Colors.white.withValues(alpha: 0.8),
              blurRadius: 1,
              spreadRadius: 0,
              offset: const Offset(0, 1),
            ),
        ],
      ),
      child: child,
    );

    if (onTap != null) {
      return Padding(
        padding: margin,
        child: Material(
          color: Colors.transparent,
          borderRadius: BorderRadius.circular(borderRadius),
          child: InkWell(
            onTap: () {
              HapticFeedback.lightImpact();
              onTap!();
            },
            borderRadius: BorderRadius.circular(borderRadius),
            splashColor: theme.colorScheme.primary.withValues(alpha: 0.1),
            highlightColor: theme.colorScheme.primary.withValues(alpha: 0.05),
            child: GlassCard(
              padding: padding,
              margin: EdgeInsets.zero,
              borderRadius: borderRadius,
              borderColor: borderColor,
              backgroundColor: backgroundColor,
              child: child,
            ),
          ),
        ),
      );
    }

    return cardWidget;
  }
}
