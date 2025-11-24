import 'package:flutter/material.dart';

class LocationSwitchCard extends StatelessWidget {
  final String fromLocation;
  final String toLocation;
  final IconData fromIcon;
  final IconData toIcon;
  final VoidCallback onSwitch;

  const LocationSwitchCard({
    super.key,
    required this.fromLocation,
    required this.toLocation,
    required this.fromIcon,
    required this.toIcon,
    required this.onSwitch,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
            color: Colors.grey.withValues(alpha: 0.08),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _buildLocationRow(fromIcon, 'From', fromLocation, Colors.green),
            
            // Switch Button
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 8),
              child: Center(
                child: IconButton(
                  onPressed: onSwitch,
                  icon: Container(
                    width: 36,
                    height: 36,
                    decoration: BoxDecoration(
                      color: Colors.blue[50],
                      borderRadius: BorderRadius.circular(18),
                      border: Border.all(color: Colors.blue[100]!),
                    ),
                    child: Icon(
                      Icons.swap_vert,
                      color: Colors.blue[700],
                      size: 20,
                    ),
                  ),
                ),
              ),
            ),
            
            _buildLocationRow(toIcon, 'To', toLocation, Colors.red),
          ],
        ),
      ),
    );
  }

  Widget _buildLocationRow(IconData icon, String label, String location, MaterialColor color) {
    return Row(
      children: [
        Container(
          width: 40,
          height: 40,
          decoration: BoxDecoration(
            color: color.shade50,
            borderRadius: BorderRadius.circular(8),
          ),
          child: Icon(
            icon,
            color: color.shade700,
            size: 20,
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                label,
                style: const TextStyle(
                  fontSize: 12,
                  color: Colors.black54,
                  fontWeight: FontWeight.w500,
                ),
              ),
              const SizedBox(height: 2),
              Text(
                location,
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: Colors.black87,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}