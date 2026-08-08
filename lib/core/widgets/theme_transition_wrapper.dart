import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../providers/providers.dart';
import '../theme/app_theme.dart';

/// Wraps the application to render a 120Hz-ready, continuous ThemeData lerp transition
/// whenever the theme mode changes (Light <-> Dark <-> System).
class ThemeTransitionWrapper extends ConsumerStatefulWidget {
  final Widget child;

  const ThemeTransitionWrapper({
    super.key,
    required this.child,
  });

  @override
  ConsumerState<ThemeTransitionWrapper> createState() =>
      _ThemeTransitionWrapperState();
}

class _ThemeTransitionWrapperState
    extends ConsumerState<ThemeTransitionWrapper> {
  ThemeData _resolveTargetTheme(ThemeMode mode, BuildContext context) {
    switch (mode) {
      case ThemeMode.light:
        return AppTheme.lightTheme;
      case ThemeMode.dark:
        return AppTheme.darkTheme;
      case ThemeMode.system:
        final platformBrightness = MediaQuery.platformBrightnessOf(context);
        return platformBrightness == Brightness.dark
            ? AppTheme.darkTheme
            : AppTheme.lightTheme;
    }
  }

  void _updateSystemUiOverlay(ThemeData theme) {
    final isDark = theme.brightness == Brightness.dark;
    SystemChrome.setSystemUIOverlayStyle(
      SystemUiOverlayStyle(
        statusBarColor: Colors.transparent,
        statusBarIconBrightness: isDark ? Brightness.light : Brightness.dark,
        statusBarBrightness: isDark ? Brightness.dark : Brightness.light,
        systemNavigationBarColor: theme.scaffoldBackgroundColor,
        systemNavigationBarIconBrightness:
            isDark ? Brightness.light : Brightness.dark,
        systemNavigationBarDividerColor: Colors.transparent,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final themeMode = ref.watch(themeModeProvider);
    final targetTheme = _resolveTargetTheme(themeMode, context);

    // Synchronize system navigation and status bars seamlessly with theme changes
    _updateSystemUiOverlay(targetTheme);

    return AnimatedTheme(
      data: targetTheme,
      duration: const Duration(milliseconds: 350),
      curve: Curves.fastOutSlowIn,
      child: widget.child,
    );
  }
}
