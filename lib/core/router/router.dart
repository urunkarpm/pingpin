import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../../features/onboarding/onboarding_screen.dart';
import '../../features/main_shell.dart';
import '../../features/alarm/alarm_ringing_screen.dart';
import '../../core/constants/app_constants.dart';
import '../../providers/providers.dart';

final rootNavigatorKey = GlobalKey<NavigatorState>();

SharedPreferences? _cachedPrefs;

final routerProvider = Provider<GoRouter>((ref) {
  final onboardingState = ref.watch(onboardingCompleteProvider);

  return GoRouter(
    navigatorKey: rootNavigatorKey,
    initialLocation: '/',
    redirect: (context, state) async {
      _cachedPrefs ??= await SharedPreferences.getInstance();
      final isCompleted = _cachedPrefs?.getBool(AppKeys.onboardingComplete) ?? onboardingState;
      final isOnboardingRoute = state.matchedLocation == '/onboarding';

      if (!isCompleted && !isOnboardingRoute) {
        return '/onboarding';
      }
      if (isCompleted && isOnboardingRoute) {
        return '/';
      }
      return null;
    },
    routes: [
      GoRoute(
        path: '/onboarding',
        name: 'onboarding',
        builder: (context, state) => const OnboardingScreen(),
      ),
      GoRoute(
        path: '/',
        name: 'home',
        builder: (context, state) => const MainShell(),
      ),
      GoRoute(
        path: '/alarm-ringing',
        name: 'alarm-ringing',
        builder: (context, state) {
          final extra = state.extra as Map<String, dynamic>?;
          return AlarmRingingScreen(
            alarmId: extra?['alarmId'] ?? 101,
            portalUrl: extra?['portalUrl'] ?? '',
          );
        },
      ),
    ],
  );
});
