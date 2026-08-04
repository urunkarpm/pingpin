import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class AppTheme {
  // Light E-Ink Warm Paper Theme
  static ThemeData get lightTheme {
    const paperBg = Color(0xFFF7F4EB); // Warm off-white paper
    const surfaceBg = Color(0xFFEFECE2); // Card surface
    const inkBlack = Color(0xFF121212); // Deep rich black ink
    const borderDark = Color(0xFF222222);

    final baseTextTheme = ThemeData.light().textTheme;
    final googleSansTheme = GoogleFonts.googleSansTextTheme(baseTextTheme).apply(
      bodyColor: inkBlack,
      displayColor: inkBlack,
    );

    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.light,
      textTheme: googleSansTheme,
      primaryColor: inkBlack,
      scaffoldBackgroundColor: paperBg,
      colorScheme: const ColorScheme.light(
        primary: inkBlack,
        secondary: inkBlack,
        surface: surfaceBg,
        error: Colors.black,
        onPrimary: paperBg,
        onSecondary: paperBg,
        onSurface: inkBlack,
      ),
      appBarTheme: AppBarTheme(
        backgroundColor: paperBg,
        foregroundColor: inkBlack,
        elevation: 0,
        scrolledUnderElevation: 0,
        centerTitle: false,
        iconTheme: const IconThemeData(color: inkBlack),
        titleTextStyle: GoogleFonts.googleSans(
          color: inkBlack,
          fontSize: 22,
          fontWeight: FontWeight.bold,
          letterSpacing: 0.2,
        ),
      ),
      cardTheme: CardThemeData(
        color: surfaceBg,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
          side: const BorderSide(color: borderDark, width: 2.0),
        ),
      ),
      dividerTheme: const DividerThemeData(
        color: borderDark,
        thickness: 1.5,
        space: 1,
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: inkBlack,
          foregroundColor: paperBg,
          elevation: 0,
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(8),
            side: const BorderSide(color: borderDark, width: 2),
          ),
          textStyle: GoogleFonts.googleSans(
            fontSize: 16,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: inkBlack,
          backgroundColor: surfaceBg,
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
          side: const BorderSide(color: borderDark, width: 2),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(8),
          ),
          textStyle: GoogleFonts.googleSans(
            fontSize: 16,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: surfaceBg,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8),
          borderSide: const BorderSide(color: borderDark, width: 2),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8),
          borderSide: const BorderSide(color: borderDark, width: 2),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8),
          borderSide: const BorderSide(color: inkBlack, width: 3),
        ),
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: surfaceBg,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
          side: const BorderSide(color: borderDark, width: 2.5),
        ),
      ),
    );
  }

  // Dark E-Ink Charcoal Theme
  static ThemeData get darkTheme {
    const darkPaperBg = Color(0xFF121212); // Deep e-ink charcoal black
    const darkSurfaceBg = Color(0xFF1E1E1E); // E-ink dark card surface
    const inkWhite = Color(0xFFF7F4EB); // Off-white ink text
    const borderLight = Color(0xFFEFECE2); // Sharp light e-ink borders

    final baseTextTheme = ThemeData.dark().textTheme;
    final googleSansTheme = GoogleFonts.googleSansTextTheme(baseTextTheme).apply(
      bodyColor: inkWhite,
      displayColor: inkWhite,
    );

    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      textTheme: googleSansTheme,
      primaryColor: inkWhite,
      scaffoldBackgroundColor: darkPaperBg,
      colorScheme: const ColorScheme.dark(
        primary: inkWhite,
        secondary: inkWhite,
        surface: darkSurfaceBg,
        error: Colors.white,
        onPrimary: darkPaperBg,
        onSecondary: darkPaperBg,
        onSurface: inkWhite,
      ),
      appBarTheme: AppBarTheme(
        backgroundColor: darkPaperBg,
        foregroundColor: inkWhite,
        elevation: 0,
        scrolledUnderElevation: 0,
        centerTitle: false,
        iconTheme: const IconThemeData(color: inkWhite),
        titleTextStyle: GoogleFonts.googleSans(
          color: inkWhite,
          fontSize: 22,
          fontWeight: FontWeight.bold,
          letterSpacing: 0.2,
        ),
      ),
      cardTheme: CardThemeData(
        color: darkSurfaceBg,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
          side: const BorderSide(color: borderLight, width: 2.0),
        ),
      ),
      dividerTheme: const DividerThemeData(
        color: borderLight,
        thickness: 1.5,
        space: 1,
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: inkWhite,
          foregroundColor: darkPaperBg,
          elevation: 0,
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(8),
            side: const BorderSide(color: borderLight, width: 2),
          ),
          textStyle: GoogleFonts.googleSans(
            fontSize: 16,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: inkWhite,
          backgroundColor: darkSurfaceBg,
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 14),
          side: const BorderSide(color: borderLight, width: 2),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(8),
          ),
          textStyle: GoogleFonts.googleSans(
            fontSize: 16,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: darkSurfaceBg,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8),
          borderSide: const BorderSide(color: borderLight, width: 2),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8),
          borderSide: const BorderSide(color: borderLight, width: 2),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(8),
          borderSide: const BorderSide(color: inkWhite, width: 3),
        ),
      ),
      dialogTheme: DialogThemeData(
        backgroundColor: darkSurfaceBg,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
          side: const BorderSide(color: borderLight, width: 2.5),
        ),
      ),
    );
  }
}
