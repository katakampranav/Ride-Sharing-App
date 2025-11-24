import 'package:flutter/material.dart';

class RideDetailsSection extends StatelessWidget {
  final String pickupTime;
  final String eta;
  final String carModel;
  final String licensePlate;

  const RideDetailsSection({
    super.key,
    required this.pickupTime,
    required this.eta,
    required this.carModel,
    required this.licensePlate,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Row(
          children: [
            const Icon(Icons.access_time, size: 18, color: Colors.black54),
            const SizedBox(width: 8),
            const Text('Pickup:', style: TextStyle(color: Colors.black54)),
            const SizedBox(width: 8),
            Text(
              pickupTime,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
            const Spacer(),
            const Icon(Icons.timer, size: 18, color: Colors.black54),
            const SizedBox(width: 8),
            const Text('ETA:', style: TextStyle(color: Colors.black54)),
            const SizedBox(width: 8),
            Text(
              eta,
              style: const TextStyle(fontWeight: FontWeight.w600),
            ),
          ],
        ),
        const SizedBox(height: 8),
        Container(
          decoration: BoxDecoration(
            color: Colors.grey.shade100,
            borderRadius: BorderRadius.circular(12),
          ),
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              const Icon(Icons.directions_car, size: 18, color: Colors.black54),
              const SizedBox(width: 8),
              Text(
                '$carModel • $licensePlate',
                style: const TextStyle(fontWeight: FontWeight.w600),
              ),
            ],
          ),
        ),
      ],
    );
  }
}