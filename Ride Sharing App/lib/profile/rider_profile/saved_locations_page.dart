import 'package:flutter/material.dart';
import 'package:my_app/profile/rider_profile/rider_profile_page.dart';
import 'package:my_app/rider/book_ride_page.dart';
import 'package:my_app/rider/dashboard.dart';
import 'package:my_app/wallet/rider_wallet_page.dart';
import 'package:my_app/widgets/locations/add_location_button.dart';
import 'package:my_app/widgets/locations/add_location_dialog.dart';
import 'package:my_app/widgets/navigation/custom_bottom_nav_bar.dart';
import 'package:my_app/widgets/locations/locations_list.dart';

class SavedLocationsPage extends StatefulWidget {
  const SavedLocationsPage({super.key});

  @override
  State<SavedLocationsPage> createState() => _SavedLocationsPageState();
}

class _SavedLocationsPageState extends State<SavedLocationsPage> {
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
  final List<Map<String, String>> _savedLocations = [
    {
      'title': 'Home',
      'address': '123 Residential St, Apt 456',
      'type': 'home',
    },
    {
      'title': 'Corporate HQ',
      'address': '789 Business Ave, Building A',
      'type': 'work',
    },
    {
      'title': 'Downtown Apartments',
      'address': '101 City Center Blvd',
      'type': 'other',
    },
  ];

  void _showAddLocationDialog() {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AddLocationDialog(
          onSave: (title, address, type) {
            setState(() {
              _savedLocations.add({
                'title': title,
                'address': address,
                'type': type,
              });
            });
            Navigator.of(context).pop();
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(
                content: Text('Location saved successfully!'),
                backgroundColor: Colors.green,
              ),
            );
          },
        );
      },
    );
  }

  void _deleteLocation(int index) {
    setState(() {
      _savedLocations.removeAt(index);
    });
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Location deleted'),
        backgroundColor: Colors.red,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        foregroundColor: Colors.black87,
        title: const Text(
          'Saved Locations',
          style: TextStyle(
            fontWeight: FontWeight.w600,
            fontSize: 20,
            color: Colors.black87,
          ),
        ),
        centerTitle: true,
      ),
      body: Column(
        children: [
          // Locations List
          Expanded(
            child: LocationsList(
              locations: _savedLocations,
              onDeleteLocation: _deleteLocation,
            ),
          ),

          // Add New Location Button
          AddLocationButton(onPressed: _showAddLocationDialog),
        ],
      ),
      bottomNavigationBar: CustomBottomNavBar(
        currentIndex: 3,
        onTap: _onNavItemTapped,
        items: _navItems,
      ),
    );
  }
}