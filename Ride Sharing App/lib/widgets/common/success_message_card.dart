import 'package:flutter/material.dart';

class SuccessMessageCard extends StatelessWidget {
  final String message;
  final Color backgroundColor;
  final Color textColor;

  const SuccessMessageCard({
    super.key,
    required this.message,
    this.backgroundColor = const Color(0xFFDCFCE7),
    this.textColor = Colors.black,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16.0),
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(8.0),
      ),
      child: Text(
        message,
        style: TextStyle(
          fontSize: 14,
          color: textColor,
        ),
      ),
    );
  }
}