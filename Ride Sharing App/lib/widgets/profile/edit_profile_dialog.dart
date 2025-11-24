import 'package:flutter/material.dart';
import 'package:my_app/widgets/auth/auth_card.dart';
import 'package:my_app/widgets/auth/auth_button.dart';
import 'package:my_app/widgets/auth/auth_text_field.dart';

class EditProfileDialog extends StatelessWidget {
  final TextEditingController nameController;
  final TextEditingController phoneController;
  final TextEditingController emailController;
  final TextEditingController vehicleController;
  final TextEditingController licenseController;
  final TextEditingController driverLicenseController;
  final VoidCallback onSave;

  const EditProfileDialog({
    super.key,
    required this.nameController,
    required this.phoneController,
    required this.emailController,
    required this.vehicleController,
    required this.licenseController,
    required this.driverLicenseController,
    required this.onSave,
  });

  @override
  Widget build(BuildContext context) {
    return Dialog(
      insetPadding: const EdgeInsets.all(20),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: ConstrainedBox(
        constraints: BoxConstraints(
          maxHeight: MediaQuery.of(context).size.height * 0.8, // Limit maximum height
        ),
        child: SingleChildScrollView(
          child: AuthCard(
            padding: const EdgeInsets.all(20), // Reduced padding
            children: [
              // Header
              const Text(
                "Edit Profile",
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                  color: Colors.black87,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 16), // Reduced spacing

              // Name Field
              AuthTextField(
                label: "Full Name",
                hintText: "Enter your full name",
                controller: nameController,
              ),
              const SizedBox(height: 12), // Reduced spacing

              // Phone Field
              AuthTextField(
                label: "Phone Number",
                hintText: "Enter your phone number",
                controller: phoneController,
                keyboardType: TextInputType.phone,
              ),
              const SizedBox(height: 12), // Reduced spacing

              // Email Field
              AuthTextField(
                label: "Email Address",
                hintText: "Enter your email address",
                controller: emailController,
                keyboardType: TextInputType.emailAddress,
              ),
              const SizedBox(height: 12), // Reduced spacing

              // Vehicle Field
              AuthTextField(
                label: "Vehicle Model",
                hintText: "Enter your vehicle model",
                controller: vehicleController,
              ),
              const SizedBox(height: 12), // Reduced spacing

              // License Plate Field
              AuthTextField(
                label: "License Plate",
                hintText: "Enter your license plate",
                controller: licenseController,
              ),
              const SizedBox(height: 12), // Reduced spacing

              // Driver License Field
              AuthTextField(
                label: "Driver License",
                hintText: "Enter your driver license",
                controller: driverLicenseController,
              ),
              const SizedBox(height: 20), // Reduced spacing

              // Buttons
              Row(
                children: [
                  // Cancel Button
                  Expanded(
                    child: AuthButton(
                      text: "Cancel",
                      onPressed: () => Navigator.of(context).pop(),
                      isPrimary: false,
                    ),
                  ),
                  const SizedBox(width: 12),

                  // Save Button
                  Expanded(
                    child: AuthButton(
                      text: "Save",
                      onPressed: onSave,
                      isPrimary: true,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8), // Small bottom padding
            ],
          ),
        ),
      ),
    );
  }
}