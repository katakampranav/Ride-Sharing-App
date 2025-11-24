import 'package:flutter/material.dart';
import 'package:my_app/widgets/maps/map_selection_button.dart';
import 'package:my_app/widgets/maps/map_preview.dart';

class LocationInputField extends StatelessWidget {
  final String label;
  final String hintText;
  final TextEditingController controller;
  final VoidCallback onMapSelect;
  final bool showMapPreview;
  final String? mapButtonText;

  const LocationInputField({
    super.key,
    required this.label,
    required this.hintText,
    required this.controller,
    required this.onMapSelect,
    this.showMapPreview = false,
    this.mapButtonText,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Text(
          label,
          style: const TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.w600,
          ),
        ),
        const SizedBox(height: 16),
        Row(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Expanded(
              flex: 2,
              child: TextField(
                controller: controller,
                decoration: InputDecoration(
                  hintText: hintText,
                  hintStyle: TextStyle(color: Colors.grey.shade500),
                  contentPadding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 12,
                  ),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(8),
                    borderSide: const BorderSide(
                      color: Color(0xFFE0E0E0),
                    ),
                  ),
                  enabledBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(8),
                    borderSide: const BorderSide(
                      color: Color(0xFFE0E0E0),
                    ),
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(8),
                    borderSide: const BorderSide(
                      color: Color(0xFF2563EB),
                      width: 2,
                    ),
                  ),
                ),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: SizedBox(
                height: 48, // Match text field height
                child: MapSelectionButton(
                  onPressed: onMapSelect,
                  text: mapButtonText ?? 'Select on Map',
                ),
              ),
            ),
          ],
        ),
        if (showMapPreview) ...[
          const SizedBox(height: 20),
          const MapPreview(),
        ],
      ],
    );
  }
}