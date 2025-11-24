import 'package:flutter/material.dart';
import 'package:my_app/driver/dashboard.dart';
import 'package:my_app/driver/destination_reached_page.dart';
import 'package:my_app/driver/offer_ride_page.dart';
import 'package:my_app/profile/driver_profile/driver_profile_page.dart';
import 'package:my_app/wallet/driver_wallet_page.dart';
import 'package:my_app/widgets/auth/auth_button.dart';
import 'package:my_app/widgets/navigation/custom_bottom_nav_bar.dart';
import 'package:my_app/widgets/common/notification_icon.dart';
import 'package:my_app/widgets/rides/safety_features_card.dart';
import 'package:my_app/widgets/rides/driver_info_header.dart';
import 'package:my_app/widgets/rides/route_details_card.dart';
import 'package:my_app/widgets/common/status_message_card.dart';

class FollowUpPage extends StatefulWidget {
  final String driverName;
  final String driverRole;
  final String carModel;
  final String licensePlate;
  final String from;
  final String to;
  final String pickupTime;
  final String eta;
  final String price;
  final String avatarUrl;

  const FollowUpPage({
    super.key,
    required this.driverName,
    required this.driverRole,
    required this.carModel,
    required this.licensePlate,
    required this.from,
    required this.to,
    required this.pickupTime,
    required this.eta,
    required this.price,
    required this.avatarUrl,
  });

  @override
  State<FollowUpPage> createState() => _FollowUpPageState();
}

class _FollowUpPageState extends State<FollowUpPage> {
  final List<BottomNavigationBarItem> _navItems = const [
    BottomNavigationBarItem(icon: Icon(Icons.home_outlined), label: 'Home'),
    BottomNavigationBarItem(
      icon: Icon(Icons.directions_car_outlined),
      label: 'Offer Ride',
    ),
    BottomNavigationBarItem(
      icon: Icon(Icons.account_balance_wallet_outlined),
      label: 'Wallet',
    ),
    BottomNavigationBarItem(
      icon: Icon(Icons.person_outlined),
      label: 'Profile',
    ),
  ];

  void _onNavItemTapped(int index) {
    if (index == 0) {
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(
          builder: (context) => const DriverDashboard(userName: 'John Doe'),
        ),
        (route) => false,
      );
    } else if (index == 1) {
      Navigator.of(context).push(
        MaterialPageRoute(builder: (context) => const DriverOfferRidePage()),
      );
    } else if (index == 2) {
      Navigator.of(
        context,
      ).push(MaterialPageRoute(builder: (context) => const DriverWalletPage()));
    } else if (index == 3) {
      Navigator.of(context).push(
        MaterialPageRoute(builder: (context) => const DriverProfilePage()),
      );
    }
  }

  void _showReportOptions(BuildContext context) {
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
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // Driver Info Header
                DriverInfoHeader(
                  avatarUrl: widget.avatarUrl,
                  name: widget.driverName,
                  role: widget.driverRole,
                  onMessage: () {
                    // Handle message
                  },
                  onCall: () {
                    // Handle call
                  },
                ),
                const SizedBox(height: 16),

                // Warning Icon
                const Icon(
                  Icons.warning_amber_rounded,
                  size: 60,
                  color: Color(0xFFE11D48),
                ),
                const SizedBox(height: 20),

                // Report Options
                _buildReportOption(
                  title: "I'm fine, keep going",
                  onTap: () {
                    Navigator.of(context).pop();
                    _handleReportSelection("I'm fine, keep going");
                  },
                ),
                const SizedBox(height: 12),

                _buildReportOption(
                  title: "Need fuel assistance",
                  onTap: () {
                    Navigator.of(context).pop();
                    _handleReportSelection("Need fuel assistance");
                  },
                ),
                const SizedBox(height: 12),

                _buildReportOption(
                  title: "Traffic, police stop, or other reason",
                  onTap: () {
                    Navigator.of(context).pop();
                    _handleReportSelection(
                      "Traffic, police stop, or other reason",
                    );
                  },
                ),
                const SizedBox(height: 20),

                // Call Emergency Button
                SizedBox(
                  height: 50,
                  child: ElevatedButton(
                    onPressed: () {
                      Navigator.of(context).pop();
                      _handleEmergencyCall();
                    },
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.white,
                      foregroundColor: const Color(0xFFE11D48),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                        side: const BorderSide(color: Color(0xFFE11D48)),
                      ),
                    ),
                    child: const Text(
                      'Call Emergency',
                      style: TextStyle(fontWeight: FontWeight.w800),
                    ),
                  ),
                ),
                const SizedBox(height: 12),

                // Cancel Button
                SizedBox(
                  height: 50,
                  child: OutlinedButton(
                    onPressed: () {
                      Navigator.of(context).pop();
                    },
                    style: OutlinedButton.styleFrom(
                      side: const BorderSide(color: Colors.grey),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                    child: const Text(
                      'Cancel',
                      style: TextStyle(
                        color: Colors.black87,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildReportOption({
    required String title,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Colors.grey[50],
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: Colors.grey[300]!),
        ),
        child: Row(
          children: [
            Expanded(
              child: Text(
                title,
                style: const TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
            const Icon(Icons.chevron_right, color: Colors.grey),
          ],
        ),
      ),
    );
  }

  void _handleReportSelection(String option) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text('Reported: $option')));
  }

  void _handleEmergencyCall() {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(const SnackBar(content: Text('Emergency call initiated')));
  }

  @override
  void initState() {
    super.initState();
    // Auto-navigate to DestinationReachedPage after 5 seconds
    Future.delayed(const Duration(seconds: 5), () {
      if (mounted) {
        Navigator.of(context).pushReplacement(
          MaterialPageRoute(builder: (_) => DestinationReachedPage()),
        );
      }
    });
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
          'Offering the Ride',
          style: TextStyle(color: Colors.black87, fontWeight: FontWeight.w600),
        ),
        actions: [_buildProfileSection()],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Map preview
            _buildMapPreview(),
            const SizedBox(height: 16),

            // Main Content Card
            Container(
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(12),
                boxShadow: [
                  BoxShadow(
                    color: Colors.grey.withOpacity(0.08),
                    blurRadius: 10,
                    offset: const Offset(0, 4),
                  ),
                ],
              ),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    // Driver Info Header
                    DriverInfoHeader(
                      avatarUrl: widget.avatarUrl,
                      name: widget.driverName,
                      role: widget.driverRole,
                      onMessage: () {
                        // Handle message
                      },
                      onCall: () {
                        // Handle call
                      },
                    ),
                    const SizedBox(height: 16),

                    // Status Message
                    StatusMessageCard(
                      message: 'Travelling to the destination...',
                      icon: Icons.drive_eta,
                      iconColor: Colors.blue,
                      backgroundColor: Colors.white,
                      borderColor: Colors.grey,
                      textColor: Colors.black87,
                    ),
                    const SizedBox(height: 16),

                    // Route Information
                    RouteDetailsCard(
                      fromAddress: widget.from,
                      toAddress: widget.to,
                      showDateAndTime: false,
                      showPrice: false,
                    ),
                    const SizedBox(height: 32),

                    // Track Ride Button
                    AuthButton(text: 'Track your Ride', onPressed: () {}),
                    const SizedBox(height: 16),

                    // Report Ride Button
                    SizedBox(
                      height: 48,
                      child: OutlinedButton(
                        onPressed: () {
                          _showReportOptions(context);
                        },
                        style: OutlinedButton.styleFrom(
                          side: const BorderSide(color: Colors.black87),
                          foregroundColor: Colors.black87,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(10),
                          ),
                        ),
                        child: const Text(
                          'Report Ride',
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),

            const SizedBox(height: 16),

            // Safety Features
            const SafetyFeaturesCard(),
            const SizedBox(height: 16),
          ],
        ),
      ),
      bottomNavigationBar: CustomBottomNavBar(
        currentIndex: 1, // Book Ride is active
        onTap: _onNavItemTapped,
        items: _navItems,
      ),
    );
  }

  Widget _buildProfileSection() {
    return Row(
      children: [
        NotificationIcon(
          onPressed: () {
            // Handle notification tap
          },
        ),
        const SizedBox(width: 8),
        CircleAvatar(
          radius: 20,
          backgroundColor: Colors.grey.shade300,
          child: const Icon(Icons.person, color: Colors.grey),
        ),
        const SizedBox(width: 16),
      ],
    );
  }

  Widget _buildMapPreview() {
    return ClipRRect(
      borderRadius: BorderRadius.circular(12),
      child: SizedBox(
        height: 180,
        child: Container(
          color: Colors.grey.shade200,
          child: const Center(
            child: Text(
              'ADVERTISEMENT',
              style: TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
                color: Colors.grey,
              ),
            ),
          ),
        ),
      ),
    );
  }
}
