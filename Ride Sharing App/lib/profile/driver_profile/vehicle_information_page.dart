import 'package:flutter/material.dart';
import 'package:my_app/widgets/auth/auth_button.dart';
import 'package:my_app/widgets/auth/auth_card.dart';
import 'package:my_app/widgets/auth/auth_text_field.dart';

class VehicleInformationPage extends StatefulWidget {
  const VehicleInformationPage({super.key});

  @override
  State<VehicleInformationPage> createState() => _VehicleInformationPageState();
}

class _VehicleInformationPageState extends State<VehicleInformationPage> {
  String _make = 'Not provided';
  String _model = 'Not provided';
  String _year = 'Not provided';
  String _licensePlate = 'Not provided';

  void _showUpdateVehicleDialog() {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return UpdateVehicleDialog(
          onSave: (make, model, year, licensePlate) {
            setState(() {
              _make = make;
              _model = model;
              _year = year;
              _licensePlate = licensePlate;
            });
            Navigator.of(context).pop();
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(
                content: Text('Vehicle information updated successfully!'),
                backgroundColor: Colors.green,
              ),
            );
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF8F9FA),
      appBar: AppBar(
        elevation: 0,
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: const Text(
          'Vehicle Information',
          style: TextStyle(color: Colors.black87, fontWeight: FontWeight.w600),
        ),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            AuthCard(
              children: [
                const Text(
                  'Your Vehicle Details',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: Colors.black87,
                  ),
                ),
                const SizedBox(height: 20),

                // Vehicle Details Table
                _buildDetailRow('Make', _make),
                const SizedBox(height: 12),
                _buildDetailRow('Model', _model),
                const SizedBox(height: 12),
                _buildDetailRow('Year', _year),
                const SizedBox(height: 12),
                _buildDetailRow('License Plate', _licensePlate),
                const SizedBox(height: 24),

                const Divider(height: 1, color: Colors.grey),
                const SizedBox(height: 24),

                // Update Vehicle Information Button
                AuthButton(
                  text: 'Update Vehicle Information',
                  onPressed: _showUpdateVehicleDialog,
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDetailRow(String label, String value) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SizedBox(
          width: 120,
          child: Text(
            label,
            style: const TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.w600,
              color: Colors.black87,
            ),
          ),
        ),
        Expanded(
          child: Text(
            value,
            style: TextStyle(
              fontSize: 14,
              color: value == 'Not provided' ? Colors.grey : Colors.black87,
            ),
          ),
        ),
      ],
    );
  }
}

class UpdateVehicleDialog extends StatefulWidget {
  final Function(String, String, String, String) onSave;

  const UpdateVehicleDialog({super.key, required this.onSave});

  @override
  State<UpdateVehicleDialog> createState() => _UpdateVehicleDialogState();
}

class _UpdateVehicleDialogState extends State<UpdateVehicleDialog> {
  final TextEditingController _makeController = TextEditingController();
  final TextEditingController _modelController = TextEditingController();
  final TextEditingController _yearController = TextEditingController();
  final TextEditingController _licenseController = TextEditingController();

  @override
  void dispose() {
    _makeController.dispose();
    _modelController.dispose();
    _yearController.dispose();
    _licenseController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Dialog(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Edit Vehicle information',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
                color: Colors.black87,
              ),
            ),
            const SizedBox(height: 20),

            // Make Field
            AuthTextField(
              label: 'Vehicle',
              hintText: 'Enter vehicle make',
              controller: _makeController,
            ),
            const SizedBox(height: 16),

            // Model Field
            AuthTextField(
              label: 'Model',
              hintText: 'Enter vehicle model',
              controller: _modelController,
            ),
            const SizedBox(height: 16),

            // License Number Field
            AuthTextField(
              label: 'License Number',
              hintText: 'Enter license number',
              controller: _licenseController,
            ),
            const SizedBox(height: 16),

            // Year Field
            AuthTextField(
              label: 'Year',
              hintText: 'Enter year',
              controller: _yearController,
              keyboardType: TextInputType.number,
            ),
            const SizedBox(height: 24),

            const Divider(height: 1, color: Colors.grey),
            const SizedBox(height: 16),

            // Action Buttons
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: () => Navigator.of(context).pop(),
                    style: OutlinedButton.styleFrom(
                      side: const BorderSide(color: Color(0xFF2563EB)),
                      foregroundColor: const Color(0xFF2563EB),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                      padding: const EdgeInsets.symmetric(vertical: 14),
                    ),
                    child: const Text(
                      'Cancel',
                      style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: AuthButton(
                    text: 'Save Changes',
                    onPressed: () {
                      widget.onSave(
                        _makeController.text,
                        _modelController.text,
                        _yearController.text,
                        _licenseController.text,
                      );
                    },
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}