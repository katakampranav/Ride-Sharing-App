import 'package:flutter/material.dart';
import 'package:my_app/loginPage/login_page.dart';
import 'package:my_app/profile/rider_profile/rider_profile_page.dart';
import 'package:my_app/rider/book_ride_page.dart';
import 'package:my_app/rider/dashboard.dart';
import 'package:my_app/wallet/rider_wallet_page.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Corporate Ride Share',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
        useMaterial3: true,
      ),
      home: const LoginPage(),
      routes: {
        '/dashboard': (context) => RiderDashboard(userName: 'John Doe',),
        '/book-ride': (context) => BookRidePage(),
        '/wallet': (context) => WalletPage(),
        '/profile': (context) => ProfilePage(),
      },
    );
  }
}
