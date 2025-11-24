import 'package:flutter/material.dart';

class RouteDetailsCard extends StatelessWidget {
  final String fromAddress;
  final String toAddress;
  final String? date;
  final String? time;
  final String? price;
  final bool showDateAndTime;
  final bool showPrice;

  const RouteDetailsCard({
    super.key,
    required this.fromAddress,
    required this.toAddress,
    this.date,
    this.time,
    this.price,
    this.showDateAndTime = true,
    this.showPrice = true,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
            color: Colors.grey.withValues(alpha: 0.08),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Column(
                  children: [
                    _dot(Colors.green),
                    Container(
                      width: 2,
                      height: 50,
                      color: Colors.grey.shade300,
                    ),
                    _dot(Colors.red),
                  ],
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'From',
                        style: TextStyle(color: Colors.black54),
                      ),
                      Text(
                        fromAddress,
                        style: const TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: 12),
                      const Text(
                        'To',
                        style: TextStyle(color: Colors.black54),
                      ),
                      Text(
                        toAddress,
                        style: const TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            
            if (showDateAndTime && date != null && time != null) ...[
              const SizedBox(height: 16),
              Row(
                children: [
                  const Icon(Icons.event, size: 18, color: Colors.black54),
                  const SizedBox(width: 6),
                  Text(date!),
                  const SizedBox(width: 16),
                  const Spacer(),
                  const Icon(Icons.access_time, size: 18, color: Colors.black54),
                  const SizedBox(width: 6),
                  Text(time!),
                ],
              ),
            ],
            
            if (showPrice && price != null) ...[
              const SizedBox(height: 16),
              Container(
                decoration: BoxDecoration(
                  color: const Color(0xFFEFF6FF),
                  borderRadius: BorderRadius.circular(12),
                ),
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Row(
                      children: [
                        const Expanded(
                          child: Text(
                            'Ride fare',
                            style: TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                        Text(
                          price!,
                          style: const TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    const Text(
                      'Corporate account will be charged after the ride',
                      style: TextStyle(color: Colors.black87),
                    ),
                  ],
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _dot(Color color) {
    return Container(
      width: 8,
      height: 8,
      decoration: BoxDecoration(color: color, shape: BoxShape.circle),
    );
  }
}