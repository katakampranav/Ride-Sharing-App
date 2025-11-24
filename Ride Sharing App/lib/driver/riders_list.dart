import 'package:flutter/material.dart';
import 'package:my_app/driver/driver_pickup_map_page.dart';
import 'package:my_app/driver/follow_up_page.dart';
import 'package:my_app/widgets/common/notification_icon.dart';
import 'package:my_app/widgets/rides/waiting_rider_card.dart';

class DriverRidersListPage extends StatefulWidget {
  final String fromLocation;
  final String toLocation;
  final String date;
  final String time;
  final String seats;
  final String price;

  const DriverRidersListPage({
    super.key,
    required this.fromLocation,
    required this.toLocation,
    required this.date,
    required this.time,
    required this.seats,
    required this.price,
  });

  @override
  State<DriverRidersListPage> createState() => _DriverRidersListPageState();
}

class _DriverRidersListPageState extends State<DriverRidersListPage> {
  final List<Rider> _availableRiders = [
    Rider(
      name: 'Alex Chen',
      profilePhoto:
          'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=400&auto=format&fit=crop&q=60&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8M3x8cmFuZG9tJTIwcGVvcGxlfGVufDB8fDB8fHww',
      distance: '1.2 km away',
      fromLocation: 'Central Station',
      toLocation: 'Business District',
    ),
    Rider(
      name: 'Maria Garcia',
      profilePhoto:
          'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=400&auto=format&fit=crop&q=60&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8NHx8cmFuZG9tJTIwcGVvcGxlfGVufDB8fDB8fHww',
      distance: '0.5 km away',
      fromLocation: 'Shopping Complex',
      toLocation: 'Residential Area',
      detourInfo: '0.8km detour',
    ),
    Rider(
      name: 'David Kim',
      profilePhoto:
          'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400&auto=format&fit=crop&q=60&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Nnx8cmFuZG9tJTIwcGVvcGxlfGVufDB8fDB8fHww',
      distance: '1.5 km away',
      fromLocation: 'University Campus',
      toLocation: 'City Center',
    ),
  ];

  void _handlePickup(Rider rider) {
    // Navigate to map page
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (context) => DriverPickupMapPage(
          riderName: rider.name,
          riderPhoto: rider.profilePhoto,
          fromLocation: rider.fromLocation,
          toLocation: rider.toLocation,
          onPickupComplete: () {
            // Remove the rider from the list when pickup is complete
            _removeRider(rider);
          },
        ),
      ),
    );
  }

  void _removeRider(Rider rider) {
    setState(() {
      _availableRiders.remove(rider);
    });

    // If no riders left, navigate to confirmation page
    if (_availableRiders.isEmpty) {
      _navigateToConfirmationPage();
    }
  }

  void _navigateToConfirmationPage() {
    Future.delayed(const Duration(milliseconds: 500), () {
      // ignore: use_build_context_synchronously
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(
          builder: (context) => FollowUpPage(
            driverName: 'Michael Johnson',
            driverRole: 'QA Engineer',
            carModel: 'Silver Toyota',
            licensePlate: 'ABC1024',
            from: widget.fromLocation,
            to: widget.toLocation,
            pickupTime: widget.time,
            eta: '30 min',
            price: widget.price,
            avatarUrl: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?fm=jpg&q=60&w=3000&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8M3x8cmFuZG9tJTIwcGVvcGxlfGVufDB8fDB8fHww',
          ),
        ),
        (route) => false,
      );
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
          'Available Riders',
          style: TextStyle(color: Colors.black87, fontWeight: FontWeight.w600),
        ),
        actions: [_buildProfileSection()],
      ),
      body: Column(
        children: [
          // Riders List Header
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
            child: Row(
              children: [
                const Icon(Icons.people_outline, size: 30, color: Colors.grey),
                const SizedBox(width: 8),
                Text(
                  '${_availableRiders.length} riders waiting',
                  style: const TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.w600,
                    color: Colors.black87,
                  ),
                ),
              ],
            ),
          ),

          const SizedBox(height: 16),

          // Riders List
          Expanded(
            child: _availableRiders.isEmpty
                ? _buildEmptyState()
                : ListView.builder(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    itemCount: _availableRiders.length,
                    itemBuilder: (context, index) {
                      final rider = _availableRiders[index];
                      return Column(
                        children: [
                          WaitingRideCard(
                            avatarUrl: rider.profilePhoto,
                            name: rider.name,
                            kms: rider.distance,
                            fromAddress: rider.fromLocation,
                            toAddress: rider.toLocation,
                            detourInfo: rider.detourInfo,
                            onPickup: () => _handlePickup(rider),
                          ),
                          const SizedBox(height: 16),
                        ],
                      );
                    },
                  ),
          ),
        ],
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(
            Icons.check_circle_outline,
            size: 80,
            color: Colors.green,
          ),
          const SizedBox(height: 16),
          const Text(
            'All riders picked up!',
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: Colors.black87,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            'Taking you to confirmation page...',
            style: TextStyle(
              fontSize: 14,
              color: Colors.grey.shade600,
            ),
          ),
        ],
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
}

class Rider {
  final String name;
  final String profilePhoto;
  final String distance;
  final String fromLocation;
  final String toLocation;
  final String? detourInfo;

  Rider({
    required this.name,
    required this.profilePhoto,
    required this.distance,
    required this.fromLocation,
    required this.toLocation,
    this.detourInfo,
  });
}