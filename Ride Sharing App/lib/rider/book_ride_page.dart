import 'package:flutter/material.dart';
import 'package:my_app/profile/rider_profile/rider_profile_page.dart';
import 'package:my_app/rider/dashboard.dart';
import 'package:my_app/rider/ride_details_page.dart';
import 'package:my_app/wallet/rider_wallet_page.dart';
import 'package:my_app/widgets/auth/auth_button.dart';
import 'package:my_app/widgets/common/section_header.dart';
import 'package:my_app/widgets/rides/available_ride_card.dart';
import 'package:my_app/widgets/navigation/custom_bottom_nav_bar.dart';
import 'package:my_app/widgets/common/notification_icon.dart';
import 'package:my_app/widgets/locations/location_switch_card.dart';
import 'package:my_app/widgets/forms/date_time_input.dart';
import 'package:my_app/widgets/rides/safety_features_card.dart';

class BookRidePage extends StatefulWidget {
  const BookRidePage({super.key});

  @override
  State<BookRidePage> createState() => _BookRidePageState();
}

class _BookRidePageState extends State<BookRidePage> {
  bool _showResults = false;
  // final String userName;

  final String _homeLocation = 'HOME';
  final String _officeLocation = 'OFFICE';

  bool _isHomeToOffice = true;
  final TextEditingController _dateController = TextEditingController();
  final TextEditingController _timeController = TextEditingController();

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
    _setDefaultDateTime();
  }

  void _setDefaultDateTime() {
    final now = DateTime.now();
    _dateController.text = _formatDate(now);
    _timeController.text = _formatTime(now);
  }

  String _formatDate(DateTime date) {
    return '${date.year}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
  }

  String _formatTime(DateTime time) {
    return '${time.hour.toString().padLeft(2, '0')}:${time.minute.toString().padLeft(2, '0')}';
  }

  Future<void> _selectDate(BuildContext context) async {
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: DateTime.now(),
      firstDate: DateTime.now(),
      lastDate: DateTime(2100),
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            colorScheme: const ColorScheme.light(
              primary: Color(0xFF2563EB),
              onPrimary: Colors.white,
              onSurface: Colors.black87,
            ),
            textButtonTheme: TextButtonThemeData(
              style: TextButton.styleFrom(
                foregroundColor: const Color(0xFF2563EB),
              ),
            ),
          ),
          child: child!,
        );
      },
    );

    if (picked != null) {
      setState(() {
        _dateController.text = _formatDate(picked);
      });
    }
  }

  Future<void> _selectTime(BuildContext context) async {
    final TimeOfDay? picked = await showTimePicker(
      context: context,
      initialTime: TimeOfDay.now(),
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            colorScheme: const ColorScheme.light(
              primary: Color(0xFF2563EB),
              onPrimary: Colors.white,
              onSurface: Colors.black87,
            ),
            textButtonTheme: TextButtonThemeData(
              style: TextButton.styleFrom(
                foregroundColor: const Color(0xFF2563EB),
              ),
            ),
          ),
          child: child!,
        );
      },
    );

    if (picked != null) {
      setState(() {
        _timeController.text = picked.format(context);
      });
    }
  }

  void _toggleLocations() {
    setState(() {
      _isHomeToOffice = !_isHomeToOffice;
    });
  }

  @override
  void dispose() {
    _dateController.dispose();
    _timeController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final fromLocation = _isHomeToOffice ? _homeLocation : _officeLocation;
    final toLocation = _isHomeToOffice ? _officeLocation : _homeLocation;
    final fromIcon = _isHomeToOffice ? Icons.home : Icons.work;
    final toIcon = _isHomeToOffice ? Icons.work : Icons.home;

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
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Welcome Message
            SectionHeader(
              title: 'Shall we Start?',
              // title: 'Welcome back, ${widget.userName}',
              subtitle: 'Lets find someone to share your ride with',
              textAlign: TextAlign.left,
            ),
            const SizedBox(height: 16),

            // Location Switch Card
            LocationSwitchCard(
              fromLocation: fromLocation,
              toLocation: toLocation,
              fromIcon: fromIcon,
              toIcon: toIcon,
              onSwitch: _toggleLocations,
            ),

            const SizedBox(height: 16),

            // Date and Time Row
            Row(
              children: [
                Expanded(
                  child: DateTimeInput(
                    controller: _dateController,
                    hintText: 'Select Date',
                    suffixIcon: const Icon(
                      Icons.calendar_today,
                      size: 18,
                      color: Colors.black45,
                    ),
                    onTap: () => _selectDate(context),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: DateTimeInput(
                    controller: _timeController,
                    hintText: 'Select Time',
                    suffixIcon: const Icon(
                      Icons.access_time,
                      size: 18,
                      color: Colors.black45,
                    ),
                    onTap: () => _selectTime(context),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 20),

            // Search Rides Button
            AuthButton(
              text: 'Search Rides',
              onPressed: () {
                setState(() {
                  _showResults = true;
                });
              },
            ),

            const SizedBox(height: 16),

            // Safety Features Card
            const SafetyFeaturesCard(),

            if (_showResults) ...[
              const SizedBox(height: 16),
              _buildResultsSection(fromLocation, toLocation),
            ],
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

  Widget _buildResultsSection(String fromLocation, String toLocation) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // Header + search
        Row(
          children: [
            const Expanded(
              child: Text(
                'Available Rides',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w600,
                  color: Colors.black87,
                ),
              ),
            ),
            Expanded(child: _buildSearchField()),
          ],
        ),
        const SizedBox(height: 12),

        // Ride cards
        AvailableRideCard(
          avatarUrl: 'https://randomuser.me/api/portraits/men/32.jpg',
          name: 'David Wilson',
          seatsText: '2 seats available',
          fromAddress: fromLocation,
          toAddress: toLocation,
          date: 'Today',
          time: '5:30 PM',
          price: '12.50',
          onBook: () {
            Navigator.of(context).push(
              MaterialPageRoute(
                builder: (_) => RideDetailsPage(
                  avatarUrl: 'https://randomuser.me/api/portraits/men/32.jpg',
                  name: 'David Wilson',
                  rating: 4.8,
                  carModel: 'Toyota Camry',
                  license: 'ABC 1234',
                  from: fromLocation,
                  to: toLocation,
                  time: '5:30 PM',
                  price: r'$ 12.50',
                ),
              ),
            );
          },
        ),
        const SizedBox(height: 12),
        AvailableRideCard(
          avatarUrl: 'https://randomuser.me/api/portraits/women/68.jpg',
          name: 'Amanda Lee',
          seatsText: '3 seats available',
          fromAddress: fromLocation,
          toAddress: toLocation,
          date: 'Today',
          time: '5:45 PM',
          price: '13.00',
          onBook: () {
            Navigator.of(context).push(
              MaterialPageRoute(
                builder: (_) => RideDetailsPage(
                  avatarUrl: 'https://randomuser.me/api/portraits/women/68.jpg',
                  name: 'Amanda Lee',
                  rating: 4.9,
                  carModel: 'Honda Civic',
                  license: 'XYZ 5678',
                  from: fromLocation,
                  to: toLocation,
                  time: '5:45 PM',
                  price: r'$ 13.00',
                ),
              ),
            );
          },
        ),
        const SizedBox(height: 12),
        AvailableRideCard(
          avatarUrl: 'https://randomuser.me/api/portraits/men/75.jpg',
          name: 'Thomas Brown',
          seatsText: '1 seat available',
          fromAddress: fromLocation,
          toAddress: toLocation,
          date: 'Today',
          time: '6:00 PM',
          price: '11.75',
          onBook: () {
            Navigator.of(context).push(
              MaterialPageRoute(
                builder: (_) => RideDetailsPage(
                  avatarUrl: 'https://randomuser.me/api/portraits/men/75.jpg',
                  name: 'Thomas Brown',
                  rating: 4.7,
                  carModel: 'Hyundai Elantra',
                  license: 'LMN 2468',
                  from: fromLocation,
                  to: toLocation,
                  time: '6:00 PM',
                  price: r'$ 11.75',
                ),
              ),
            );
          },
        ),
      ],
    );
  }

  Widget _buildSearchField() {
    return TextField(
      decoration: InputDecoration(
        hintText: 'Search by driver',
        prefixIcon: const Icon(Icons.search),
        isDense: true,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 12,
          vertical: 10,
        ),
        filled: true,
        fillColor: Colors.white,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: Color(0xFFE0E0E0)),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: Color(0xFFE0E0E0)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(10),
          borderSide: const BorderSide(color: Color(0xFF2563EB), width: 2),
        ),
      ),
    );
  }
}
