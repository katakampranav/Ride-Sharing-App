import 'package:flutter/material.dart';
import 'package:my_app/profile/rider_profile/rider_profile_page.dart';
import 'package:my_app/rider/book_ride_page.dart';
import 'package:my_app/rider/dashboard.dart';
import 'package:my_app/rider/driver_arrived_page.dart';
import 'package:my_app/wallet/rider_wallet_page.dart';
import 'package:my_app/widgets/auth/auth_button.dart';
import 'package:my_app/widgets/navigation/custom_bottom_nav_bar.dart';
import 'package:my_app/widgets/common/notification_icon.dart';
import 'package:my_app/widgets/rides/route_details_card.dart';
import 'package:my_app/widgets/rides/safety_features_card.dart';
import 'package:my_app/widgets/rides/driver_info_header.dart';
import 'package:my_app/widgets/rides/ride_details_section.dart';
import 'package:my_app/widgets/rides/fare_info_card.dart';
import 'package:my_app/widgets/common/status_message_card.dart';

class RideConfirmationPage extends StatefulWidget {
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

  const RideConfirmationPage({
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
  State<RideConfirmationPage> createState() => _RideConfirmationPageState();
}

class _RideConfirmationPageState extends State<RideConfirmationPage> {
  final List<BottomNavigationBarItem> _navItems = const [
    BottomNavigationBarItem(icon: Icon(Icons.home_outlined), label: 'Home'),
    BottomNavigationBarItem(
      icon: Icon(Icons.directions_car_outlined),
      label: 'Book Ride',
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
          builder: (context) => const RiderDashboard(userName: 'John Doe'),
        ),
        (route) => false,
      );
    } else if (index == 1) {
      Navigator.of(
        context,
      ).push(MaterialPageRoute(builder: (context) => const BookRidePage()));
    } else if (index == 2) {
      Navigator.of(
        context,
      ).push(MaterialPageRoute(builder: (context) => const WalletPage()));
    } else if (index == 3) {
      Navigator.of(
        context,
      ).push(MaterialPageRoute(builder: (context) => const ProfilePage()));
    }
  }

  @override
  void initState() {
    super.initState();
    // Auto-navigate to DriverArrivedPage after 5 seconds
    Future.delayed(const Duration(seconds: 5), () {
      if (mounted) {
        Navigator.of(context).pushReplacement(
          MaterialPageRoute(
            builder: (_) => DriverArrivedPage(
              driverName: widget.driverName,
              driverRole: widget.driverRole,
              carModel: widget.carModel,
              licensePlate: widget.licensePlate,
              from: widget.from,
              to: widget.to,
              pickupTime: widget.pickupTime,
              eta: widget.eta,
              price: widget.price,
              avatarUrl: widget.avatarUrl,
            ),
          ),
        );
      }
    });
  }

  void _showMessageOptions(BuildContext context) {
    final TextEditingController _messageController = TextEditingController();

    showDialog(
      context: context,
      builder: (BuildContext context) {
        return Dialog(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
          child: Container(
            padding: const EdgeInsets.all(20),
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // Header
                  const Row(
                    children: [
                      Icon(Icons.message, size: 24, color: Color(0xFF2563EB)),
                      SizedBox(width: 8),
                      Text(
                        'Quick Message',
                        style: TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                          color: Colors.black87,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),

                  const Text(
                    'Send a quick message to your driver:',
                    style: TextStyle(fontSize: 14, color: Colors.grey),
                  ),
                  const SizedBox(height: 20),

                  // Message Options
                  _buildMessageOption(
                    icon: Icons.schedule,
                    title: "I'm running late",
                    subtitle: "Let the driver know you'll be a few minutes",
                    onTap: () {
                      Navigator.of(context).pop();
                      _handleMessageSelection("I'm running late");
                    },
                  ),
                  const SizedBox(height: 12),

                  _buildMessageOption(
                    icon: Icons.location_on,
                    title: "I'm waiting at the location",
                    subtitle: "Inform driver you've arrived at pickup point",
                    onTap: () {
                      Navigator.of(context).pop();
                      _handleMessageSelection("I'm waiting at the location");
                    },
                  ),
                  const SizedBox(height: 12),

                  _buildMessageOption(
                    icon: Icons.pin_drop,
                    title: "I'm at a different spot",
                    subtitle: "Update your exact pickup location",
                    onTap: () {
                      Navigator.of(context).pop();
                      _handleMessageSelection("I'm at a different spot");
                    },
                  ),
                  const SizedBox(height: 12),

                  _buildMessageOption(
                    icon: Icons.phone,
                    title: "Please call me when you arrive",
                    subtitle: "Request a phone call upon arrival",
                    onTap: () {
                      Navigator.of(context).pop();
                      _handleMessageSelection("Please call me when you arrive");
                    },
                  ),

                  const SizedBox(height: 24),

                  // Divider for custom message
                  Divider(color: Colors.grey.shade300, thickness: 1),
                  const SizedBox(height: 12),

                  const Text(
                    'Or type your own message:',
                    style: TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
                  ),
                  const SizedBox(height: 10),

                  // Custom message text field
                  Container(
                    decoration: BoxDecoration(
                      color: Colors.grey.shade100,
                      borderRadius: BorderRadius.circular(10),
                      border: Border.all(color: Colors.grey.shade300),
                    ),
                    child: TextField(
                      controller: _messageController,
                      maxLines: 2,
                      style: const TextStyle(fontSize: 14),
                      decoration: const InputDecoration(
                        hintText: "Type your message here...",
                        border: InputBorder.none,
                        contentPadding: EdgeInsets.symmetric(
                          horizontal: 12,
                          vertical: 10,
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),

                  // Send button
                  ElevatedButton.icon(
                    onPressed: () {
                      final message = _messageController.text.trim();
                      if (message.isEmpty) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                            content: Text(
                              "Please type a message before sending.",
                            ),
                            backgroundColor: Colors.redAccent,
                          ),
                        );
                        return;
                      }
                      Navigator.of(context).pop();
                      _handleMessageSelection(message);
                    },
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFF2563EB),
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                    ),
                    icon: const Icon(Icons.send, size: 18),
                    label: const Text(
                      "Send Message",
                      style: TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),

                  const SizedBox(height: 12),

                  // Cancel button
                  SizedBox(
                    height: 48,
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
          ),
        );
      },
    );
  }

  Widget _buildMessageOption({
    required IconData icon,
    required String title,
    required String subtitle,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Colors.grey.shade50,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: Colors.grey.shade200),
        ),
        child: Row(
          children: [
            Icon(icon, size: 20, color: const Color(0xFF2563EB)),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: const TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.w600,
                      color: Colors.black87,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    subtitle,
                    style: TextStyle(fontSize: 12, color: Colors.grey.shade600),
                  ),
                ],
              ),
            ),
            const Icon(Icons.chevron_right, size: 20, color: Colors.grey),
          ],
        ),
      ),
    );
  }

  void _handleMessageSelection(String message) {
    // Show confirmation snackbar
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Message sent: "$message"'),
        backgroundColor: Colors.green,
        duration: const Duration(seconds: 2),
      ),
    );

    // Here you would typically send the message to your backend
    // or to the driver through your messaging system
    debugPrint('Message sent to driver: $message');
  }

  void _handleCallDriver() {
    // Show a confirmation dialog or directly initiate call
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Text('Call Driver'),
          content: Text('Call ${widget.driverName}?'),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('Cancel'),
            ),
            TextButton(
              onPressed: () {
                Navigator.of(context).pop();
                // Here you would implement actual calling functionality
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text('Calling ${widget.driverName}...'),
                    backgroundColor: Colors.blue,
                  ),
                );
              },
              child: const Text('Call'),
            ),
          ],
        );
      },
    );
  }

  void _showCancelConfirmation(BuildContext context) {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Text('Cancel Ride?'),
          content: const Text('Are you sure you want to cancel this ride?'),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('No'),
            ),
            TextButton(
              onPressed: () {
                Navigator.of(context).pop(); // Close dialog
                Navigator.of(context).pop(); // Go back to previous page
              },
              child: const Text(
                'Yes',
                style: TextStyle(color: Color(0xFFE11D48)),
              ),
            ),
          ],
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
          'Book a Ride',
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
                child: // In the main content card:
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Driver Info Header
                    // Driver Info Header
                    DriverInfoHeader(
                      avatarUrl: widget.avatarUrl,
                      name: widget.driverName,
                      role: widget.driverRole,
                      onMessage: () {
                        _showMessageOptions(
                          context,
                        ); // Updated to show message options
                      },
                      onCall: () {
                        // Handle call - you can implement call functionality here
                        _handleCallDriver();
                      },
                    ),
                    const SizedBox(height: 16),

                    // Status Message
                    const StatusMessageCard(
                      message:
                          'Driver on the way\nArriving in approximately 10 minutes',
                    ),
                    const SizedBox(height: 16),

                    // Route Information using existing RouteDetailsCard
                    RouteDetailsCard(
                      fromAddress: widget.from,
                      toAddress: widget.to,
                      showDateAndTime:
                          false, // Hide date/time since we show them below
                      showPrice:
                          false, // Hide price since we show it separately
                    ),
                    const SizedBox(height: 16),

                    // Ride Details (pickup time, ETA, car info)
                    RideDetailsSection(
                      pickupTime: widget.pickupTime,
                      eta: widget.eta,
                      carModel: widget.carModel,
                      licensePlate: widget.licensePlate,
                    ),
                    const SizedBox(height: 16),

                    // Fare Information
                    FareInfoCard(price: widget.price),
                  ],
                ),
              ),
            ),

            const SizedBox(height: 16),

            // Track Ride Button
            AuthButton(text: 'Track your Ride', onPressed: () {}),

            const SizedBox(height: 16),

            // Cancel Ride Button
            SizedBox(
              height: 48,
              child: OutlinedButton(
                onPressed: () {
                  _showCancelConfirmation(context);
                },
                style: OutlinedButton.styleFrom(
                  side: const BorderSide(color: Color(0xFFE11D48)),
                  foregroundColor: const Color(0xFFE11D48),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(10),
                  ),
                ),
                child: const Text(
                  'Cancel Ride',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
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
