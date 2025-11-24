import 'package:flutter/material.dart';
import 'package:my_app/widgets/auth/auth_button.dart';

class PaymentDialog extends StatefulWidget {
  final String amount;

  const PaymentDialog({super.key, required this.amount});

  @override
  State<PaymentDialog> createState() => _PaymentDialogState();
}

class _PaymentDialogState extends State<PaymentDialog> {
  int _remainingTime = 229;

  @override
  void initState() {
    super.initState();
    _startTimer();
  }

  void _startTimer() {
    Future.delayed(const Duration(seconds: 1), () {
      if (_remainingTime > 0 && mounted) {
        setState(() {
          _remainingTime--;
        });
        _startTimer();
      }
    });
  }

  String _formatTime(int seconds) {
    int minutes = seconds ~/ 60;
    int remainingSeconds = seconds % 60;
    return '${minutes.toString().padLeft(2, '0')}:${remainingSeconds.toString().padLeft(2, '0')}';
  }

  @override
  Widget build(BuildContext context) {
    return Dialog(
      backgroundColor: Colors.white,
      insetPadding: const EdgeInsets.all(20),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Image.network(
                'https://currentaffairsonly.wordpress.com/wp-content/uploads/2017/01/bhim-app.jpg?w=640',
                height: 40,
                width: 40,
              ),
              const SizedBox(height: 20),
              
              Row(
                children: [
                  const Text(
                    "Pay to\n CCAvenue",
                    style: TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.bold,
                      color: Colors.black87,
                    ),
                    textAlign: TextAlign.center,
                  ),
                  const Spacer(),
                  Text(
                    "Amount \$${widget.amount}",
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.w600,
                      color: Colors.black87,
                    ),
                    textAlign: TextAlign.center,
                  ),
                ],
              ),
              const SizedBox(height: 20),
              
              const Divider(height: 1, color: Colors.grey),
              const SizedBox(height: 20),
              
              const Text(
                "Enter your UPI PIN on your BHIM mobile app and authorize the payment",
                style: TextStyle(
                  fontSize: 14,
                  color: Colors.black54,
                  height: 1.5,
                  fontWeight: FontWeight.w900,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 20),
              
              const Icon(Icons.phone_android, size: 60),
              const SizedBox(height: 20),
              
              Text(
                "Complete your payment in ${_formatTime(_remainingTime)}",
                style: const TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 8),
              
              const Text(
                "Do not press back/refresh button while we are processing your payment",
                style: TextStyle(
                  fontSize: 12,
                  color: Colors.black54,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 20),
              
              Image.network(
                'https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcToez0OLRfxx26lyM8_defkYEHxarohym0O2g&s',
                height: 50,
                width: 50,
              ),
              const SizedBox(height: 20),
              
              AuthButton(
                text: "Cancel Payment",
                onPressed: () => Navigator.of(context).pop(),
                isPrimary: false,
              ),
            ],
          ),
        ),
      ),
    );
  }
}