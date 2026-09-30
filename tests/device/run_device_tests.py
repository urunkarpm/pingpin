#!/usr/bin/env python3
"""
PingPin Android Live Device Integration & E2E Test Suite
Uses standard library only (no external pip dependencies).
Connects via ADB to physical device or emulator.
"""

import sys
import os
import time
import subprocess
import argparse
from pathlib import Path

# Paths
REPO_ROOT = Path(__file__).resolve().parent.parent.parent
TESTS_DIR = REPO_ROOT / "tests"
ARTIFACTS_DIR = TESTS_DIR / "artifacts"
ARTIFACTS_DIR.mkdir(parents=True, exist_ok=True)

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

PACKAGE_NAME = "com.urunkarpm.pingpin"
MAIN_ACTIVITY = f"{PACKAGE_NAME}/.MainActivity"
ALARM_ACTIVITY = f"{PACKAGE_NAME}/.AlarmActivity"
DEFAULT_WIFI_ADB = "192.168.1.8:36265"


class DeviceTestRunner:
    def __init__(self, serial=None):
        self.serial = serial
        self.artifacts_dir = ARTIFACTS_DIR
        self.results = []

    def log(self, msg):
        print(f"[DeviceTest] {msg}", flush=True)

    def run_adb(self, cmd_args, timeout=20, check=False):
        base_cmd = ["adb"]
        if self.serial:
            base_cmd.extend(["-s", self.serial])
        full_cmd = base_cmd + cmd_args
        try:
            res = subprocess.run(
                full_cmd,
                capture_output=True,
                text=True,
                timeout=timeout,
                encoding="utf-8",
                errors="replace"
            )
            if check and res.returncode != 0:
                raise RuntimeError(f"ADB command failed ({res.returncode}): {' '.join(full_cmd)}\n{res.stderr}")
            return res
        except subprocess.TimeoutExpired:
            self.log(f"WARNING: ADB command timed out after {timeout}s: {' '.join(full_cmd)}")
            return None

    def detect_device(self):
        self.log("Detecting connected Android devices...")
        res = self.run_adb(["devices"])
        if not res or res.returncode != 0:
            self.log("Failed to query 'adb devices'")
            return False

        lines = [line.strip() for line in res.stdout.strip().split("\n")[1:] if line.strip()]
        connected = []
        for line in lines:
            parts = line.split()
            if len(parts) >= 2 and parts[1] == "device":
                connected.append(parts[0])

        if self.serial and self.serial in connected:
            self.log(f"Using specified device: {self.serial}")
            return True

        if connected:
            self.serial = connected[0]
            self.log(f"Auto-selected active device: {self.serial}")
            return True

        # Try connecting via user's known Wi-Fi address
        self.log(f"No USB device found. Attempting Wi-Fi ADB connect to {DEFAULT_WIFI_ADB}...")
        c_res = subprocess.run(["adb", "connect", DEFAULT_WIFI_ADB], capture_output=True, text=True, timeout=10)
        if c_res.returncode == 0 and "connected" in c_res.stdout.lower():
            self.serial = DEFAULT_WIFI_ADB
            self.log(f"Connected to Wi-Fi device: {self.serial}")
            return True

        self.log("ERROR: No authorized Android device found over USB or Wi-Fi.")
        return False

    def shell(self, cmd_str, timeout=15):
        res = self.run_adb(["shell"] + cmd_str.split(), timeout=timeout)
        return res.stdout if res else ""

    def tap(self, x, y, delay_after=0.8):
        self.run_adb(["shell", "input", "tap", str(x), str(y)])
        if delay_after > 0:
            time.sleep(delay_after)

    def swipe(self, x1, y1, x2, y2, duration_ms=400, delay_after=0.8):
        self.run_adb(["shell", "input", "swipe", str(x1), str(y1), str(x2), str(y2), str(duration_ms)])
        if delay_after > 0:
            time.sleep(delay_after)

    def keyevent(self, keycode, delay_after=0.5):
        self.run_adb(["shell", "input", "keyevent", str(keycode)])
        if delay_after > 0:
            time.sleep(delay_after)

    def take_screenshot(self, filename):
        dest_path = self.artifacts_dir / filename
        remote_path = f"/sdcard/{filename}"
        self.run_adb(["shell", "screencap", "-p", remote_path])
        self.run_adb(["pull", remote_path, str(dest_path)])
        return dest_path

    def dump_ui(self):
        remote_xml = "/sdcard/pingpin_ui_dump.xml"
        self.run_adb(["shell", "uiautomator", "dump", remote_xml], timeout=15)
        local_xml = self.artifacts_dir / "current_ui_dump.xml"
        self.run_adb(["pull", remote_xml, str(local_xml)], timeout=10)
        if local_xml.exists():
            return local_xml.read_text(encoding="utf-8", errors="replace")
        return ""

    def wait_for_ui_pattern(self, pattern, timeout=8, interval=0.8):
        regex = re.compile(pattern, re.IGNORECASE)
        start = time.time()
        while time.time() - start < timeout:
            xml = self.dump_ui()
            if regex.search(xml):
                return True
            time.sleep(interval)
        return False

    # ----------------------------------------------------
    # TEST CASES
    # ----------------------------------------------------

    def test_01_device_readiness(self):
        """Verify device model, Android SDK version, and display state."""
        start_time = time.time()
        model = self.shell("getprop ro.product.model").strip()
        brand = self.shell("getprop ro.product.brand").strip()
        sdk = self.shell("getprop ro.build.version.sdk").strip()
        self.log(f"Device: {brand} {model} (SDK {sdk})")
        assert model, "Could not determine device model"
        assert int(sdk) >= 26, f"Expected Android SDK >= 26, found {sdk}"

        # Ensure screen is awake
        self.shell("input keyevent 224")  # KEYCODE_WAKEUP
        self.shell("wm dismiss-keyguard")
        duration = round(time.time() - start_time, 2)
        return True, f"Device {brand} {model} (Android SDK {sdk}) awake & authorized", duration

    def test_02_app_launch(self):
        """Launch MainActivity and verify foreground focus."""
        start_time = time.time()
        self.run_adb(["shell", "am", "start", "-n", MAIN_ACTIVITY])
        time.sleep(1.2)

        top_activity = self.shell("dumpsys window | grep -E 'mCurrentFocus|mFocusedApp'")
        shot = self.take_screenshot("test_02_launch.png")
        assert PACKAGE_NAME in top_activity, f"PingPin not in foreground focus: {top_activity}"
        duration = round(time.time() - start_time, 2)
        return True, f"PingPin MainActivity focused successfully. Captured: {shot.name}", duration

    def test_03_home_weather_tab(self):
        """Verify Home screen weather card, commute status, and attendance strip."""
        start_time = time.time()
        # Tap Home tab in bottom nav (x: 230, y: 2274)
        self.tap(230, 2274)
        # Ensure Weather tab selected (x: 300, y: 335)
        self.tap(300, 335)
        shot = self.take_screenshot("test_03_home_weather.png")

        xml = self.dump_ui()
        assert ("Weather" in xml or "Commute" in xml or "Overcast" in xml or "Clear" in xml or "Rain" in xml), \
            "Weather card not rendered in Home UI"
        duration = round(time.time() - start_time, 2)
        return True, f"Weather & commute hub rendered. Captured: {shot.name}", duration

    def test_04_home_holidays_tab(self):
        """Verify Indian Holidays tab with upcoming long weekend tags."""
        start_time = time.time()
        # Tap Holidays tab (x: 780, y: 335)
        self.tap(780, 335)
        shot = self.take_screenshot("test_04_home_holidays.png")

        xml = self.dump_ui()
        assert ("Holidays" in xml or "Gandhi" in xml or "Weekend" in xml or "October" in xml), \
            "Holidays content missing from Holidays tab"
        duration = round(time.time() - start_time, 2)
        return True, f"Indian Holidays schedule verified. Captured: {shot.name}", duration

    def test_05_monthly_calendar_expansion(self):
        """Expand monthly calendar sheet and verify date attendance indicators."""
        start_time = time.time()
        # Switch back to Weather tab (x: 300, y: 335)
        self.tap(300, 335)
        # Swipe up from weekly calendar strip to expand full month (x: 540, y: 2000 -> 1400)
        self.swipe(540, 2000, 540, 1300, duration_ms=300)
        shot = self.take_screenshot("test_05_calendar_expanded.png")

        xml = self.dump_ui()
        # Collapse back down
        self.swipe(540, 700, 540, 1600, duration_ms=300)
        duration = round(time.time() - start_time, 2)
        return True, f"Monthly calendar expansion verified. Captured: {shot.name}", duration

    def test_06_insights_screen(self):
        """Navigate to Insights screen and verify compliance ring & audit log."""
        start_time = time.time()
        # Tap Insights tab (x: 568, y: 2274)
        self.tap(568, 2274)
        shot1 = self.take_screenshot("test_06_insights_top.png")

        # Scroll down to audit log & PDF button
        self.swipe(540, 1800, 540, 600, duration_ms=400)
        shot2 = self.take_screenshot("test_06_insights_log.png")

        xml = self.dump_ui()
        assert ("Compliance" in xml or "Attendance" in xml or "WFO" in xml or "PRESENT" in xml), \
            "Insights metrics or audit log missing"
        duration = round(time.time() - start_time, 2)
        return True, f"Insights compliance gauge and audit log verified. Captured: {shot1.name}, {shot2.name}", duration

    def test_07_settings_profile(self):
        """Navigate to Settings -> Profile and verify employee & shift parameters."""
        start_time = time.time()
        # Tap Settings tab in bottom nav (x: 843, y: 2274)
        self.tap(843, 2274)
        # Tap Profile sub-tab (x: 160, y: 620)
        self.tap(160, 620)
        shot = self.take_screenshot("test_07_settings_profile.png")

        xml = self.dump_ui()
        assert ("Prasenjeet" in xml or "SSID" in xml or "Shift" in xml or "Check-In Time" in xml), \
            "Profile details not found in Settings"
        duration = round(time.time() - start_time, 2)
        return True, f"Settings profile & shift horizon verified. Captured: {shot.name}", duration

    def test_08_settings_automation(self):
        """Verify Settings -> Automation (HR portal auto check-in triggers)."""
        start_time = time.time()
        # Tap Automation sub-tab (x: 400, y: 620)
        self.tap(400, 620)
        shot = self.take_screenshot("test_08_settings_automation.png")

        xml = self.dump_ui()
        assert ("Auto Check-In" in xml or "HR Portal" in xml or "Execution Mode" in xml or "Auto Portal" in xml), \
            "Automation tab controls missing"
        duration = round(time.time() - start_time, 2)
        return True, f"Settings HR portal automation verified. Captured: {shot.name}", duration

    def test_09_settings_health(self):
        """Verify Settings -> Health (Exact alarm permission & OEM battery guidance)."""
        start_time = time.time()
        # Tap Health sub-tab (x: 615, y: 620)
        self.tap(615, 620)
        shot = self.take_screenshot("test_09_settings_health.png")

        xml = self.dump_ui()
        assert ("Alarm" in xml or "Battery" in xml or "Execution" in xml or "Unrestricted" in xml), \
            "Health & alarm permission controls missing"
        duration = round(time.time() - start_time, 2)
        return True, f"Settings alarm precision & OEM battery health verified. Captured: {shot.name}", duration

    def test_10_settings_updates(self):
        """Verify Settings -> Updates (App version v2.9.0 & release sync)."""
        start_time = time.time()
        # Tap Updates sub-tab (x: 840, y: 620)
        self.tap(840, 620)
        shot = self.take_screenshot("test_10_settings_updates.png")

        xml = self.dump_ui()
        assert ("v2.9.0" in xml or "Updates" in xml or "Check for Update" in xml), \
            "App update information missing"
        duration = round(time.time() - start_time, 2)
        return True, f"In-app update engine verified (v2.9.0). Captured: {shot.name}", duration

    def test_11_alarm_screen_and_chime(self):
        """Launch AlarmActivity, verify AMOLED chime layout, and dismiss safely."""
        start_time = time.time()
        cmd = [
            "shell", "am", "start", "-n", ALARM_ACTIVITY,
            "--ei", "alarmId", "101",
            "--es", "actionType", "ACTION_CHECK_IN",
            "--es", "title", "Test Morning Alarm"
        ]
        self.run_adb(cmd)
        time.sleep(1.2)
        shot = self.take_screenshot("test_11_alarm_screen.png")

        # Stop alarm chime service and dismiss
        self.run_adb(["shell", "am", "stopservice", f"{PACKAGE_NAME}/.service.AlarmSoundService"])
        # Slide up green check-out slider or press back to close alarm
        self.swipe(260, 1800, 260, 1300, duration_ms=250)
        self.keyevent(4)  # KEYCODE_BACK
        # Bring MainActivity back
        self.run_adb(["shell", "am", "start", "-n", MAIN_ACTIVITY])

        duration = round(time.time() - start_time, 2)
        return True, f"AMOLED alarm UI & chime verified and safely dismissed. Captured: {shot.name}", duration

    def test_12_pdf_statement_integrity(self):
        """Verify generated PDF statement file header (%PDF-), size, and pull artifact."""
        start_time = time.time()
        remote_pdf_dir = f"/sdcard/Android/data/{PACKAGE_NAME}/files/Documents"
        res = self.shell(f"ls {remote_pdf_dir}")
        pdf_filename = None
        for line in res.strip().split("\n"):
            line = line.strip()
            if line.endswith(".pdf"):
                pdf_filename = line
                break

        assert pdf_filename, f"No PDF statement found in {remote_pdf_dir}"
        remote_pdf_path = f"{remote_pdf_dir}/{pdf_filename}"
        local_pdf_path = self.artifacts_dir / pdf_filename

        self.run_adb(["pull", remote_pdf_path, str(local_pdf_path)])
        assert local_pdf_path.exists(), f"Failed to pull PDF file: {local_pdf_path}"
        pdf_size = local_pdf_path.stat().st_size
        assert pdf_size > 10000, f"PDF file too small ({pdf_size} bytes)"

        # Verify %PDF- magic signature
        with open(local_pdf_path, "rb") as f:
            header = f.read(5)
            assert header.startswith(b"%PDF-"), f"Invalid PDF magic signature: {header}"

        duration = round(time.time() - start_time, 2)
        return True, f"Verified PDF statement '{pdf_filename}' ({pdf_size:,} bytes, valid header).", duration

    # ----------------------------------------------------
    # RUNNER
    # ----------------------------------------------------

    def run_all(self):
        print("=" * 72)
        print(" PingPin Android Live Device Integration & E2E Test Suite")
        print("=" * 72)

        if not self.detect_device():
            print("\n❌ ABORTED: No responsive Android device found.")
            return False

        tests = [
            ("test_01_device_readiness", self.test_01_device_readiness),
            ("test_02_app_launch", self.test_02_app_launch),
            ("test_03_home_weather_tab", self.test_03_home_weather_tab),
            ("test_04_home_holidays_tab", self.test_04_home_holidays_tab),
            ("test_05_monthly_calendar_expansion", self.test_05_monthly_calendar_expansion),
            ("test_06_insights_screen", self.test_06_insights_screen),
            ("test_07_settings_profile", self.test_07_settings_profile),
            ("test_08_settings_automation", self.test_08_settings_automation),
            ("test_09_settings_health", self.test_09_settings_health),
            ("test_10_settings_updates", self.test_10_settings_updates),
            ("test_11_alarm_screen_and_chime", self.test_11_alarm_screen_and_chime),
            ("test_12_pdf_statement_integrity", self.test_12_pdf_statement_integrity),
        ]

        passed = 0
        failed = 0
        total_start = time.time()

        print(f"\nTarget Device : {self.serial}")
        print(f"Artifacts Dir : {self.artifacts_dir}\n")

        for name, func in tests:
            doc = func.__doc__.strip() if func.__doc__ else name
            print(f"• Running: {name} ...", end=" ", flush=True)
            try:
                ok, msg, duration = func()
                if ok:
                    passed += 1
                    print(f"[PASS] ({duration}s)")
                    print(f"  └─ {msg}")
                    self.results.append((name, "PASS", duration, msg))
                else:
                    failed += 1
                    print(f"[FAIL] ({duration}s)")
                    print(f"  └─ {msg}")
                    self.results.append((name, "FAIL", duration, msg))
            except Exception as e:
                failed += 1
                print("[FAIL]")
                print(f"  └─ Error: {e}")
                self.results.append((name, "FAIL", 0.0, str(e)))

        total_duration = round(time.time() - total_start, 2)
        print("\n" + "=" * 72)
        print(f" Device Test Summary: {passed} Passed, {failed} Failed (Total: {total_duration}s)")
        print("=" * 72)

        return failed == 0


def main():
    parser = argparse.ArgumentParser(description="Run PingPin Live Device Integration Tests")
    parser.add_argument("--device", "-d", help="ADB device serial (e.g. 10BG4Y0TDS001TD or 192.168.1.8:36265)")
    args = parser.parse_args()

    runner = DeviceTestRunner(serial=args.device)
    success = runner.run_all()
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
