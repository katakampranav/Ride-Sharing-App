import 'package:flutter/material.dart';
import 'package:my_app/driver/dashboard.dart';
import 'package:my_app/driver/registrationPage/registration.dart';
import 'package:my_app/loginPage/reset_password_page.dart';
import 'package:my_app/rider/dashboard.dart';
import 'package:my_app/rider/registrationPage/registration.dart';
import 'package:my_app/widgets/auth/auth_card.dart';
import 'package:my_app/widgets/auth/auth_tab_selector.dart';
import 'package:my_app/widgets/auth/auth_text_field.dart';
import 'package:my_app/widgets/auth/remember_me_forgot_password.dart';
import 'package:my_app/widgets/auth/auth_button.dart';
import 'package:my_app/widgets/auth/registration_buttons_row.dart';

class LoginForm extends StatefulWidget {
  const LoginForm({super.key});

  @override
  State<LoginForm> createState() => _LoginFormState();
}

class _LoginFormState extends State<LoginForm> {
  bool isRiderSelected = true;
  bool rememberMe = false;

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      child: AuthCard(
        children: [
          AuthTabSelector(
            initialIsRiderSelected: isRiderSelected,
            onTabChanged: (bool isRider) {
              setState(() {
                isRiderSelected = isRider;
              });
            },
          ),
          const SizedBox(height: 24),

          const AuthTextField(
            label: 'Email Id',
            hintText: 'abc@gmail.com',
            keyboardType: TextInputType.emailAddress,
          ),
          const SizedBox(height: 16),

          const AuthTextField(
            label: 'Password',
            hintText: 'Enter your Password',
            obscureText: true,
          ),
          const SizedBox(height: 16),

          RememberMeForgotPassword(
            rememberMe: rememberMe,
            onRememberMeChanged: (bool? value) {
              setState(() {
                rememberMe = value ?? false;
              });
            },
            onForgotPasswordPressed: () {
              Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (context) => const ResetPasswordPage(),
                ),
              );
            },
          ),
          const SizedBox(height: 24),

          AuthButton(
            text: 'Sign in',
            onPressed: () {
              debugPrint("Remember me: $rememberMe");

              if (isRiderSelected) {
                Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (context) => RiderDashboard(userName: "John Doe"),
                  ),
                );
              } else {
                // Navigate to Driver Dashboard
                Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (context) => DriverDashboard(userName: "John Doe"),
                  ),
                );
              }
            },
          ),
          const SizedBox(height: 16),

          const Center(
            child: Text(
              "Don't have an account?",
              style: TextStyle(fontSize: 14, color: Colors.black54),
            ),
          ),
          const SizedBox(height: 32),

          RegistrationButtonsRow(
            onRiderRegister: () {
              Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (context) => const RiderRegistrationPage(),
                ),
              );
            },
            onDriverRegister: () {
              Navigator.of(context).push(
                MaterialPageRoute(
                  builder: (context) => const DriverRegistrationPage(),
                ),
              );
            },
          ),
        ],
      ),
    );
  }
}
