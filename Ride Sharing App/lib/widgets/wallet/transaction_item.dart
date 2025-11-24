import 'package:flutter/material.dart';

class TransactionItem extends StatelessWidget {
  final String title;
  final String date;
  final String time;
  final double amount;
  final String type;

  const TransactionItem({
    super.key,
    required this.title,
    required this.date,
    required this.time,
    required this.amount,
    required this.type,
  });

  @override
  Widget build(BuildContext context) {
    final isPositive = amount > 0;

    // Pick icon based on type
    IconData icon;
    Color iconColor;
    switch (type) {
      case 'ride':
        icon = Icons.local_taxi;
        iconColor = Colors.blue;
        break;
      case 'topup':
        icon = Icons.add_card;
        iconColor = Colors.green;
        break;
      case 'fee':
        icon = Icons.warning_amber_rounded;
        iconColor = Colors.redAccent;
        break;
      default:
        icon = Icons.receipt_long;
        iconColor = Colors.grey;
    }

    return Card(
      color: Colors.white,
      margin: const EdgeInsets.symmetric(
        horizontal: 12,
        vertical: 6,
      ),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
      ),
      elevation: 2,
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: iconColor.withOpacity(0.2),
          child: Icon(icon, color: iconColor),
        ),
        title: Text(
          title,
          style: const TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w600,
            color: Colors.black87,
          ),
        ),
        subtitle: Text(
          "$date • $time",
          style: const TextStyle(
            fontSize: 14,
            color: Colors.black54,
          ),
        ),
        trailing: Text(
          "${isPositive ? '+' : '-'}\$${amount.abs().toStringAsFixed(2)}",
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.bold,
            color: isPositive ? Colors.green : Colors.red,
          ),
        ),
      ),
    );
  }
}