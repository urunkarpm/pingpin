/**
 * Component Testing Harness for PingPin UI Components & State Verification
 * 
 * Provides mock data builders, property validators, accessibility assertions,
 * and contract checks for Jetpack Compose UI component logic.
 */

/**
 * Generate mock attendance data for CalendarView and Weekly views
 * @param {number} year 
 * @param {number} month (1-indexed)
 */
function createMockAttendanceData(year = 2026, month = 8) {
  const records = {};
  const daysInMonth = new Date(year, month, 0).getDate();
  
  for (let day = 1; day <= daysInMonth; day++) {
    const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    const dayOfWeek = new Date(year, month - 1, day).getDay();
    
    if (day === 15) {
      records[dateStr] = { status: 'HOLIDAY', label: 'Independence Day' };
    } else if (dayOfWeek === 0 || dayOfWeek === 6) {
      records[dateStr] = { status: 'OFF_DAY', label: 'Weekend' };
    } else if (day < 28) {
      records[dateStr] = { status: 'PRESENT', label: 'Present / WFO', checkIn: '09:12 AM', checkOut: '06:05 PM' };
    } else {
      records[dateStr] = { status: 'MISSED', label: 'Missed WFO' };
    }
  }
  return records;
}

/**
 * Mock 2026 Indian Holidays Dataset for UpcomingHolidaysCard
 */
const MOCK_INDIAN_HOLIDAYS_2026 = [
  { id: '1', date: '2026-01-26', name: 'Republic Day', category: 'NATIONAL', isLongWeekend: true, dayOfWeek: 'Monday' },
  { id: '2', date: '2026-03-25', name: 'Holi', category: 'FESTIVAL', isLongWeekend: false, dayOfWeek: 'Wednesday' },
  { id: '3', date: '2026-04-14', name: 'Dr. Ambedkar Jayanti', category: 'REGIONAL', isLongWeekend: false, dayOfWeek: 'Tuesday' },
  { id: '4', date: '2026-08-15', name: 'Independence Day', category: 'NATIONAL', isLongWeekend: true, dayOfWeek: 'Saturday' },
  { id: '5', date: '2026-10-02', name: 'Mahatma Gandhi Jayanti', category: 'NATIONAL', isLongWeekend: true, dayOfWeek: 'Friday' },
  { id: '6', date: '2026-10-20', name: 'Dussehra', category: 'FESTIVAL', isLongWeekend: false, dayOfWeek: 'Tuesday' },
  { id: '7', date: '2026-11-08', name: 'Diwali', category: 'FESTIVAL', isLongWeekend: true, dayOfWeek: 'Sunday' },
  { id: '8', date: '2026-12-25', name: 'Christmas', category: 'NATIONAL', isLongWeekend: true, dayOfWeek: 'Friday' }
];

/**
 * Validate GlassCard props contract
 */
function validateGlassCardProps(props) {
  if (props.cornerRadius !== undefined && typeof props.cornerRadius !== 'number') {
    throw new TypeError('GlassCard cornerRadius must be a number (dp)');
  }
  if (props.onClick !== undefined && typeof props.onClick !== 'function') {
    throw new TypeError('GlassCard onClick must be a function if provided');
  }
  return true;
}

/**
 * Validate PingPinSwitch state transition
 */
function togglePingPinSwitch(currentState, enabled = true) {
  if (!enabled) return currentState;
  return !currentState;
}

/**
 * Calculate ProgressRadialRing sweep angle and color token
 * @param {number} current 
 * @param {number} target 
 */
function calculateRadialProgress(current, target) {
  if (target <= 0) return { percentage: 0, colorCategory: 'RED' };
  const percentage = Math.min(Math.round((current / target) * 100), 100);
  let colorCategory = 'RED';
  if (percentage >= 80) colorCategory = 'GREEN';
  else if (percentage >= 50) colorCategory = 'YELLOW';
  return { percentage, colorCategory };
}

/**
 * Compute Time Picker formatted string
 * @param {number} hour (0-23)
 * @param {number} minute (0-59)
 */
function formatTimePickerString(hour, minute) {
  const ampm = hour >= 12 ? 'PM' : 'AM';
  const displayHour = hour % 12 === 0 ? 12 : hour % 12;
  const displayMinute = String(minute).padStart(2, '0');
  return `${displayHour}:${displayMinute} ${ampm}`;
}

module.exports = {
  createMockAttendanceData,
  MOCK_INDIAN_HOLIDAYS_2026,
  validateGlassCardProps,
  togglePingPinSwitch,
  calculateRadialProgress,
  formatTimePickerString
};
