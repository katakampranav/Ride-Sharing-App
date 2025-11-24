import 'package:flutter/material.dart';
import 'package:my_app/driver/dashboard.dart';
import 'package:my_app/driver/offer_ride_page.dart';
import 'package:my_app/driver/riders_list.dart';
import 'package:my_app/profile/driver_profile/driver_profile_page.dart';
import 'package:my_app/wallet/driver_wallet_page.dart';
import 'package:my_app/widgets/auth/auth_button.dart';
import 'package:my_app/widgets/auth/auth_card.dart';
import 'package:my_app/widgets/navigation/custom_bottom_nav_bar.dart';
import 'package:my_app/widgets/common/notification_icon.dart';

class DriverRideSuccessPage extends StatefulWidget {
  final String fromLocation;
  final String toLocation;
  final String date;
  final String time;
  final String seats;
  final String price;

  const DriverRideSuccessPage({
    super.key,
    required this.fromLocation,
    required this.toLocation,
    required this.date,
    required this.time,
    required this.seats,
    required this.price,
  });

  @override
  State<DriverRideSuccessPage> createState() => _DriverRideSuccessPageState();
}

class _DriverRideSuccessPageState extends State<DriverRideSuccessPage> {
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
      // Already on success page, do nothing
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(builder: (context) => const DriverOfferRidePage()),
        (route) => false,
      );
    } else if (index == 2) {
      // Handle wallet navigation
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(builder: (context) => const DriverWalletPage()),
        (route) => false,
      );
    } else if (index == 3) {
      // Handle profile navigation
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(builder: (context) => const DriverProfilePage()),
        (route) => false,
      );
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
          'Ride Offered',
          style: TextStyle(color: Colors.black87, fontWeight: FontWeight.w600),
        ),
        actions: [_buildProfileSection()],
      ),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              // Success Message Card
              AuthCard(
                children: [
                  const Icon(Icons.check_circle, size: 64, color: Colors.green),
                  const SizedBox(height: 16),
                  const Text(
                    'Ride Offered Successfully!',
                    style: TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.bold,
                      color: Colors.black87,
                    ),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Your ride has been posted and is now available for riders to book.',
                    style: TextStyle(fontSize: 14, color: Colors.grey.shade600),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 24),

                  // From and To Locations with Arrow
                  Row(
                    children: [
                      // From Location Column
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'From',
                              style: TextStyle(
                                fontSize: 12,
                                color: Colors.grey.shade600,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              widget.fromLocation,
                              style: const TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                                color: Colors.black87,
                              ),
                            ),
                          ],
                        ),
                      ),

                      // Arrow Icon
                      const Padding(
                        padding: EdgeInsets.symmetric(horizontal: 8),
                        child: Icon(
                          Icons.arrow_forward,
                          size: 20,
                          color: Colors.grey,
                        ),
                      ),

                      // To Location Column
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'To',
                              style: TextStyle(
                                fontSize: 12,
                                color: Colors.grey.shade600,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              widget.toLocation,
                              style: const TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                                color: Colors.black87,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),

                  // Seats Available
                  Row(
                    children: [
                      const Icon(
                        Icons.airline_seat_recline_normal,
                        size: 16,
                        color: Colors.grey,
                      ),
                      const SizedBox(width: 4),
                      Text(
                        '${widget.seats} seats available',
                        style: TextStyle(
                          fontSize: 14,
                          color: Colors.grey.shade600,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),

                  const Divider(height: 1, color: Colors.grey),
                  const SizedBox(height: 16),

                  // Date, Time and Price
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            _formatDateForDisplay(widget.date),
                            style: const TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.w600,
                              color: Colors.black87,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            'at ${widget.time}',
                            style: TextStyle(
                              fontSize: 14,
                              color: Colors.grey.shade600,
                            ),
                          ),
                        ],
                      ),
                      Text(
                        '\$${widget.price} / rider',
                        style: const TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.bold,
                          color: Colors.black87,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),

                  const Divider(height: 1, color: Colors.grey),
                  const SizedBox(height: 16),

                  // Return to Dashboard Button
                  AuthButton(
                    text: 'Start the Ride',
                    onPressed: () {
                      Navigator.of(context).pushAndRemoveUntil(
                        MaterialPageRoute(
                          builder: (context) => DriverRidersListPage(
                            fromLocation: widget.fromLocation,
                            toLocation: widget.toLocation,
                            date: widget.date,
                            time: widget.time,
                            seats: widget.seats,
                            price: widget.price,
                          ),
                        ),
                        (route) => false,
                      );
                    },
                  ),
                  const SizedBox(height: 12),
                  // Return to Dashboard Button
                  AuthButton(
                    isPrimary: false,
                    text: 'Return to Dashboard',
                    onPressed: () {
                      Navigator.of(context).pushAndRemoveUntil(
                        MaterialPageRoute(
                          builder: (context) =>
                              const DriverDashboard(userName: 'John Doe'),
                        ),
                        (route) => false,
                      );
                    },
                  ),
                ],
              ),
            ],
          ),
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

  String _formatDateForDisplay(String date) {
    // Convert "2025-09-13" to "Sep 13"
    try {
      final parts = date.split('-');
      if (parts.length == 3) {
        final month = int.parse(parts[1]);
        final day = int.parse(parts[2]);
        final monthName = _getMonthName(month);
        return '$monthName $day';
      }
    } catch (e) {
      debugPrint('Error formatting date: $e');
    }
    return date;
  }

  String _getMonthName(int month) {
    switch (month) {
      case 1:
        return 'Jan';
      case 2:
        return 'Feb';
      case 3:
        return 'Mar';
      case 4:
        return 'Apr';
      case 5:
        return 'May';
      case 6:
        return 'Jun';
      case 7:
        return 'Jul';
      case 8:
        return 'Aug';
      case 9:
        return 'Sep';
      case 10:
        return 'Oct';
      case 11:
        return 'Nov';
      case 12:
        return 'Dec';
      default:
        return '';
    }
  }
}
