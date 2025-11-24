import 'package:flutter/material.dart';

class MapSelectionButton extends StatelessWidget {
  final VoidCallback onPressed;
  final String text;

  const MapSelectionButton({
    super.key,
    required this.onPressed,
    this.text = 'Select on Map',
  });

  @override
  Widget build(BuildContext context) {
    return ElevatedButton(
      onPressed: onPressed,
      style: ElevatedButton.styleFrom(
        backgroundColor: Colors.grey.shade100,
        foregroundColor: Colors.black87,
        padding: const EdgeInsets.symmetric(vertical: 12),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(8),
        ),
      ),
      child: Text(text),
    );
  }
}