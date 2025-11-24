import 'package:flutter/material.dart';
import 'package:my_app/widgets/auth/auth_button.dart';

class BalanceCard extends StatelessWidget {
  final double balance;
  final VoidCallback onAddFundsPressed;

  const BalanceCard({
    super.key,
    required this.balance,
    required this.onAddFundsPressed,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(24),
      color: Colors.white,
      child: Column(
        children: [
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Colors.blue[100],
              borderRadius: BorderRadius.circular(100),
              border: Border.all(color: Colors.grey[300]!),
            ),
            child: const Icon(
              Icons.account_balance_wallet_rounded,
              size: 48,
              color: Colors.blue,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            '\$ ${balance.toStringAsFixed(2)}',
            style: const TextStyle(
              fontSize: 36,
              fontWeight: FontWeight.bold,
              color: Colors.black87,
            ),
          ),
          const SizedBox(height: 8),
          const Text(
            'Available Balance',
            style: TextStyle(fontSize: 16, color: Colors.black54),
          ),
          const SizedBox(height: 10),
          SizedBox(
            width: double.infinity,
            child: AuthButton(
              text: "Add Funds",
              onPressed: onAddFundsPressed,
              isPrimary: true,
            ),
          ),
        ],
      ),
    );
  }
}
