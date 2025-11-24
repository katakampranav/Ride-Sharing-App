import 'package:flutter/material.dart';
import 'package:my_app/loginPage/reset_success_page.dart';
import 'package:my_app/widgets/auth/auth_header.dart';
import 'package:my_app/widgets/auth/auth_button.dart';
import 'package:my_app/widgets/auth/verification_code_input.dart';
import 'package:my_app/widgets/auth/resend_code_link.dart';

class VerificationPage extends StatefulWidget {
  const VerificationPage({super.key});

  @override
  State<VerificationPage> createState() => _VerificationPageState();
}

class _VerificationPageState extends State<VerificationPage> {
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
          builder: (context) => const PasswordResetSuccessPage(),
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
              subtitle: 'We\'ve sent 5 digit code to example@mail.com\nplease enter it below',
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