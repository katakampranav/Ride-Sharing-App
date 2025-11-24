import 'dart:io';
import 'package:flutter/material.dart';
import 'package:my_app/rider/registrationPage/email_verification_page.dart';
import 'package:my_app/widgets/auth/auth_card.dart';
import 'package:my_app/widgets/auth/auth_text_field.dart';
import 'package:my_app/widgets/auth/auth_button.dart';
import 'package:my_app/widgets/common/error_message_banner.dart';
import 'package:my_app/widgets/auth/divider_with_text.dart';
import 'package:my_app/widgets/forms/photo_upload.dart'; 

class RiderRegistrationPage extends StatefulWidget {
  const RiderRegistrationPage({super.key});

  @override
  State<RiderRegistrationPage> createState() => _RiderRegistrationPageState();
}

class _RiderRegistrationPageState extends State<RiderRegistrationPage> {
  final _fullNameController = TextEditingController();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();

  String? _errorMessage;
  File? _profilePhoto; // Add this to store the selected photo

  void _onContinue() {
    setState(() {
      _errorMessage = null;

      if (_fullNameController.text.trim().isEmpty) {
        _errorMessage = "Full name cannot be empty";
      } else if (_emailController.text.trim().isEmpty) {
        _errorMessage = "Email cannot be empty";
      } else if (!_isValidEmail(_emailController.text.trim())) {
        _errorMessage = "Please enter a valid email address";
      } else if (_passwordController.text.isEmpty) {
        _errorMessage = "Password cannot be empty";
      } else if (_passwordController.text.length < 6) {
        _errorMessage = "Password must be at least 6 characters long";
      } else if (_confirmPasswordController.text.isEmpty) {
        _errorMessage = "Please confirm your password";
      } else if (_passwordController.text != _confirmPasswordController.text) {
        _errorMessage = "Passwords do not match";
      } else if (_profilePhoto == null) {
        _errorMessage = "Please upload your profile photo"; // Add this validation
      } else {
        debugPrint("Full Name: ${_fullNameController.text}");
        debugPrint("Email: ${_emailController.text}");
        debugPrint("Password: ${_passwordController.text}");
        debugPrint("Profile Photo: ${_profilePhoto?.path}"); // Log photo path

        Navigator.of(context).push(
          MaterialPageRoute(builder: (context) => const EmailVerificationPage()),
        );
      }
    });
  }

  bool _isValidEmail(String email) {
    return RegExp(r'^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$').hasMatch(email);
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
                  'Register as Rider',
                  style: const TextStyle(
                    fontSize: 28,
                    fontWeight: FontWeight.bold,
                  ),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 8),
                const Text(
                  'Create your account to start booking rides',
                  style: TextStyle(
                    fontSize: 16,
                    color: Colors.black54,
                  ),
                  textAlign: TextAlign.center,
                ),
              ],
            ),
            const SizedBox(height: 32),
            
            const Text(
              'Personal Info',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
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
                  hintText: 'Full Name',
                  controller: _fullNameController,
                ),
                const SizedBox(height: 16),
                
                AuthTextField(
                  label: 'Email Id',
                  hintText: 'Enter your corporate email Id',
                  controller: _emailController,
                  keyboardType: TextInputType.emailAddress,
                ),
                const SizedBox(height: 16),
                
                AuthTextField(
                  label: 'Password',
                  hintText: 'Password',
                  controller: _passwordController,
                  obscureText: true,
                ),
                const SizedBox(height: 16),
                
                AuthTextField(
                  label: 'Confirm Password',
                  hintText: 'Confirm Password',
                  controller: _confirmPasswordController,
                  obscureText: true,
                ),
                const SizedBox(height: 16),

                // Add the Photo Upload Widget here
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
                  onPressed: _onContinue,
                ),
              ],
            ),

            const SizedBox(height: 32),
            const DividerWithText(text: 'Already have an account?'),
            const SizedBox(height: 16),

            AuthButton(
              text: 'Sign In',
              onPressed: () {
                Navigator.pop(context);
              },
              isPrimary: false, // Make it a secondary button
            ),
          ],
        ),
      ),
    );
  }
}