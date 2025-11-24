import 'package:flutter/material.dart';
import 'package:my_app/widgets/auth/auth_button.dart';

class RegistrationButtonsRow extends StatelessWidget {
  final VoidCallback onRiderRegister;
  final VoidCallback onDriverRegister;

  const RegistrationButtonsRow({
    super.key,
    required this.onRiderRegister,
    required this.onDriverRegister,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: AuthButton(
            text: 'Register as Rider',
            onPressed: onRiderRegister,
            isPrimary: false,
          ),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: AuthButton(
            text: 'Register as Driver',
            onPressed: onDriverRegister,
            isPrimary: false,
          ),
        ),
      ],
    );
  }
}