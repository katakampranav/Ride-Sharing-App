import 'package:flutter/material.dart';

class ResendCodeLink extends StatelessWidget {
  final VoidCallback onResend;
  final String text;

  const ResendCodeLink({
    super.key,
    required this.onResend,
    this.text = 'Resend code?',
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onResend,
      child: Text(
        text,
        style: const TextStyle(
          fontSize: 14,
          color: Colors.blue,
          decoration: TextDecoration.underline,
        ),
        textAlign: TextAlign.center,
      ),
    );
  }
}