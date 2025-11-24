import 'dart:io';
import 'package:flutter/material.dart';
import 'package:my_app/driver/registrationPage/email_verification_page.dart';
import 'package:my_app/widgets/auth/auth_card.dart';
import 'package:my_app/widgets/auth/auth_text_field.dart';
import 'package:my_app/widgets/auth/auth_button.dart';
import 'package:my_app/widgets/common/error_message_banner.dart';
import 'package:my_app/widgets/auth/divider_with_text.dart';
import 'package:my_app/widgets/common/section_header.dart';
import 'package:my_app/widgets/forms/photo_upload.dart';

class DriverRegistrationPage extends StatefulWidget {
  const DriverRegistrationPage({super.key});

  @override
  State<DriverRegistrationPage> createState() => _DriverRegistrationPageState();
}

class _DriverRegistrationPageState extends State<DriverRegistrationPage> {
  final TextEditingController _fullNameController = TextEditingController();
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _phoneNumberController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  final TextEditingController _confirmPasswordController =
      TextEditingController();

  String? _errorMessage;
  File? _profilePhoto; // Add this to store the selected photo

  @override
  void dispose() {
    _fullNameController.dispose();
    _emailController.dispose();
    _phoneNumberController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  void _navigateToLocationDetails() {
    setState(() {
      _errorMessage = null;

      if (_fullNameController.text.trim().isEmpty) {
        _errorMessage = 'Please enter your full name';
      } else if (_emailController.text.trim().isEmpty) {
        _errorMessage = 'Please enter your corporate email';
      } else if (_phoneNumberController.text.trim().isEmpty) {
        _errorMessage = 'Please enter your phone number';
      } else if (_phoneNumberController.text.trim().length < 10) {
        _errorMessage = 'Please enter a valid phone number';
      } else if (_passwordController.text.isEmpty) {
        _errorMessage = 'Please enter your password';
      } else if (_passwordController.text.length < 6) {
        _errorMessage = 'Password must be at least 6 characters';
      } else if (_confirmPasswordController.text.isEmpty) {
        _errorMessage = 'Please confirm your password';
      } else if (_passwordController.text != _confirmPasswordController.text) {
        _errorMessage = 'Passwords do not match';
      } else if (_profilePhoto == null) {
        _errorMessage = 'Please upload your profile photo';
      } else {
        Navigator.of(context).push(
          MaterialPageRoute(
            builder: (context) => EmailVerificationPage(),
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
              mainAxisSize: MainAxisSize.min,
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
                _buildStep(2, 'Location Details', false),
                const SizedBox(height: 8),
                _buildStep(3, 'License Verification', false),
              ],
            ),
            const SizedBox(height: 32),

            // Form Section
            const SectionHeader(
              title: 'Personal Info',
              textAlign: TextAlign.center,
              crossAxisAlignment: CrossAxisAlignment.stretch,
            ),
            const SizedBox(height: 16),

            AuthCard(
              children: [
                if (_errorMessage != null) ...[
                  ErrorMessageBanner(message: _errorMessage!),
                  const SizedBox(height: 16),
                ],

                AuthTextField(
                  label: 'Full Name',
                  hintText: 'Enter your full name',
                  controller: _fullNameController,
                ),
                const SizedBox(height: 16),

                AuthTextField(
                  label: 'Email',
                  hintText: 'Enter your corporate email',
                  controller: _emailController,
                ),
                const SizedBox(height: 16),

                AuthTextField(
                  label: 'Phone Number',
                  hintText: 'Enter your phone number',
                  controller: _phoneNumberController,
                  keyboardType: TextInputType.phone,
                ),
                const SizedBox(height: 16),

                AuthTextField(
                  label: 'Password',
                  hintText: 'Enter your password',
                  controller: _passwordController,
                  obscureText: true,
                ),
                const SizedBox(height: 16),

                AuthTextField(
                  label: 'Confirm Password',
                  hintText: 'Confirm your password',
                  controller: _confirmPasswordController,
                  obscureText: true,
                ),
                const SizedBox(height: 16),

                // Photo Upload Widget
                PhotoUploadWidget(
                  label: 'Upload Curret Photo',
                  onPhotoChanged: (File? photo) {
                    setState(() {
                      _profilePhoto = photo;
                    });
                  },
                  containerHeight: 140,
                  containerWidth: double.infinity,
                ),
                const SizedBox(height: 24),

                AuthButton(
                  text: 'Verify Email',
                  onPressed: _navigateToLocationDetails,
                ),
              ],
            ),

            const SizedBox(height: 32),
            const DividerWithText(text: 'Already have an account?'),
            const SizedBox(height: 16),

            AuthButton(
              text: 'Sign In',
              onPressed: () {
                Navigator.of(context).pop();
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
}