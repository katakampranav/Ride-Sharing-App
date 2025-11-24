import 'package:flutter/material.dart';
import 'package:my_app/loginPage/login_page.dart';
import 'package:my_app/widgets/auth/auth_card.dart';
import 'package:my_app/widgets/auth/auth_button.dart';
import 'package:my_app/widgets/auth/auth_text_field.dart';
import 'package:my_app/widgets/common/section_header.dart';
import 'package:my_app/widgets/common/success_message_card.dart';
import 'package:my_app/widgets/forms/date_time_input.dart';

class DriverLicenseVerificationPage extends StatefulWidget {
  // final String fullName;
  // final String employeeId;
  // final String phoneNumber;
  // final String password;
  // final String vehicleMake;
  // final String vehicleModel;
  // final String licensePlate;
  // final bool preferFemaleRiders;

  const DriverLicenseVerificationPage({
    Key? key,
    // required this.fullName,
    // required this.employeeId,
    // required this.phoneNumber,
    // required this.password,
    // required this.vehicleMake,
    // required this.vehicleModel,
    // required this.licensePlate,
    // required this.preferFemaleRiders,
  }) : super(key: key);

  @override
  State<DriverLicenseVerificationPage> createState() =>
      _DriverLicenseVerificationPageState();
}

class _DriverLicenseVerificationPageState
    extends State<DriverLicenseVerificationPage> {
  final TextEditingController _licenseNumberController =
      TextEditingController();
  final TextEditingController _expiryDateController = TextEditingController();

  String? _frontLicenseImage;
  String? _backLicenseImage;

  @override
  void dispose() {
    _licenseNumberController.dispose();
    _expiryDateController.dispose();
    super.dispose();
  }

  void _submitForVerification() {
    setState(() {
      if (_licenseNumberController.text.trim().isEmpty) {
      } else if (_expiryDateController.text.isEmpty) {
      } else if (_frontLicenseImage == null) {
      } else if (_backLicenseImage == null) {
      } else {
        // Here you would typically submit all the data to your backend
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Registration submitted for verification!'),
          ),
        );

        Navigator.of(context).push(
          MaterialPageRoute(builder: (context) => const LoginPage()),
        );
      }
    });
  }

  Future<void> _pickImage(bool isFront) async {
    // TODO: Implement image picker functionality
    // For now, we'll simulate image selection
    setState(() {
      if (isFront) {
        _frontLicenseImage = 'selected';
      } else {
        _backLicenseImage = 'selected';
      }
    });

    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('${isFront ? 'Front' : 'Back'} license image selected'),
      ),
    );
  }

  Future<void> _selectDate() async {
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: DateTime.now(),
      firstDate: DateTime.now(),
      lastDate: DateTime(2100),
    );
    if (picked != null) {
      setState(() {
        _expiryDateController.text =
            "${picked.month}/${picked.day}/${picked.year}";
      });
    }
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
                _buildStep(3, 'License Verification', true),
              ],
            ),
            const SizedBox(height: 32),

            // License Information Section
            const SectionHeader(
              title: 'License Information',
              textAlign: TextAlign.center,
              crossAxisAlignment: CrossAxisAlignment.stretch,
            ),
            const SizedBox(height: 16),

            AuthCard(
              children: [
                // License Number
                AuthTextField(
                  label: 'License Number',
                  hintText: 'Enter your license number',
                  controller: _licenseNumberController,
                ),
                const SizedBox(height: 16),

                // Expiry Date
                DateTimeInput(
                  controller: _expiryDateController,
                  hintText: 'mm/dd/yyyy',
                  suffixIcon: const Icon(Icons.calendar_today, size: 20),
                  onTap: _selectDate,
                ),
              ],
            ),
            const SizedBox(height: 24),

            // License Upload Section
            const SectionHeader(
              title: 'License Upload',
              textAlign: TextAlign.center,
              crossAxisAlignment: CrossAxisAlignment.stretch,
            ),
            const SizedBox(height: 16),

            AuthCard(
              children: [
                // Upload License Front
                _buildUploadSection(
                  title: 'Upload License Front',
                  isUploaded: _frontLicenseImage != null,
                  onTap: () => _pickImage(true),
                ),
                const SizedBox(height: 20),

                // Upload License Back
                _buildUploadSection(
                  title: 'Upload License Back',
                  isUploaded: _backLicenseImage != null,
                  onTap: () => _pickImage(false),
                ),
              ],
            ),
            const SizedBox(height: 24),

            // Verification Info Card
            const SuccessMessageCard(
              message:
                  'Your license will be verified by our team within 24-48 hours. You\'ll receive an email notification once verification is complete.',
              backgroundColor: Color(0xFFEFF6FF),
              textColor: Color(0xFF2563EB),
            ),
            const SizedBox(height: 32),

            // Submit Button
            AuthButton(
              text: 'Submit for Verification',
              onPressed: _submitForVerification,
              isPrimary: true,
            ),
            const SizedBox(height: 24),
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

  Widget _buildUploadSection({
    required String title,
    required bool isUploaded,
    required VoidCallback onTap,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          title,
          style: const TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w500,
            color: Colors.black87,
          ),
        ),
        const SizedBox(height: 8),
        GestureDetector(
          onTap: onTap,
          child: Container(
            width: double.infinity,
            padding: const EdgeInsets.all(24),
            decoration: BoxDecoration(
              border: Border.all(
                color: isUploaded ? Colors.green : Colors.grey,
                width: 2,
              ),
              borderRadius: BorderRadius.circular(12),
              color: isUploaded ? Colors.green[50] : Colors.grey[50],
            ),
            child: Column(
              children: [
                Icon(
                  isUploaded ? Icons.check_circle : Icons.cloud_upload,
                  size: 48,
                  color: isUploaded ? Colors.green : Colors.grey[600],
                ),
                const SizedBox(height: 12),
                Text(
                  isUploaded
                      ? 'File Uploaded Successfully'
                      : 'Click to upload or drag and drop',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w500,
                    color: isUploaded ? Colors.green : Colors.black87,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  'PNG, JPG up to 5MB',
                  style: TextStyle(
                    fontSize: 12,
                    color: isUploaded ? Colors.green[600] : Colors.grey[600],
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}
