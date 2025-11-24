import 'package:flutter/material.dart';
import 'package:my_app/widgets/auth/auth_button.dart';

class AddLocationButton extends StatelessWidget {
  final VoidCallback onPressed;

  const AddLocationButton({super.key, required this.onPressed});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        border: Border(top: BorderSide(color: Colors.grey.shade300)),
      ),
      child: SizedBox(
        width: double.infinity,
        child: AuthButton(
          text: "Add New Location",
          onPressed: onPressed,
          isPrimary: true,
        ),
      ),
    );
  }
}
