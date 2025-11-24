import 'package:flutter/material.dart';
import 'package:my_app/widgets/auth/auth_card.dart';
import 'package:my_app/widgets/auth/auth_button.dart';
import 'package:my_app/widgets/auth/auth_text_field.dart';

class AddLocationDialog extends StatefulWidget {
  final Function(String title, String address, String type) onSave;

  const AddLocationDialog({super.key, required this.onSave});

  @override
  State<AddLocationDialog> createState() => _AddLocationDialogState();
}

class _AddLocationDialogState extends State<AddLocationDialog> {
  final TextEditingController _titleController = TextEditingController();
  final TextEditingController _addressController = TextEditingController();
  String _selectedType = 'home';

  @override
  void dispose() {
    _titleController.dispose();
    _addressController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Dialog(
      insetPadding: const EdgeInsets.all(20),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: ConstrainedBox(
        constraints: BoxConstraints(
          maxHeight: MediaQuery.of(context).size.height * 0.8, // Limit maximum height
        ),
      child: SingleChildScrollView(
        child: AuthCard(
          padding: const EdgeInsets.all(24),
          children: [
            // Header
            const Text(
              "Add New Location",
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
                color: Colors.black87,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 20),
        
            // Location Type
            const Text(
              "Location Type",
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w600,
                color: Colors.black87,
              ),
            ),
            const SizedBox(height: 8),
            Card(
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
              child: Column(
                children: [
                  RadioListTile<String>(
                    value: "home",
                    groupValue: _selectedType,
                    onChanged: (value) {
                      setState(() => _selectedType = value!);
                    },
                    title: const Text("Home"),
                    secondary: const Icon(Icons.home, color: Colors.blue),
                  ),
                  const Divider(height: 1),
                  RadioListTile<String>(
                    value: "work",
                    groupValue: _selectedType,
                    onChanged: (value) {
                      setState(() => _selectedType = value!);
                    },
                    title: const Text("Work"),
                    secondary: const Icon(Icons.work, color: Colors.green),
                  ),
                  const Divider(height: 1),
                  RadioListTile<String>(
                    value: "other",
                    groupValue: _selectedType,
                    onChanged: (value) {
                      setState(() => _selectedType = value!);
                    },
                    title: const Text("Other"),
                    secondary: const Icon(
                      Icons.location_on,
                      color: Colors.orange,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),
        
            // Title Field
            AuthTextField(
              label: "Location Name",
              hintText: "e.g., Home, Office, Gym",
              controller: _titleController,
            ),
            const SizedBox(height: 16),
        
            // Address Field
            Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Text(
                  "Address",
                  style: TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                    color: Colors.black87,
                  ),
                ),
                const SizedBox(height: 8),
                TextField(
                  controller: _addressController,
                  maxLines: 3,
                  decoration: InputDecoration(
                    hintText: "Enter full address",
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: const BorderSide(color: Colors.grey),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: const BorderSide(color: Colors.blue, width: 2),
                    ),
                    contentPadding: const EdgeInsets.symmetric(
                      horizontal: 16,
                      vertical: 14,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 24),
        
            // Buttons
            Row(
              children: [
                // Cancel Button
                Expanded(
                  child: AuthButton(
                    text: "Cancel",
                    onPressed: () => Navigator.of(context).pop(),
                    isPrimary: false,
                  ),
                ),
                const SizedBox(width: 12),
        
                // Save Button
                Expanded(
                  child: AuthButton(
                    text: "Save Location",
                    onPressed: () {
                      if (_titleController.text.isEmpty ||
                          _addressController.text.isEmpty) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                            content: Text('Please fill all fields'),
                            backgroundColor: Colors.red,
                          ),
                        );
                        return;
                      }
                      widget.onSave(
                        _titleController.text,
                        _addressController.text,
                        _selectedType,
                      );
                    },
                    isPrimary: true,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),),
    );
  }
}
