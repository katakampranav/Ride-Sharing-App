import 'package:flutter/material.dart';

class DriverInfoHeader extends StatelessWidget {
  final String avatarUrl;
  final String name;
  final String role;
  final VoidCallback? onMessage;
  final VoidCallback? onCall;

  const DriverInfoHeader({
    super.key,
    required this.avatarUrl,
    required this.name,
    required this.role,
    this.onMessage,
    this.onCall,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        CircleAvatar(
          radius: 24,
          backgroundImage: NetworkImage(avatarUrl),
          child: avatarUrl.isEmpty
              ? const Icon(Icons.person, color: Colors.black54)
              : null,
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                name,
                style: const TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w700,
                ),
              ),
              Text(
                role,
                style: const TextStyle(color: Colors.black54),
              ),
            ],
          ),
        ),
        const SizedBox(width: 12),
        Row(
          children: [
            Container(
              decoration: BoxDecoration(
                color: Colors.grey[50],
                borderRadius: BorderRadius.circular(100),
                border: Border.all(color: Colors.grey[300]!),
              ),
              child: IconButton(
                onPressed: onMessage,
                icon: const Icon(
                  Icons.message_rounded,
                  color: Colors.blue,
                ),
              ),
            ),
            const SizedBox(width: 8),
            Container(
              decoration: BoxDecoration(
                color: Colors.grey[50],
                borderRadius: BorderRadius.circular(100),
                border: Border.all(color: Colors.grey[300]!),
              ),
              child: IconButton(
                onPressed: onCall,
                icon: const Icon(Icons.call, color: Colors.green),
              ),
            ),
          ],
        ),
      ],
    );
  }
}