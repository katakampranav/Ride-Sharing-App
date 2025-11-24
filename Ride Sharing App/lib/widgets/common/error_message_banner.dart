import 'package:flutter/material.dart';

class ErrorMessageBanner extends StatelessWidget {
  final String message;
  final Color backgroundColor;
  final Color textColor;

  const ErrorMessageBanner({
    super.key,
    required this.message,
    this.backgroundColor = Colors.red,
    this.textColor = Colors.white,
  });

  @override
  Widget build(BuildContext context) {
    if (message.isEmpty) return const SizedBox.shrink();
    
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        message,
        style: TextStyle(
          color: textColor,
          fontSize: 14,
        ),
      ),
    );
  }
}