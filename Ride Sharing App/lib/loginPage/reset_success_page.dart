import 'package:flutter/material.dart';
import 'package:my_app/widgets/auth/auth_header.dart';
import 'package:my_app/widgets/auth/auth_card.dart';
import 'package:my_app/widgets/auth/auth_button.dart';
import 'package:my_app/widgets/common/success_message_card.dart';

class PasswordResetSuccessPage extends StatelessWidget {
  const PasswordResetSuccessPage({super.key});

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
                  SuccessMessageCard(
                    message: 'Reset link sent!\n\nWe\'ve sent a password reset link to\nabc@gmail.com. Please check your inbox\nand follow the instructions.',
                  ),
                  const SizedBox(height: 24),
                  AuthButton(
                    text: 'Return to Login',
                    onPressed: () {
                      Navigator.popUntil(context, (route) => route.isFirst);
                    },
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