# PingPin v2.6.0 Release

## What's New & Refined

### Edge-to-Edge Status Bar Blur & Visual Isolation
- **Edge-to-Edge Status Bar Backdrop Blur**: Extended `16dp` backdrop blur edge-to-edge behind the Android status bar for expanded calendar, weather detail, and holiday sheets, leaving system clock and icons crisp and readable.
- **Selective Background Blur for Calendar**: When expanding the monthly calendar, background surroundings are blurred while the calendar grid itself remains 100% sharp, readable, and clear.

### De-congested Settings UX
- **Compact Profile Hero Header**: Merged avatar (42dp), completion status pill, and quick dark/light theme toggle into a single compact horizontal header card.
- **Clean Segmented Category Selector**: Upgraded category tabs (`Profile`, `Automation`, `Health`, `Updates`) to eliminate text truncation and provide generous tab padding.
- **Reclaimed Vertical Real Estate**: Reclaimed over `92dp` of vertical screen height, making input controls immediately visible without initial scrolling.

### Initial Setup (Onboarding) UX Refinement
- **7-Dot Step Indicator Bar**: Upgraded onboarding wizard navigation to a clean 7-dot step indicator bar featuring active ElectricBlue highlighting (26dp) and completed EmeraldGreen checkmarks.
- **Contextual Action Buttons**: Dynamic button labels (`Next: Office Wi-Fi`, `Next: Shift Timings`, `Next: HR Portal`, `Next: Test Run`, `Next: Review & Launch`).
- **Live Avatar Preview**: Dynamic initial avatar badge in Step 1 updating in real-time as users type their full name.

### Scalability & Grid Symmetry
- **Multi-Form Factor Scalability**: Integrated `WindowSizeUtils` adaptive breakpoint engine switching from floating bottom bar (`LiquidGlassNavBar`) on compact phones to side rail (`LiquidGlassNavRail`) and dual-pane split view on foldables, tablets, and landscape.
- **Equal Grid Padding**: Equalized day cell spacing and row padding (`4.dp` uniform gaps) across the 7x6 monthly calendar grid.
- **Bottom Navigation Label**: Renamed middle tab to **Insights** with matching icons.
