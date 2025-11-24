import 'package:flutter/material.dart';
import 'package:my_app/loginPage/verification_page.dart';
import 'package:my_app/widgets/auth/auth_card.dart';
import 'package:my_app/widgets/auth/auth_header.dart';
import 'package:my_app/widgets/auth/auth_text_field.dart';
import 'package:my_app/widgets/auth/auth_button.dart';

class ResetPasswordPage extends StatelessWidget {
  const ResetPasswordPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: Center(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const AuthHeader(
                title: 'Reset your password',
                subtitle: 'Enter your email address and we\'ll send you a link to reset your password',
              ),
              const SizedBox(height: 32),
              AuthCard(
                children: [
                  const AuthTextField(
                    label: 'Email Id',
                    hintText: 'Your Email',
                    keyboardType: TextInputType.emailAddress,
                  ),
                  const SizedBox(height: 24),
                  AuthButton(
                    text: 'Send reset link',
                    onPressed: () {
                      Navigator.of(context).push(
                        MaterialPageRoute(
                          builder: (context) => const VerificationPage(),
                        ),
                      );
                    },
                  ),
                  const SizedBox(height: 16),
                  TextButton(
                    onPressed: () {
                      Navigator.pop(context);
                    },
                    child: const Text(
                      'Back to login',
                      style: TextStyle(
                        fontSize: 16,
                        color: Colors.blue,
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}