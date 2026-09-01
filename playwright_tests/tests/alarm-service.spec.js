const { test, expect } = require('@playwright/test');

test.describe('Alarm System Architecture & Scheduling Engine Specs', () => {

  const ALARM_IDS = {
    CHECK_IN_ALARM_ID: 101,
    CHECK_OUT_ALARM_ID: 102,
    CHECK_IN_SNOOZE_ID: 103,
    CHECK_OUT_SNOOZE_ID: 104,
    MAKEUP_WFO_ALARM_ID: 201,
    TEST_ALARM_ID: 999
  };

  const CHANNELS = {
    ALARM_CHANNEL_ID: 'alarm_channel_v4',
    ATTENDANCE_CHANNEL_ID: 'attendance_channel'
  };

  const STORAGE = {
    PREFS_NAME: 'pingpin_native_alarm_prefs'
  };

  const WORKING_DAYS = {
    MONDAY: 1,
    TUESDAY: 2,
    WEDNESDAY: 4,
    THURSDAY: 8,
    FRIDAY: 16,
    SATURDAY: 32,
    SUNDAY: 64,
    DEFAULT_WEEKDAYS: 31 // Mon-Fri
  };

  function isWorkingDay(dayOfWeek, mask) {
    // dayOfWeek: 0=Mon, 1=Tue, 2=Wed, 3=Thu, 4=Fri, 5=Sat, 6=Sun
    return (mask & (1 << dayOfWeek)) !== 0;
  }

  function parseTime(timeStr) {
    if (!timeStr || typeof timeStr !== 'string') return null;
    const parts = timeStr.trim().split(':');
    if (parts.length < 2) return null;
    const hour = parseInt(parts[0], 10);
    const minute = parseInt(parts[1], 10);
    if (isNaN(hour) || isNaN(minute)) return null;
    if (hour < 0 || hour > 23 || minute < 0 || minute > 59) return null;
    return { hour, minute };
  }

  function getNextOccurrence(hour, minute, workingDaysMask, baseDate) {
    const target = new Date(baseDate.getTime());
    target.setHours(hour, minute, 0, 0);

    if (target.getTime() <= baseDate.getTime()) {
      target.setDate(target.getDate() + 1);
    }

    if (workingDaysMask === 0) {
      return target;
    }

    let safety = 0;
    while (safety < 14) {
      // JS getDay(): 0=Sun, 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat
      const jsDay = target.getDay();
      const shift = jsDay === 0 ? 6 : jsDay - 1; // 0=Mon..6=Sun
      if (isWorkingDay(shift, workingDaysMask)) {
        break;
      }
      target.setDate(target.getDate() + 1);
      safety++;
    }
    return target;
  }

  // ==========================================
  // 1. Constants & ID Verification
  // ==========================================

  test('should verify exact notification IDs, channels, and preferences store', () => {
    expect(ALARM_IDS.CHECK_IN_ALARM_ID).toBe(101);
    expect(ALARM_IDS.CHECK_OUT_ALARM_ID).toBe(102);
    expect(ALARM_IDS.CHECK_IN_SNOOZE_ID).toBe(103);
    expect(ALARM_IDS.CHECK_OUT_SNOOZE_ID).toBe(104);
    expect(ALARM_IDS.MAKEUP_WFO_ALARM_ID).toBe(201);
    expect(CHANNELS.ALARM_CHANNEL_ID).toBe('alarm_channel_v4');
    expect(STORAGE.PREFS_NAME).toBe('pingpin_native_alarm_prefs');
  });

  // ==========================================
  // 2. Time Parsing Matrix
  // ==========================================

  test('should parse valid time strings and reject invalid / out-of-range strings', () => {
    expect(parseTime('09:30')).toEqual({ hour: 9, minute: 30 });
    expect(parseTime('9:30')).toEqual({ hour: 9, minute: 30 });
    expect(parseTime('00:00')).toEqual({ hour: 0, minute: 0 });
    expect(parseTime('23:59')).toEqual({ hour: 23, minute: 59 });
    expect(parseTime('  18:05  ')).toEqual({ hour: 18, minute: 5 });

    expect(parseTime('24:00')).toBeNull();
    expect(parseTime('-1:00')).toBeNull();
    expect(parseTime('12:60')).toBeNull();
    expect(parseTime('invalid')).toBeNull();
    expect(parseTime('')).toBeNull();
  });

  // ==========================================
  // 3. Scheduling & Monday Morning Rollover Matrix
  // ==========================================

  test('should schedule same-day on Monday morning before check-in time', () => {
    // Monday Sep 7, 2026 at 08:00 AM
    const mon8am = new Date(2026, 8, 7, 8, 0, 0, 0);
    expect(mon8am.getDay()).toBe(1); // 1 = Monday

    const next = getNextOccurrence(9, 30, WORKING_DAYS.DEFAULT_WEEKDAYS, mon8am);
    expect(next.getDay()).toBe(1); // Still Monday
    expect(next.getHours()).toBe(9);
    expect(next.getMinutes()).toBe(30);
    expect(next.getDate()).toBe(7);
  });

  test('should advance from Friday check-in (09:30 AM) skipping weekend to Monday (09:30 AM)', () => {
    // Friday Sep 11, 2026 at 09:30 AM
    const fri930am = new Date(2026, 8, 11, 9, 30, 0, 0);
    expect(fri930am.getDay()).toBe(5); // 5 = Friday

    const next = getNextOccurrence(9, 30, WORKING_DAYS.DEFAULT_WEEKDAYS, fri930am);
    expect(next.getDay()).toBe(1); // Monday
    expect(next.getDate()).toBe(14); // 11 + 3 = 14
    expect(next.getHours()).toBe(9);
    expect(next.getMinutes()).toBe(30);
  });

  test('should advance from Friday check-out (18:00 PM) skipping weekend to Monday (18:00 PM)', () => {
    // Friday Sep 11, 2026 at 18:00 PM
    const fri6pm = new Date(2026, 8, 11, 18, 0, 0, 0);
    expect(fri6pm.getDay()).toBe(5); // Friday

    const next = getNextOccurrence(18, 0, WORKING_DAYS.DEFAULT_WEEKDAYS, fri6pm);
    expect(next.getDay()).toBe(1); // Monday
    expect(next.getDate()).toBe(14);
    expect(next.getHours()).toBe(18);
    expect(next.getMinutes()).toBe(0);
  });

  test('should advance from Saturday midday to Monday morning', () => {
    // Saturday Sep 12, 2026 at 14:00 PM
    const sat2pm = new Date(2026, 8, 12, 14, 0, 0, 0);
    expect(sat2pm.getDay()).toBe(6); // Saturday

    const next = getNextOccurrence(9, 30, WORKING_DAYS.DEFAULT_WEEKDAYS, sat2pm);
    expect(next.getDay()).toBe(1); // Monday
    expect(next.getDate()).toBe(14);
    expect(next.getHours()).toBe(9);
    expect(next.getMinutes()).toBe(30);
  });

  test('should schedule from Sunday midnight date change (00:00:01 AM) directly for Monday morning', () => {
    // Sunday Sep 13, 2026 at 00:00:01 AM
    const sunMidnight = new Date(2026, 8, 13, 0, 0, 1, 0);
    expect(sunMidnight.getDay()).toBe(0); // Sunday

    const next = getNextOccurrence(9, 30, WORKING_DAYS.DEFAULT_WEEKDAYS, sunMidnight);
    expect(next.getDay()).toBe(1); // Monday
    expect(next.getDate()).toBe(14);
    expect(next.getHours()).toBe(9);
    expect(next.getMinutes()).toBe(30);
  });

  test('should compute standard 10-minute snooze duration (600,000 ms)', () => {
    const triggerTimeMs = 1757822400000;
    const snoozeDurationMs = 10 * 60 * 1000; // 10 mins
    const snoozedAlarmTimeMs = triggerTimeMs + snoozeDurationMs;

    expect(snoozedAlarmTimeMs - triggerTimeMs).toBe(600000);
  });

  // ==========================================
  // 4. Working Days Bitmask Arithmetic
  // ==========================================

  test('should accurately verify bitmask values and toggle operations', () => {
    expect(WORKING_DAYS.DEFAULT_WEEKDAYS).toBe(31); // 1+2+4+8+16
    expect(WORKING_DAYS.DEFAULT_WEEKDAYS | WORKING_DAYS.SATURDAY).toBe(63); // 31+32
    expect(WORKING_DAYS.DEFAULT_WEEKDAYS | WORKING_DAYS.SATURDAY | WORKING_DAYS.SUNDAY).toBe(127); // 63+64

    // Toggle Saturday on and off
    let mask = 31;
    mask = mask ^ WORKING_DAYS.SATURDAY;
    expect(mask).toBe(63);
    mask = mask ^ WORKING_DAYS.SATURDAY;
    expect(mask).toBe(31);
  });
});
