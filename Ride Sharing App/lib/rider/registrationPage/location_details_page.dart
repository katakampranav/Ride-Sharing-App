import 'package:flutter/material.dart';
import 'package:my_app/loginPage/login_page.dart';
import 'package:my_app/widgets/auth/auth_card.dart';
import 'package:my_app/widgets/common/section_header.dart';
import 'package:my_app/widgets/auth/auth_button.dart';
import 'package:my_app/widgets/auth/divider_with_text.dart';
import 'package:my_app/widgets/forms/date_time_input.dart';
import 'package:my_app/widgets/maps/location_input_field.dart';
import 'package:my_app/widgets/common/preference_checkbox.dart';

class LocationDetailsPage extends StatefulWidget {
  const LocationDetailsPage({super.key});

  @override
  State<LocationDetailsPage> createState() => _LocationDetailsPageState();
}

class _LocationDetailsPageState extends State<LocationDetailsPage> {
  final _homeAddressController = TextEditingController();
  final _workAddressController = TextEditingController();
  final TextEditingController _timeController = TextEditingController();
  bool _preferFemaleDrivers = false;

  void _onRegister() {
    debugPrint("Home Address: ${_homeAddressController.text}");
    debugPrint("Work Address: ${_workAddressController.text}");
    debugPrint("Prefer Female Drivers: $_preferFemaleDrivers");

    Navigator.of(context).push(
      MaterialPageRoute(builder: (context) => const LoginPage()),
    );
  }

  void _selectHomeOnMap() {
    debugPrint("Select home address on map");
  }

  void _selectWorkOnMap() {
    debugPrint("Select work address on map");
  }

  Future<void> _selectTime(BuildContext context) async {
    final TimeOfDay? picked = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.now(),
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            colorScheme: const ColorScheme.light(
              primary: Color(0xFF2563EB),
              onPrimary: Colors.white,
              onSurface: Colors.black87,
            ),
            textButtonTheme: TextButtonThemeData(
              style: TextButton.styleFrom(
                foregroundColor: const Color(0xFF2563EB),
              ),
            ),
          ),
          child: child!,
        );
      },
    );

    if (picked != null) {
      setState(() {
        _timeController.text = picked.format(context);
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        elevation: 0,
        backgroundColor: Colors.white,
        iconTheme: const IconThemeData(color: Colors.black),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Colors.black),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const SizedBox(height: 16),
            const SectionHeader(
              title: 'Home & Office Setup',
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 32),

            AuthCard(
              children: [
                LocationInputField(
                  label: 'Home Location',
                  hintText: 'Select your home address',
                  controller: _homeAddressController,
                  onMapSelect: _selectHomeOnMap,
                ),
                const SizedBox(height: 24),
                LocationInputField(
                  label: 'Work Location',
                  hintText: 'Select your work address',
                  controller: _workAddressController,
                  onMapSelect: _selectWorkOnMap,
                ),
                const SizedBox(height: 24),
                DateTimeInput(
                  label: 'Preferred Travel Time',
                  controller: _timeController,
                  hintText: 'Select Time',
                  suffixIcon: const Icon(
                    Icons.access_time,
                    size: 18,
                    color: Colors.black45,
                  ),
                  onTap: () => _selectTime(context),
                ),
                const SizedBox(height: 24),
                PreferenceCheckbox(
                  label: 'Prefer female drivers only',
                  value: _preferFemaleDrivers,
                  onChanged: (value) {
                    setState(() {
                      _preferFemaleDrivers = value ?? false;
                    });
                  },
                ),
                const SizedBox(height: 32),
                AuthButton(text: 'Register', onPressed: _onRegister),
              ],
            ),

            const SizedBox(height: 32),
            const DividerWithText(text: 'Already have an account?'),
            const SizedBox(height: 16),

            AuthButton(
              text: 'Sign In',
              onPressed: () {
                Navigator.popUntil(context, (route) => route.isFirst);
              },
            ),
            const SizedBox(height: 32),
          ],
        ),
      ),
    );
  }
}
