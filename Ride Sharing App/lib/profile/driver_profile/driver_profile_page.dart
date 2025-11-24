import 'package:flutter/material.dart';
import 'package:my_app/driver/dashboard.dart';
import 'package:my_app/driver/offer_ride_page.dart';
import 'package:my_app/loginPage/login_page.dart';
import 'package:my_app/profile/driver_profile/vehicle_information_page.dart';
import 'package:my_app/wallet/driver_wallet_page.dart';
import 'package:my_app/widgets/profile/action_section.dart';
import 'package:my_app/widgets/navigation/custom_bottom_nav_bar.dart';
import 'package:my_app/widgets/profile/edit_profile_dialog.dart';
import 'package:my_app/widgets/common/notification_icon.dart';
import 'package:my_app/widgets/profile/personal_info_section.dart';
import 'package:my_app/widgets/profile/profile_header.dart';

class DriverProfilePage extends StatefulWidget {
  const DriverProfilePage({super.key});

  @override
  State<DriverProfilePage> createState() => _DriverProfilePageState();
}

class _DriverProfilePageState extends State<DriverProfilePage> {
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

  // User data that can be edited
  String _name = 'John Doe';
  String _phone = '+1 (555) 123-4567';
  String _email = 'johndoe@example.com';
  String _vehicleModel = 'Toyota Camry (2020)';
  String _licensePlate = 'ABC-1234';
  String _driverLicense = 'DL-9876543210';

  final TextEditingController _nameController = TextEditingController();
  final TextEditingController _phoneController = TextEditingController();
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _vehicleController = TextEditingController();
  final TextEditingController _licenseController = TextEditingController();
  final TextEditingController _driverLicenseController =
      TextEditingController();

  @override
  void initState() {
    super.initState();
    // Initialize controllers with current data
    _nameController.text = _name;
    _phoneController.text = _phone;
    _emailController.text = _email;
    _vehicleController.text = _vehicleModel;
    _licenseController.text = _licensePlate;
    _driverLicenseController.text = _driverLicense;
  }

  @override
  void dispose() {
    _nameController.dispose();
    _phoneController.dispose();
    _emailController.dispose();
    _vehicleController.dispose();
    _licenseController.dispose();
    _driverLicenseController.dispose();
    super.dispose();
  }

  void _showEditProfileDialog() {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return EditProfileDialog(
          nameController: _nameController,
          phoneController: _phoneController,
          emailController: _emailController,
          vehicleController: _vehicleController,
          licenseController: _licenseController,
          driverLicenseController: _driverLicenseController,
          onSave: () {
            setState(() {
              _name = _nameController.text;
              _phone = _phoneController.text;
              _email = _emailController.text;
              _vehicleModel = _vehicleController.text;
              _licensePlate = _licenseController.text;
              _driverLicense = _driverLicenseController.text;
            });
            Navigator.of(context).pop();
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(
                content: Text('Profile updated successfully!'),
                backgroundColor: Colors.green,
              ),
            );
          },
        );
      },
    );
  }

  void _handleVehicleDetails() {
    Navigator.of(
      context,
    ).push(MaterialPageRoute(builder: (context) => const VehicleInformationPage()));
  }

  void _handleNotifications() {
    // Handle notifications navigation
  }

  void _handleLogout() {
    // Handle logout logic
    Navigator.of(context).pushAndRemoveUntil(
      MaterialPageRoute(
        builder: (context) => const LoginPage(),
      ),
      (route) => false,
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF5F5F5),
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
        child: Padding(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            children: [
              // Profile Header Section
              ProfileHeader(
                name: _name,
                imageUrl:
                    'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=600&auto=format&fit=crop&q=60&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8cmFuZG9tJTIwcGVvcGxlfGVufDB8fDB8fHww',
                rating: 4.8,
                rideCount: 124,
                isVerified: true,
              ),

              const SizedBox(height: 16),

              // Personal Information Section
              PersonalInfoSection(
                phone: _phone,
                email: _email,
                vehicleModel: _vehicleModel,
                licensePlate: _licensePlate,
                driverLicense: _driverLicense,
                memberSince: 'January 2022',
              ),

              const SizedBox(height: 16),

              // Action Items Section
              ActionSection.driver(
                onEditProfile: _showEditProfileDialog,
                onVehicleDetails: _handleVehicleDetails,
                onNotifications: _handleNotifications,
                onLogout: _handleLogout,
              ),
            ],
          ),
        ),
      ),
      bottomNavigationBar: CustomBottomNavBar(
        currentIndex: 3,
        onTap: _onNavItemTapped,
        items: _navItems,
      ),
    );
  }
}
