import 'package:flutter/material.dart';
import 'package:my_app/driver/registrationPage/license_verification.dart';
import 'package:my_app/widgets/auth/auth_card.dart';
import 'package:my_app/widgets/auth/auth_button.dart';
import 'package:my_app/widgets/auth/auth_text_field.dart';
import 'package:my_app/widgets/common/section_header.dart';
import 'package:my_app/widgets/common/preference_checkbox.dart';
import 'package:my_app/widgets/auth/divider_with_text.dart';

class DriverLocationDetailsPage extends StatefulWidget {
  // final String fullName;
  // final String employeeId;
  // final String phoneNumber;
  // final String password;

  const DriverLocationDetailsPage({super.key
    // required this.fullName,
    // required this.employeeId,
    // required this.phoneNumber,
    // required this.password,
  });

  @override
  State<DriverLocationDetailsPage> createState() =>
      _DriverLocationDetailsPageState();
}

class _DriverLocationDetailsPageState extends State<DriverLocationDetailsPage> {
  final TextEditingController _homeAddressController = TextEditingController();
  final TextEditingController _workAddressController = TextEditingController();
  final TextEditingController _vehicleMakeController = TextEditingController();
  final TextEditingController _vehicleModelController = TextEditingController();
  final TextEditingController _licensePlateController = TextEditingController();

  final Map<String, TextEditingController> _officeTimeControllers = {
    'Monday': TextEditingController(text: '9:00'),
    'Tuesday': TextEditingController(text: '9:00'),
    'Wednesday': TextEditingController(text: '9:00'),
    'Thursday': TextEditingController(text: '9:00'),
    'Friday': TextEditingController(text: '9:00'),
    'Saturday': TextEditingController(text: '9:00'),
  };

  bool _preferFemaleRiders = false;

  @override
  void dispose() {
    _homeAddressController.dispose();
    _workAddressController.dispose();
    _vehicleMakeController.dispose();
    _vehicleModelController.dispose();
    _licensePlateController.dispose();
    for (var controller in _officeTimeControllers.values) {
      controller.dispose();
    }
    super.dispose();
  }

  void _navigateToLicenseVerification() {
    setState(() {
      if (_vehicleMakeController.text.trim().isEmpty) {
      } else if (_vehicleModelController.text.trim().isEmpty) {
      } else if (_licensePlateController.text.trim().isEmpty) {
      } else {
        Navigator.of(context).push(
          MaterialPageRoute(
            builder: (context) => DriverLicenseVerificationPage(
              // fullName: widget.fullName,
              // employeeId: widget.employeeId,
              // phoneNumber: widget.phoneNumber,
              // password: widget.password,
              // vehicleMake: _vehicleMakeController.text,
              // vehicleModel: _vehicleModelController.text,
              // licensePlate: _licensePlateController.text,
              // preferFemaleRiders: _preferFemaleRiders,
            ),
          ),
        );
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        elevation: 0,
        backgroundColor: Colors.white,
        iconTheme: const IconThemeData(color: Colors.black),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const SizedBox(height: 16),

            Column(
              crossAxisAlignment: CrossAxisAlignment.center,
              mainAxisSize:
                  MainAxisSize.min, // Important: prevent infinite height
              children: [
                Text(
                  'Register as Driver',
                  style: const TextStyle(
                    fontSize: 28,
                    fontWeight: FontWeight.bold,
                  ),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 8),
                const Text(
                  'Create your account to start offering rides',
                  style: TextStyle(fontSize: 16, color: Colors.black54),
                  textAlign: TextAlign.center,
                ),
              ],
            ),
            const SizedBox(height: 32),

            // Registration Process Steps Card
            AuthCard(
              children: [
                const Text(
                  'Registration Process',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                    color: Colors.black87,
                  ),
                ),
                const SizedBox(height: 16),
                _buildStep(1, 'Basic Information', true),
                const SizedBox(height: 8),
                _buildStep(2, 'Location Details', true),
                const SizedBox(height: 8),
                _buildStep(3, 'License Verification', false),
              ],
            ),
            const SizedBox(height: 32),

            // Location Details Section
            const SectionHeader(
              title: 'Location Details',
              textAlign: TextAlign.center,
              crossAxisAlignment: CrossAxisAlignment.stretch,
            ),
            const SizedBox(height: 16),

            AuthCard(
              children: [
                // Home Location
                _buildLocationSection(
                  title: 'Home Location',
                  hintText: 'Select your home address',
                ),
                const SizedBox(height: 20),

                // Work Location
                _buildLocationSection(
                  title: 'Work Location',
                  hintText: 'Select your work address',
                ),
                const SizedBox(height: 24),

                // Daily Office Login Times
                const Text(
                  'Daily Office Login Times',
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                    color: Colors.black87,
                  ),
                ),
                const SizedBox(height: 8),
                const Text(
                  'Please specify when you typically leave your office each day. This helps us match you with riders.',
                  style: TextStyle(fontSize: 14, color: Colors.black54),
                ),
                const SizedBox(height: 16),

                // Office Times Grid
                _buildOfficeTimesGrid(),
              ],
            ),
            const SizedBox(height: 24),

            // Vehicle Information Section
            const SectionHeader(
              title: 'Vehicle Information',
              textAlign: TextAlign.center,
              crossAxisAlignment: CrossAxisAlignment.stretch,
            ),
            const SizedBox(height: 16),

            AuthCard(
              children: [
                // Vehicle Make and Model Row
                Row(
                  children: [
                    Expanded(
                      child: AuthTextField(
                        label: 'Vehicle Make',
                        hintText: 'e.g. Toyota',
                        controller: _vehicleMakeController,
                      ),
                    ),
                    const SizedBox(width: 16),
                    Expanded(
                      child: AuthTextField(
                        label: 'Vehicle Model',
                        hintText: 'e.g. Camry',
                        controller: _vehicleModelController,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 16),

                // License Plate
                AuthTextField(
                  label: 'License Plate',
                  hintText: 'e.g. ABC 1234',
                  controller: _licensePlateController,
                ),
                const SizedBox(height: 16),

                // Preference Checkbox
                PreferenceCheckbox(
                  label: 'Prefer female riders only',
                  value: _preferFemaleRiders,
                  onChanged: (value) {
                    setState(() {
                      _preferFemaleRiders = value ?? false;
                    });
                  },
                ),
              ],
            ),
            const SizedBox(height: 32),

            // Navigation Buttons
            Row(
              children: [
                Expanded(
                  child: AuthButton(
                    text: 'Back',
                    onPressed: () => Navigator.of(context).pop(),
                    isPrimary: false,
                  ),
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: AuthButton(
                    text: 'Continue to License Verification',
                    onPressed: _navigateToLicenseVerification,
                    isPrimary: true,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 24),

            const DividerWithText(text: 'Already have an account?'),
            const SizedBox(height: 16),

            AuthButton(
              text: 'Sign In',
              onPressed: () {
                Navigator.of(context).popUntil((route) => route.isFirst);
              },
              isPrimary: false,
            ),

            const SizedBox(height: 32),
          ],
        ),
      ),
    );
  }

  Widget _buildStep(int stepNumber, String title, bool isActive) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      decoration: BoxDecoration(
        color: isActive ? Colors.blue[100] : Colors.transparent,
        borderRadius: BorderRadius.circular(8),
        border: isActive ? Border.all(color: Colors.blue[300]!) : null,
      ),
      child: Row(
        children: [
          Container(
            width: 28,
            height: 28,
            decoration: BoxDecoration(
              color: isActive ? const Color(0xFF2563EB) : Colors.grey[400],
              shape: BoxShape.circle,
            ),
            child: Center(
              child: Text(
                stepNumber.toString(),
                style: const TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.w600,
                  fontSize: 14,
                ),
              ),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              title,
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w500,
                color: isActive ? const Color(0xFF2563EB) : Colors.grey[700],
              ),
            ),
          ),
          if (isActive)
            const Icon(Icons.check_circle, color: Color(0xFF2563EB), size: 20),
        ],
      ),
    );
  }

  Widget _buildLocationSection({
    required String title,
    required String hintText,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
              child: AuthTextField(
                label: title,
                hintText: hintText,
              ),
            ),
            const SizedBox(width: 6),
            Column(
              children: [
                const SizedBox(height: 25,),
                SizedBox(
                  height: 50,
                  child: OutlinedButton(
                    onPressed: () {},
                    style: OutlinedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(horizontal: 16),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(8),
                      ),
                      side: const BorderSide(color: Colors.grey),
                    ),
                    child: const Text(
                      'Select on Map',
                      style: TextStyle(fontSize: 14, color: Colors.black87),
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildOfficeTimesGrid() {
    final dayPairs = [
      ['Monday', 'Tuesday'],
      ['Wednesday', 'Thursday'],
      ['Friday', 'Saturday'],
    ];

    return Column(
      children: dayPairs.map((pair) {
        return Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: Row(
            children: pair.map((day) {
              final isSaturday = day == 'Saturday';
              final controller = _officeTimeControllers[day]!;

              return Expanded(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 4),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        isSaturday ? '$day (optional)' : day,
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w500,
                          color: isSaturday ? Colors.grey[600] : Colors.black87,
                        ),
                      ),
                      const SizedBox(height: 6),
                      Container(
                        height: 44,
                        padding: const EdgeInsets.symmetric(horizontal: 12),
                        decoration: BoxDecoration(
                          border: Border.all(color: Colors.grey[400]!),
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Row(
                          children: [
                            Expanded(
                              child: TextFormField(
                                controller: controller,
                                style: const TextStyle(fontSize: 14),
                                textAlign: TextAlign.center,
                                decoration: const InputDecoration(
                                  border: InputBorder.none,
                                  contentPadding: EdgeInsets.zero,
                                  isDense: true,
                                  hintText: '9:00',
                                  hintStyle: TextStyle(color: Colors.grey),
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              );
            }).toList(),
          ),
        );
      }).toList(),
    );
  }
}
