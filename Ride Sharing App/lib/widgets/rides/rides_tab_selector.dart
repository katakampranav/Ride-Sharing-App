import 'package:flutter/material.dart';

class RidesTabSelector extends StatelessWidget {
  final bool showUpcomingRides;
  final ValueChanged<bool> onTabChanged;

  const RidesTabSelector({
    super.key,
    required this.showUpcomingRides,
    required this.onTabChanged,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        _buildTab('Upcoming Rides', showUpcomingRides),
        const SizedBox(width: 24),
        _buildTab('Past Rides', !showUpcomingRides),
      ],
    );
  }

  Widget _buildTab(String text, bool isSelected) {
    return GestureDetector(
      onTap: () => onTabChanged(text == 'Upcoming Rides'),
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
        decoration: BoxDecoration(
          border: Border(
            bottom: BorderSide(
              color: isSelected ? const Color(0xFF2563EB) : Colors.transparent,
              width: 3,
            ),
          ),
        ),
        child: Text(
          text,
          style: TextStyle(
            fontSize: 16,
            fontWeight: isSelected ? FontWeight.w600 : FontWeight.normal,
            color: isSelected ? const Color(0xFF2563EB) : Colors.black54,
          ),
        ),
      ),
    );
  }
}