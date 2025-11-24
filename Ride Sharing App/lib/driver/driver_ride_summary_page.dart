import 'package:flutter/material.dart';
import 'package:my_app/driver/dashboard.dart';
import 'package:my_app/driver/driver_ride_success_page.dart';
import 'package:my_app/driver/offer_ride_page.dart';
import 'package:my_app/profile/driver_profile/driver_profile_page.dart';
import 'package:my_app/wallet/driver_wallet_page.dart';
import 'package:my_app/widgets/auth/auth_button.dart';
import 'package:my_app/widgets/navigation/custom_bottom_nav_bar.dart';
import 'package:my_app/widgets/common/notification_icon.dart';
import 'package:my_app/widgets/rides/route_details_card.dart';

class DriverRideSummaryPage extends StatefulWidget {
  final String fromLocation;
  final String toLocation;
  final String date;
  final String time;
  final String seats;
  final String price;
  final String detourDistance;
  final bool isRecurring;

  const DriverRideSummaryPage({
    super.key,
    required this.fromLocation,
    required this.toLocation,
    required this.date,
    required this.time,
    required this.seats,
    required this.price,
    required this.detourDistance,
    required this.isRecurring,
  });

  @override
  State<DriverRideSummaryPage> createState() => _DriverRideSummaryPageState();
}

class _DriverRideSummaryPageState extends State<DriverRideSummaryPage> {
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
      Navigator.of(
        context,
      ).push(MaterialPageRoute(builder: (context) => const DriverProfilePage()));
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
            _buildMapPreview(),
            const SizedBox(height: 16),
            RouteDetailsCard(
              fromAddress: widget.fromLocation,
              toAddress: widget.toLocation,
              date: 'Friday, Oct 3',
              time: '23:05',
            ),

            const SizedBox(height: 16),

            // Details Table
            _buildDetailsTable(),
            const SizedBox(height: 24),

            // Divider
            const SizedBox(height: 24),

            // Confirm Offer Section
            _buildConfirmOfferSection(),
            const SizedBox(height: 16),
          ],
        ),
      ),
      bottomNavigationBar: CustomBottomNavBar(
        currentIndex: 1,
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

  Widget _buildDetailsTable() {
    return Row(
      children: [
        // Seats Box
        Expanded(
          child: Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: const BorderRadius.only(
                topLeft: Radius.circular(12),
                bottomLeft: Radius.circular(12),
              ),
              border: Border.all(color: Colors.grey.shade300),
            ),
            child: Column(
              children: [
                Text(
                  'Seats',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: Colors.black87,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  widget.seats,
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: Colors.black87,
                  ),
                ),
              ],
            ),
          ),
        ),

        // Price/Rider Box
        Expanded(
          child: Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Colors.white,
              border: Border.all(color: Colors.grey.shade300),
            ),
            child: Column(
              children: [
                Text(
                  'Price/Rider',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: Colors.black87,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  '\$ ${widget.price}',
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: Colors.black87,
                  ),
                ),
              ],
            ),
          ),
        ),

        // Max Detour Box
        Expanded(
          child: Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: const BorderRadius.only(
                topRight: Radius.circular(12),
                bottomRight: Radius.circular(12),
              ),
              border: Border.all(color: Colors.grey.shade300),
            ),
            child: Column(
              children: [
                Text(
                  'Max Detour',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: Colors.black87,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  '${widget.detourDistance} km',
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: Colors.black87,
                  ),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildConfirmOfferSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // In _buildConfirmOfferSection() method of DriverRideSummaryPage
        AuthButton(
          text: 'Confirm Offer',
          onPressed: () {
            Navigator.of(context).push(
              MaterialPageRoute(
                builder: (context) => DriverRideSuccessPage(
                  fromLocation: widget.fromLocation,
                  toLocation: widget.toLocation,
                  date: widget.date,
                  time: widget.time,
                  seats: widget.seats,
                  price: widget.price,
                ),
              ),
            );
          },
        ),
        const SizedBox(height: 12),
        OutlinedButton(
          onPressed: () {
            // Navigate back to edit details
            Navigator.of(context).pop();
          },
          style: OutlinedButton.styleFrom(
            side: const BorderSide(color: Color(0xFF2563EB)),
            foregroundColor: const Color(0xFF2563EB),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
            padding: const EdgeInsets.symmetric(vertical: 14),
          ),
          child: const Text(
            'Edit Details',
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
          ),
        ),
      ],
    );
  }
}
