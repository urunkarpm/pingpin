import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/widgets/glass_card.dart';
import '../../providers/ble_laptop_provider.dart';

class OfficeOccupancyCard extends ConsumerWidget {
  const OfficeOccupancyCard({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(bleLaptopNotifierProvider);
    final notifier = ref.read(bleLaptopNotifierProvider.notifier);
    final theme = Theme.of(context);
    final isDark = theme.brightness == Brightness.dark;
    final onSurface = theme.colorScheme.onSurface;

    return GlassCard(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header Row
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: theme.colorScheme.primary.withValues(alpha: 0.12),
                      shape: BoxShape.circle,
                    ),
                    child: Icon(
                      Icons.bluetooth_searching_rounded,
                      color: theme.colorScheme.primary,
                      size: 22,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Text(
                    'Office Occupancy',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w700,
                      color: onSurface,
                      letterSpacing: -0.2,
                    ),
                  ),
                ],
              ),
              ElevatedButton.icon(
                onPressed: state.isScanning
                    ? null
                    : () {
                        HapticFeedback.lightImpact();
                        notifier.startScan();
                      },
                icon: state.isScanning
                    ? const SizedBox(
                        width: 14,
                        height: 14,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.radar_rounded, size: 16),
                label: Text(state.isScanning ? 'Scanning...' : 'Scan Now'),
                style: ElevatedButton.styleFrom(
                  elevation: 0,
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
              ),
            ],
          ),

          const SizedBox(height: 16),
          Divider(
            height: 1,
            color: onSurface.withValues(alpha: isDark ? 0.1 : 0.06),
          ),
          const SizedBox(height: 16),

          // Scan Results or Initial State
          if (state.errorMessage != null) ...[
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.red.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Text(
                state.errorMessage!,
                style: const TextStyle(color: Colors.red, fontSize: 13),
              ),
            ),
          ] else if (state.result != null) ...[
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _buildCountBadge(
                  context,
                  label: 'People in Office',
                  count: '${state.result!.totalCount}',
                  icon: Icons.people_alt_rounded,
                ),
                _buildCountBadge(
                  context,
                  label: 'Windows Laptops',
                  count: '${state.result!.windowsCount}',
                  icon: Icons.desktop_windows_rounded,
                ),
                _buildCountBadge(
                  context,
                  label: 'MacBooks',
                  count: '${state.result!.macCount}',
                  icon: Icons.laptop_mac_rounded,
                ),
              ],
            ),
          ] else ...[
            Center(
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 8),
                child: Text(
                  'Tap "Scan Now" to detect active office laptops in BLE range.',
                  style: TextStyle(
                    fontSize: 13,
                    color: onSurface.withValues(alpha: 0.6),
                    fontStyle: FontStyle.italic,
                  ),
                  textAlign: TextAlign.center,
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildCountBadge(
    BuildContext context, {
    required String label,
    required String count,
    required IconData icon,
  }) {
    final theme = Theme.of(context);
    final onSurface = theme.colorScheme.onSurface;
    return Column(
      children: [
        Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: onSurface.withValues(alpha: 0.05),
            shape: BoxShape.circle,
          ),
          child: Icon(icon, color: onSurface.withValues(alpha: 0.85), size: 20),
        ),
        const SizedBox(height: 6),
        Text(
          count,
          style: TextStyle(
            fontSize: 22,
            fontWeight: FontWeight.w800,
            color: onSurface,
            letterSpacing: -0.5,
          ),
        ),
        Text(
          label,
          style: TextStyle(
            fontSize: 11,
            fontWeight: FontWeight.w500,
            color: onSurface.withValues(alpha: 0.6),
          ),
        ),
      ],
    );
  }
}

