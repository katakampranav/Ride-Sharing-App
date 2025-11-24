import 'package:flutter/material.dart';
import 'package:my_app/driver/registrationPage/location_details_page.dart';
import 'package:my_app/widgets/auth/auth_button.dart';
import 'package:my_app/widgets/common/section_header.dart';

class TermsConditionsPage extends StatefulWidget {
  const TermsConditionsPage({super.key});

  @override
  State<TermsConditionsPage> createState() => _TermsConditionsPageState();
}

class _TermsConditionsPageState extends State<TermsConditionsPage> {
  bool _acceptedTerms = false;
  bool _acceptedPrivacy = false;
  bool _acceptedCommunityGuidelines = false;

  bool get _allAccepted => _acceptedTerms && _acceptedPrivacy && _acceptedCommunityGuidelines;

  void _onFinish() {
    if (_allAccepted) {
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(builder: (context) => const DriverLocationDetailsPage()),
        (route) => false,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        elevation: 0,
        backgroundColor: Colors.white,
        foregroundColor: Colors.black87,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: const Text(
          'Terms & Conditions',
          style: TextStyle(
            color: Colors.black87,
            fontWeight: FontWeight.w600,
          ),
        ),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 24.0, vertical: 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const SizedBox(height: 16),
            
            // Header
            const SectionHeader(
              title: 'Almost There!',
              subtitle: 'Please review and accept our terms to continue',
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 32),

            // Terms Card
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: Colors.grey.shade50,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: Colors.grey.shade200),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Terms of Service
                  _buildAgreementItem(
                    title: 'Terms of Service',
                    content: 'By using our ride-sharing service, you agree to follow our community guidelines, respect other users, and maintain a safe environment for everyone.',
                    value: _acceptedTerms,
                    onChanged: (value) {
                      setState(() {
                        _acceptedTerms = value ?? false;
                      });
                    },
                  ),
                  const SizedBox(height: 20),

                  // Privacy Policy
                  _buildAgreementItem(
                    title: 'Privacy Policy',
                    content: 'We collect and use your location, payment, and profile information to provide our services. Your data is protected and will not be shared without your consent.',
                    value: _acceptedPrivacy,
                    onChanged: (value) {
                      setState(() {
                        _acceptedPrivacy = value ?? false;
                      });
                    },
                  ),
                  const SizedBox(height: 20),

                  // Community Guidelines
                  _buildAgreementItem(
                    title: 'Community Guidelines',
                    content: 'Be respectful to drivers and fellow riders. No smoking, alcohol, or inappropriate behavior. Keep the vehicle clean and follow safety instructions.',
                    value: _acceptedCommunityGuidelines,
                    onChanged: (value) {
                      setState(() {
                        _acceptedCommunityGuidelines = value ?? false;
                      });
                    },
                  ),
                ],
              ),
            ),

            const SizedBox(height: 24),

            // Important Notes
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.blue.shade50,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: Colors.blue.shade100),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(Icons.info_outline, size: 20, color: Colors.blue.shade700),
                      const SizedBox(width: 8),
                      const Text(
                        'Important Information',
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w600,
                          color: Colors.blue,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Text(
                    '• You must be at least 18 years old to use our service\n'
                    '• Payment will be processed according to your selected method\n'
                    '• Cancellation fees may apply for last-minute cancellations\n'
                    '• Safety is our top priority - report any concerns immediately',
                    style: TextStyle(
                      fontSize: 12,
                      color: Colors.blue.shade700,
                      height: 1.4,
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(height: 32),

            // Finish Button
            AuthButton(
              text: 'Continue',
              onPressed: _allAccepted ? _onFinish : () {},
            ),

            const SizedBox(height: 16),

            // View Full Terms Link
            Center(
              child: TextButton(
                onPressed: () {
                  // You can implement viewing full terms in a webview or dialog
                  _showFullTermsDialog();
                },
                child: const Text(
                  'View Full Terms and Conditions',
                  style: TextStyle(
                    color: Color(0xFF2563EB),
                    fontSize: 14,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAgreementItem({
    required String title,
    required String content,
    required bool value,
    required ValueChanged<bool?> onChanged,
  }) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Checkbox(
          value: value,
          onChanged: onChanged,
          activeColor: const Color(0xFF2563EB),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(4),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                  color: Colors.black87,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                content,
                style: TextStyle(
                  fontSize: 14,
                  color: Colors.grey.shade700,
                  height: 1.4,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  void _showFullTermsDialog() {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return Dialog(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
          child: Container(
            padding: const EdgeInsets.all(20),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Full Terms and Conditions',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 16),
                SizedBox(
                  height: 300,
                  child: SingleChildScrollView(
                    child: Text(
                      _getFullTermsText(),
                      style: const TextStyle(fontSize: 14, height: 1.4),
                    ),
                  ),
                ),
                const SizedBox(height: 20),
                Center(
                  child: AuthButton(
                    text: 'Close',
                    onPressed: () => Navigator.of(context).pop(),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  String _getFullTermsText() {
    return '''
1. Acceptance of Terms
By accessing and using this ride-sharing service, you accept and agree to be bound by the terms and provision of this agreement.

2. User Responsibilities
- You must be at least 18 years old to use our services
- You are responsible for maintaining the confidentiality of your account
- You must provide accurate and complete information

3. Payment Terms
- All rides are subject to the fare calculation at the time of booking
- Payment will be charged to your selected payment method
- Cancellation fees may apply for rides canceled after driver assignment

4. Safety Guidelines
- Always wear your seatbelt
- No smoking or alcohol consumption in vehicles
- Respect the driver and other passengers
- Follow all traffic and safety laws

5. Privacy
We collect location data, payment information, and profile details to provide our services. Your data is protected under our privacy policy.

6. Cancellation Policy
- Free cancellation within 2 minutes of booking
- 50% fee for cancellations after driver assignment
- Full charge for no-shows after 5 minutes of arrival

7. Liability
We are not liable for any delays, accidents, or incidents beyond our reasonable control.

8. Changes to Terms
We reserve the right to modify these terms at any time. Continued use of the service constitutes acceptance of modified terms.
''';
  }
}