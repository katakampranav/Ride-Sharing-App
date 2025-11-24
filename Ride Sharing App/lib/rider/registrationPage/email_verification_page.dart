import 'package:flutter/material.dart';
import 'package:my_app/rider/registrationPage/terms_conditions_page.dart';
import 'package:my_app/widgets/auth/auth_header.dart';
import 'package:my_app/widgets/auth/auth_button.dart';
import 'package:my_app/widgets/auth/verification_code_input.dart';
import 'package:my_app/widgets/auth/resend_code_link.dart';

class EmailVerificationPage extends StatefulWidget {
  const EmailVerificationPage({super.key});

  @override
  State<EmailVerificationPage> createState() => _EmailVerificationPageState();
}

class _EmailVerificationPageState extends State<EmailVerificationPage> {
  String _currentCode = '';

  void _onCodeChanged(String code) {
    setState(() {
      _currentCode = code;
    });
  }

  void _onCodeCompleted(String code) {
    // You could automatically submit when code is complete
    // or just store the completed code
    debugPrint('Code completed: $code');
  }

  void _onResendCode() {
    // Implement resend logic here
    debugPrint('Resend code requested');
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Verification code sent!')),
    );
  }

  void _onSubmit() {
    if (_currentCode.length == 5) {
      Navigator.of(context).push(
        MaterialPageRoute(
          builder: (context) => const TermsConditionsPage(),
        ),
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please enter the complete verification code')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const AuthHeader(
              title: 'Verification Code',
              subtitle: 'We’ve sent a 5-digit verification code to example@mail.com.\n Please enter it below to verify your company email.',
              subtitlePadding: EdgeInsets.zero,
            ),
            const SizedBox(height: 16),
            ResendCodeLink(
              onResend: _onResendCode,
            ),
            const SizedBox(height: 32),
            VerificationCodeInput(
              length: 5,
              onCodeChanged: _onCodeChanged,
              onCodeCompleted: _onCodeCompleted,
            ),
            const SizedBox(height: 32),
            AuthButton(
              text: 'Submit',
              onPressed: _onSubmit,
            ),
          ],
        ),
      ),
    );
  }
}