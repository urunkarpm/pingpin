import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// A reusable card container with smooth theme lerping, ambient soft drop shadow,
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

    final defaultBg = backgroundColor ?? theme.colorScheme.surface;
    final defaultBorder = borderColor ?? theme.colorScheme.outlineVariant;

    final cardWidget = Container(
      margin: margin,
      padding: padding,
      decoration: BoxDecoration(
        color: defaultBg,
        borderRadius: BorderRadius.circular(borderRadius),
        border: Border.all(color: defaultBorder, width: 1.2),
        boxShadow: [
          BoxShadow(
            color: theme.shadowColor,
            blurRadius: 18,
            spreadRadius: 0,
            offset: const Offset(0, 8),
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
