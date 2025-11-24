import 'package:flutter/material.dart';
import 'package:my_app/profile/rider_profile/rider_profile_page.dart';
import 'package:my_app/rider/book_ride_page.dart';
import 'package:my_app/rider/dashboard.dart';
import 'package:my_app/rider/rider_confirmation_page.dart';
import 'package:my_app/wallet/rider_wallet_page.dart';
import 'package:my_app/widgets/auth/auth_button.dart';
import 'package:my_app/widgets/navigation/custom_bottom_nav_bar.dart';
import 'package:my_app/widgets/common/notification_icon.dart';
import 'package:my_app/widgets/rides/safety_features_card.dart';
import 'package:my_app/widgets/rides/driver_info_card.dart';
import 'package:my_app/widgets/rides/route_details_card.dart';

class RideDetailsPage extends StatefulWidget {
  final String avatarUrl;
  final String name;
  final double rating;
  final String carModel;
  final String license;
  final String from;
  final String to;
  final String time;
  final String price;

  const RideDetailsPage({
    super.key,
    required this.avatarUrl,
    required this.name,
    required this.rating,
    required this.carModel,
    required this.license,
    required this.from,
    required this.to,
    required this.time,
    required this.price,
  });

  @override
  State<RideDetailsPage> createState() => _RideDetailsPageState();
}

class _RideDetailsPageState extends State<RideDetailsPage> {
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
            // Map preview/Advertisement
            _buildMapPreview(),
            const SizedBox(height: 16),

            // Track Ride Button
            AuthButton(text: 'Track your Ride', onPressed: () {}),
            const SizedBox(height: 16),

            // Driver Information
            DriverInfoCard(
              avatarUrl: widget.avatarUrl,
              name: widget.name,
              rating: widget.rating,
              carModel: widget.carModel,
              license: widget.license,
            ),
            const SizedBox(height: 16),

            // Route Details
            RouteDetailsCard(
              fromAddress: widget.from,
              toAddress: widget.to,
              date: 'Today',
              time: widget.time,
              price: widget.price,
            ),
            const SizedBox(height: 16),

            // Book Ride Button
            AuthButton(
              text: 'Book this Ride',
              onPressed: () {
                Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (_) => RideConfirmationPage(
                      avatarUrl: widget.avatarUrl,
                      driverName: widget.name,
                      driverRole: 'Developer',
                      carModel: widget.carModel,
                      licensePlate: widget.license,
                      from: widget.from,
                      to: widget.to,
                      pickupTime: widget.time,
                      eta: '5:55 PM',
                      price: widget.price,
                    ),
                  ),
                );
              },
            ),
            const SizedBox(height: 16),

            // Safety Features
            const SafetyFeaturesCard(),
            const SizedBox(height: 16),

            // Back Button
            OutlinedButton(
              onPressed: () => Navigator.of(context).pop(),
              style: OutlinedButton.styleFrom(
                side: const BorderSide(color: Color(0xFFE0E0E0)),
                foregroundColor: Colors.black87,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
                padding: const EdgeInsets.symmetric(vertical: 14),
              ),
              child: const Text(
                'Back to Results',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
              ),
            ),
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
