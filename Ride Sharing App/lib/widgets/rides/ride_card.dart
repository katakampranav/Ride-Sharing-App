import 'package:flutter/material.dart';

class RideCard extends StatelessWidget {
  final String fromAddress;
  final String toAddress;
  final String date;
  final String time;
  final String price;
  final Color fromDotColor;
  final Color toDotColor;
  final VoidCallback? onTap;

  const RideCard({
    super.key,
    required this.fromAddress,
    required this.toAddress,
    required this.date,
    required this.time,
    required this.price,
    this.fromDotColor = Colors.green,
    this.toDotColor = Colors.red,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(12),
          boxShadow: [
            BoxShadow(
              color: Colors.grey.withValues(alpha: 0.1),
              blurRadius: 4,
              offset: const Offset(0, 2),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _buildLocationRow('From', fromAddress, fromDotColor),
            const SizedBox(height: 12),
            _buildConnectingLine(),
            const SizedBox(height: 4),
            _buildLocationRow('To', toAddress, toDotColor),
            const SizedBox(height: 16),
            _buildMetaInfo(),
          ],
        ),
      ),
    );
  }

  Widget _buildLocationRow(String label, String address, Color dotColor) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Container(
              width: 8,
              height: 8,
              decoration: BoxDecoration(
                color: dotColor,
                shape: BoxShape.circle,
              ),
            ),
            const SizedBox(width: 12),
            Text(
              label,
              style: const TextStyle(fontSize: 14, color: Colors.black54),
            ),
          ],
        ),
        const SizedBox(height: 4),
        Padding(
          padding: const EdgeInsets.only(left: 20),
          child: Text(
            address,
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w600,
              color: Colors.black87,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildConnectingLine() {
    return Padding(
      padding: const EdgeInsets.only(left: 4),
      child: Container(
        width: 1,
        height: 20,
        color: Colors.grey.shade400,
      ),
    );
  }

  Widget _buildMetaInfo() {
    return Row(
      children: [
        Icon(Icons.calendar_today, size: 16, color: Colors.grey.shade600),
        const SizedBox(width: 4),
        Text(
          date,
          style: TextStyle(fontSize: 14, color: Colors.grey.shade600),
        ),
        const SizedBox(width: 24),
        Icon(Icons.access_time, size: 16, color: Colors.grey.shade600),
        const SizedBox(width: 4),
        Text(
          time,
          style: TextStyle(fontSize: 14, color: Colors.grey.shade600),
        ),
        const Spacer(),
        Text(
          price,
          style: const TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.bold,
            color: Colors.black87,
          ),
        ),
      ],
    );
  }
}