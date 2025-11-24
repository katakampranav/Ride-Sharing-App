import 'package:flutter/material.dart';
import 'package:my_app/widgets/auth/auth_card.dart';

class ProfileHeader extends StatelessWidget {
  final String name;
  final String imageUrl;
  final double rating;
  final int rideCount;
  final bool isVerified;

  const ProfileHeader({
    super.key,
    required this.name,
    required this.imageUrl,
    required this.rating,
    required this.rideCount,
    required this.isVerified,
  });

  @override
  Widget build(BuildContext context) {
    return AuthCard(
      padding: const EdgeInsets.all(24),
      children: [
        // Profile Picture
        CircleAvatar(
          radius: 40,
          backgroundColor: Colors.grey,
          child: Icon(Icons.person, size: 40, color: Colors.white),
        ),
        const SizedBox(height: 16),

        // Name
        Center(
          child: Text(
            name,
            style: const TextStyle(
              fontSize: 24,
              fontWeight: FontWeight.bold,
              color: Colors.black87,
            ),
          ),
        ),
        const SizedBox(height: 8),

        // Rating and rides
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.star, color: Colors.amber, size: 20),
            const SizedBox(width: 4),
            Text(
              rating.toStringAsFixed(1),
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w600,
                color: Colors.black87,
              ),
            ),
            const SizedBox(width: 4),
            Text(
              '($rideCount rides)',
              style: const TextStyle(
                fontSize: 16,
                color: Colors.black54,
              ),
            ),
          ],
        ),
        const SizedBox(height: 8),

        // Verified badge (conditionally shown)
        if (isVerified) ...[
          Container(
            padding: const EdgeInsets.symmetric(
              horizontal: 12,
              vertical: 12,
            ),
            decoration: BoxDecoration(
              color: Colors.green[50],
              borderRadius: BorderRadius.circular(12),
            ),
            child: Center(
              child: const Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.verified, size: 16, color: Colors.green),
                  SizedBox(width: 6),
                  Text(
                    'Verified',
                    style: TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      color: Colors.green,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ],
    );
  }
}