import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../features/onboarding/onboarding_screen.dart';
import '../../features/main_shell.dart';

import 'package:shared_preferences/shared_preferences.dart';
import '../../core/constants/app_constants.dart';

final routerProvider = Provider<GoRouter>((ref) {
  return GoRouter(
    initialLocation: '/',
    redirect: (context, state) async {
      final prefs = await SharedPreferences.getInstance();
      final isCompleted = prefs.getBool(AppKeys.onboardingComplete) ?? false;
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
    ],
  );
});

