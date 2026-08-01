# PingPin

PingPin is an automated, privacy-focused office attendance tracking application designed with a sleek liquid glass aesthetic, E-Ink paper style elements, and tactile haptic feedback.

## Key Features

- **Automatic Wi-Fi Attendance Detection**: Automatically records attendance when connected to your configured office Wi-Fi network.
- **BLE Office Presence & Laptop Occupancy Detection**: Non-intrusively scans Bluetooth Low Energy (BLE) signals to detect nearby active office laptops (macOS & Windows) without installing agent software.
- **Liquid Glass Navigation Bar**: Modern, floating bottom navigation design with smooth translucent glassmorphism effects and tactile haptic feedback.
- **E-Ink & Dark Themes**: High-contrast paper newsprint visual styling (`#F7F4EB`) and dark charcoal mode for maximum legibility.
- **Full Month Calendar View**: High-contrast attendance calendar with quick month-stepping controls (`<` and `>`) and interactive 12-month picker modal.
- **Saved Wi-Fi Networks History**: Dropdown selector populated with historical saved networks, active Wi-Fi connections, and local database entries.
- **PDF Attendance Reports**: Export monthly attendance summaries, statistics, and log history to PDF documents.
- **Local Data Privacy**: All attendance records, BLE scan metrics, and Wi-Fi configurations remain completely on-device.

## Automated Builds & Releases

PingPin includes CI/CD automation via GitHub Actions:
- **iOS / IPA Build Workflow**: Automatically builds and packages the iOS `.ipa` payload on push to `main`/`master` or via manual workflow dispatch (`build_ios.yml`).
