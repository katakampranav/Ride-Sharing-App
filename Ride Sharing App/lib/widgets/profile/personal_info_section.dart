import 'package:flutter/material.dart';
import 'package:my_app/widgets/auth/auth_card.dart';
import 'package:my_app/widgets/profile/info_item.dart';

class PersonalInfoSection extends StatelessWidget {
  final String phone;
  final String email;
  final String vehicleModel;
  final String licensePlate;
  final String driverLicense;
  final String memberSince;

  const PersonalInfoSection({
    super.key,
    required this.phone,
    required this.email,
    required this.vehicleModel,
    required this.licensePlate,
    required this.driverLicense,
    required this.memberSince,
  });

  @override
  Widget build(BuildContext context) {
    return AuthCard(
      children: [
        const Text(
          'Personal Information',
          style: TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.w600,
            color: Colors.black87,
          ),
        ),
        const SizedBox(height: 20),

        // Phone Number
        InfoItem(
          icon: Icons.phone,
          title: 'Phone Number',
          value: phone,
        ),
        const SizedBox(height: 16),

        // Email
        InfoItem(
          icon: Icons.email,
          title: 'Email',
          value: email,
        ),
        const SizedBox(height: 16),

        // Vehicle
        InfoItem(
          icon: Icons.directions_car,
          title: 'Vehicle',
          value: vehicleModel,
          subtitle: 'License: $licensePlate',
        ),
        const SizedBox(height: 16),

        // Driver License
        InfoItem(
          icon: Icons.credit_card,
          title: 'Driver License',
          value: driverLicense,
          subtitle: 'Expires: 12/25/2025',
        ),
        const SizedBox(height: 16),

        // Member Since
        InfoItem(
          icon: Icons.calendar_today,
          title: 'Member Since',
          value: memberSince,
        ),
      ],
    );
  }
}