import 'package:flutter/material.dart';
import 'package:my_app/driver/dashboard.dart';
import 'package:my_app/driver/driver_ride_summary_page.dart';
import 'package:my_app/profile/driver_profile/driver_profile_page.dart';
import 'package:my_app/wallet/driver_wallet_page.dart';
import 'package:my_app/widgets/auth/auth_button.dart';
import 'package:my_app/widgets/navigation/custom_bottom_nav_bar.dart';
import 'package:my_app/widgets/common/notification_icon.dart';
import 'package:my_app/widgets/locations/location_switch_card.dart';
import 'package:my_app/widgets/forms/date_time_input.dart';

class DriverOfferRidePage extends StatefulWidget {
  const DriverOfferRidePage({super.key});

  @override
  State<DriverOfferRidePage> createState() => _DriverOfferRidePageState();
}

class _DriverOfferRidePageState extends State<DriverOfferRidePage> {
  final String _homeLocation = 'HOME';
  final String _officeLocation = 'OFFICE';

  bool _isHomeToOffice = true;
  final TextEditingController _dateController = TextEditingController();
  final TextEditingController _timeController = TextEditingController();
  final TextEditingController _seatsController = TextEditingController(
    text: '3',
  );
  final TextEditingController _priceController = TextEditingController(
    text: '15.00',
  );

  bool _isRecurringRide = false;
  double _detourDistance = 0.5;

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

  bool _isOnline = false;
  String _logoutTime = 'Not set';

  void _toggleOnlineStatus() {
    setState(() {
      _isOnline = !_isOnline;
    });
  }

  void _showUpdateLogoutTimeDialog() {
    TimeOfDay selectedTime = const TimeOfDay(
      hour: 17,
      minute: 0,
    ); // Default to 17:00

    showDialog(
      context: context,
      builder: (BuildContext context) {
        return StatefulBuilder(
          builder: (context, setState) {
            return Dialog(
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
              child: Padding(
                padding: const EdgeInsets.all(24.0),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Title
                    const Center(
                      child: Text(
                        'Update Logout Time',
                        style: TextStyle(
                          fontSize: 20,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),

                    const SizedBox(height: 20),

                    // Today's Logout Time
                    const Text(
                      "Today's Logout Time ( Monday )",
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w500,
                      ),
                    ),

                    const SizedBox(height: 16),

                    // Time Picker Container
                    GestureDetector(
                      onTap: () async {
                        final TimeOfDay? picked = await showTimePicker(
                          context: context,
                          initialTime: selectedTime,
                          builder: (BuildContext context, Widget? child) {
                            return Theme(
                              data: ThemeData.light().copyWith(
                                colorScheme: const ColorScheme.light(
                                  primary: Colors.blue,
                                ),
                              ),
                              child: child!,
                            );
                          },
                        );
                        if (picked != null) {
                          setState(() {
                            selectedTime = picked;
                          });
                        }
                      },
                      child: Container(
                        width: double.infinity,
                        padding: const EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          color: Colors.grey.shade100,
                          borderRadius: BorderRadius.circular(8),
                          border: Border.all(color: Colors.grey.shade300),
                        ),
                        child: Center(
                          child: Text(
                            '${selectedTime.hour.toString().padLeft(2, '0')}:${selectedTime.minute.toString().padLeft(2, '0')}',
                            style: const TextStyle(
                              fontSize: 24,
                              fontWeight: FontWeight.bold,
                              color: Colors.blue,
                            ),
                          ),
                        ),
                      ),
                    ),

                    const SizedBox(height: 16),

                    // Info Text
                    const Text(
                      'Need to update other days? You can set your weekly schedule in your profile settings.',
                      style: TextStyle(fontSize: 14, color: Colors.grey),
                    ),

                    const SizedBox(height: 24),

                    // Action Buttons
                    Row(
                      children: [
                        Expanded(
                          child: AuthButton(
                            text: 'Cancel',
                            isPrimary: false,
                            onPressed: () {
                              Navigator.of(context).pop();
                            },
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: AuthButton(
                            text: 'Update',
                            onPressed: () {
                              // Update the logout time
                              final formattedTime =
                                  '${selectedTime.hour.toString().padLeft(2, '0')}:${selectedTime.minute.toString().padLeft(2, '0')}';
                              setState(() {
                                _logoutTime = formattedTime;
                              });
                              Navigator.of(context).pop();
                            },
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            );
          },
        );
      },
    );
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
    _seatsController.dispose();
    _priceController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final fromLocation = _isHomeToOffice ? _homeLocation : _officeLocation;
    final toLocation = _isHomeToOffice ? _officeLocation : _homeLocation;
    final fromIcon = _isHomeToOffice ? Icons.work : Icons.home;
    final toIcon = _isHomeToOffice ? Icons.home : Icons.work;

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
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Today's logout time section
            _buildLogoutTimeSection(),

            const SizedBox(height: 20),

            // Go Online/Offline Button
            SizedBox(
              width: double.infinity,
              child: AuthButton(
                text: _isOnline ? 'Go Offline' : 'Go Online',
                isPrimary: false,
                onPressed: _toggleOnlineStatus,
                backgroundColor: Colors.transparent,
                borderColor: _isOnline ? Colors.red : Colors.green,
                textColor: _isOnline ? Colors.red : Colors.green,
              ),
            ),

            const SizedBox(height: 24),

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
            const SizedBox(height: 16),

            // Recurring Ride Checkbox
            Row(
              children: [
                Checkbox(
                  value: _isRecurringRide,
                  onChanged: (bool? value) {
                    setState(() {
                      _isRecurringRide = value ?? false;
                    });
                  },
                  activeColor: const Color(0xFF2563EB),
                ),
                const Text(
                  'This is a recurring ride',
                  style: TextStyle(fontSize: 14, color: Colors.black87),
                ),
              ],
            ),
            const SizedBox(height: 16),

            // Available Seats and Price Row
            Row(
              children: [
                Expanded(
                  child: _buildNumberField(
                    label: 'Available Seats',
                    controller: _seatsController,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _buildNumberField(
                    label: 'Price per Rider (\$)',
                    controller: _priceController,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 20),

            // Maximum Detour Distance
            _buildDetourDistanceSection(),
            const SizedBox(height: 16),

            // Info Text
            Container(
              padding: const EdgeInsets.all(12),
              width: double.infinity,
              color: Colors.yellow.shade100,
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Padding(
                    padding: EdgeInsets.only(top: 2.0),
                    child: Icon(
                      Icons.info_outline,
                      color: Colors.black54,
                      size: 16,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    // Add Expanded here to constrain the text width
                    child: Text(
                      'By offering a ride, you agree to pick up riders along your route within your specified detour distance.',
                      style: const TextStyle(fontSize: 12, color: Colors.grey),
                      textAlign: TextAlign.left,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),

            // In DriverOfferRidePage's Continue button
            AuthButton(
              text: 'Offer a Ride',
              onPressed: () {
                Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (context) => DriverRideSummaryPage(
                      fromLocation: fromLocation,
                      toLocation: toLocation,
                      date: _dateController.text,
                      time: _timeController.text,
                      seats: _seatsController.text,
                      price: _priceController.text,
                      detourDistance: _detourDistance.toString(),
                      isRecurring: _isRecurringRide,
                    ),
                  ),
                );
              },
            ),
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

  Widget _buildLogoutTimeSection() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
            color: Colors.grey.withOpacity(0.1),
            blurRadius: 4,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Row(
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                "Today's logout time:",
                style: Theme.of(
                  context,
                ).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              Text(
                _logoutTime,
                style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                  color: _logoutTime == 'Not set'
                      ? Colors.grey.shade600
                      : Colors.blue,
                  fontWeight: _logoutTime == 'Not set'
                      ? FontWeight.normal
                      : FontWeight.bold,
                ),
              ),
            ],
          ),
          const Spacer(),
          SizedBox(
            width: 100,
            child: AuthButton(
              isPrimary: false,
              text: 'Update',
              onPressed: _showUpdateLogoutTimeDialog,
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

  Widget _buildNumberField({
    required String label,
    required TextEditingController controller,
  }) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: const TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w500,
            color: Colors.black87,
          ),
        ),
        const SizedBox(height: 8),
        TextField(
          controller: controller,
          keyboardType: TextInputType.number,
          decoration: InputDecoration(
            filled: true,
            fillColor: Colors.white,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(8),
              borderSide: const BorderSide(color: Color(0xFFE0E0E0)),
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(8),
              borderSide: const BorderSide(color: Color(0xFFE0E0E0)),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(8),
              borderSide: const BorderSide(color: Color(0xFF2563EB), width: 2),
            ),
            contentPadding: const EdgeInsets.symmetric(
              horizontal: 12,
              vertical: 12,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildDetourDistanceSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'Maximum Detour Distance (km)',
          style: TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.w500,
            color: Colors.black87,
          ),
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            _buildDetourOption(0.5, '0.5 km'),
            const SizedBox(width: 8),
            _buildDetourOption(1.0, '1 km'),
            const SizedBox(width: 8),
            _buildDetourOption(1.5, '1.5 km'),
            const SizedBox(width: 8),
            _buildDetourOption(2.0, '2 km'),
          ],
        ),
      ],
    );
  }

  Widget _buildDetourOption(double value, String label) {
    final isSelected = _detourDistance == value;
    return Expanded(
      child: GestureDetector(
        onTap: () {
          setState(() {
            _detourDistance = value;
          });
        },
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 12),
          decoration: BoxDecoration(
            color: isSelected ? const Color(0xFF2563EB) : Colors.white,
            borderRadius: BorderRadius.circular(8),
            border: Border.all(
              color: isSelected
                  ? const Color(0xFF2563EB)
                  : Colors.grey.shade300,
            ),
          ),
          child: Center(
            child: Text(
              label,
              style: TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w500,
                color: isSelected ? Colors.white : Colors.black87,
              ),
            ),
          ),
        ),
      ),
    );
  }
}
