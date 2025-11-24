import 'package:flutter/material.dart';
import 'package:my_app/profile/rider_profile/rider_profile_page.dart';
import 'package:my_app/rider/book_ride_page.dart';
import 'package:my_app/rider/dashboard.dart';
import 'package:my_app/widgets/wallet/add_funds_form.dart';
import 'package:my_app/widgets/wallet/balance_card.dart';
import 'package:my_app/widgets/common/notification_icon.dart';
import 'package:my_app/widgets/wallet/payment_dialog.dart';
import 'package:my_app/widgets/wallet/transaction_item.dart';
import 'package:my_app/widgets/navigation/custom_bottom_nav_bar.dart';

class WalletPage extends StatefulWidget {
  const WalletPage({super.key});

  @override
  State<WalletPage> createState() => _WalletPageState();
}

class _WalletPageState extends State<WalletPage>
    with SingleTickerProviderStateMixin {
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

  final List<Map<String, dynamic>> _transactions = [
    {
      'title': 'Ride to Downtown Apartments',
      'date': '15 May, 2023',
      'time': 'at 5:45 PM',
      'amount': -12.50,
      'type': 'ride',
    },
    {
      'title': 'Added funds',
      'date': '10 May, 2023',
      'time': 'at 2:30 PM',
      'amount': 50.00,
      'type': 'topup',
    },
    {
      'title': 'Ride to Corporate HQ',
      'date': '8 May, 2023',
      'time': 'at 8:15 AM',
      'amount': -15.75,
      'type': 'ride',
    },
    {
      'title': 'Cancellation fee',
      'date': '5 May, 2023',
      'time': 'at 4:20 PM',
      'amount': -5.00,
      'type': 'fee',
    },
  ];

  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  void _showPaymentDialog(String amount) {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (BuildContext context) {
        return PaymentDialog(amount: amount);
      },
    );
  }

  void _handleAddFunds(String amount, String method) {
    _showPaymentDialog(amount);
  }

  void _switchToAddFundsTab() {
    _tabController.animateTo(1);
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
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.white,
        elevation: 0,
        foregroundColor: Colors.black87,
        title: const Text(
          'Wallet',
          style: TextStyle(color: Colors.black87, fontWeight: FontWeight.w600),
        ),
        actions: [_buildProfileSection()],
      ),
      body: Column(
        children: [
          // Balance Section using your AuthButton
          BalanceCard(balance: 500.00, onAddFundsPressed: _switchToAddFundsTab),

          // Divider
          Container(height: 8, color: const Color(0xFFF5F5F5)),

          // Tab Bar
          Container(
            color: Colors.white,
            child: TabBar(
              controller: _tabController,
              labelColor: Colors.blue,
              unselectedLabelColor: Colors.black54,
              indicatorColor: Colors.blue,
              tabs: const [
                Tab(text: 'Transactions'),
                Tab(text: 'Add Funds'),
              ],
            ),
          ),

          // Tab Content
          Expanded(
            child: TabBarView(
              controller: _tabController,
              children: [
                // Transactions Tab
                ListView.builder(
                  itemCount: _transactions.length,
                  itemBuilder: (context, index) {
                    final transaction = _transactions[index];
                    return TransactionItem(
                      title: transaction['title'],
                      date: transaction['date'],
                      time: transaction['time'],
                      amount: transaction['amount'],
                      type: transaction['type'],
                    );
                  },
                ),

                // Add Funds Tab using your AuthTextField and AuthButton
                AddFundsForm(onAddFunds: _handleAddFunds),
              ],
            ),
          ),
        ],
      ),
      bottomNavigationBar: CustomBottomNavBar(
        currentIndex: 2,
        onTap: _onNavItemTapped,
        items: _navItems,
      ),
    );
  }
}
