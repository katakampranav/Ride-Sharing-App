import 'package:flutter/material.dart';
import 'package:my_app/driver/follow_up_page.dart';
import 'package:my_app/driver/offer_ride_page.dart';
import 'package:my_app/profile/driver_profile/driver_profile_page.dart';
import 'package:my_app/driver/dashboard.dart';
import 'package:my_app/wallet/driver_wallet_page.dart';
import 'package:my_app/widgets/auth/auth_button.dart';
import 'package:my_app/widgets/navigation/custom_bottom_nav_bar.dart';
import 'package:my_app/widgets/common/notification_icon.dart';
import 'package:my_app/widgets/rides/route_details_card.dart';
import 'package:my_app/widgets/rides/safety_features_card.dart';
import 'package:my_app/widgets/rides/driver_info_header.dart';
import 'package:my_app/widgets/rides/ride_details_section.dart';
import 'package:my_app/widgets/rides/fare_info_card.dart';
import 'package:my_app/widgets/common/status_message_card.dart';

class DriverArrivedPage extends StatefulWidget {
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

  const DriverArrivedPage({
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
  State<DriverArrivedPage> createState() => _DriverArrivedPageState();
}

class _DriverArrivedPageState extends State<DriverArrivedPage> {
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
      Navigator.of(
        context,
      ).push(MaterialPageRoute(builder: (context) => const DriverOfferRidePage()));
    } else if (index == 2) {
      Navigator.of(
        context,
      ).push(MaterialPageRoute(builder: (context) => const DriverWalletPage()));
    } else if (index == 3) {
      Navigator.of(
        context,
      ).push(MaterialPageRoute(builder: (context) => const DriverProfilePage()));
    }
  }

  @override
  void initState() {
    super.initState();
    // Auto-navigate to FollowUpPage after 5 seconds
    Future.delayed(const Duration(seconds: 5), () {
      if (mounted) {
        Navigator.of(context).pushReplacement(
          MaterialPageRoute(
            builder: (_) => FollowUpPage(
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
          'Offer a Ride',
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
                  crossAxisAlignment: CrossAxisAlignment.start,
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

                    // Status Message - Driver Arrived
                    StatusMessageCard(
                      message:
                          'Driver has arrived\nYour driver is waiting at the pickup location',
                      icon: Icons.location_on,
                      iconColor: const Color(0xFF16A34A),
                      backgroundColor: const Color(0xFFF0FDF4),
                      borderColor: const Color(0xFFBBF7D0),
                      textColor: const Color(0xFF166534),
                    ),
                    const SizedBox(height: 16),

                    // Route Information using existing RouteDetailsCard
                    RouteDetailsCard(
                      fromAddress: widget.from,
                      toAddress: widget.to,
                      showDateAndTime: false,
                      showPrice: false,
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
