import 'package:flutter/material.dart';

class NotificationIcon extends StatelessWidget {
  final VoidCallback onPressed;
  final bool showBadge;
  final Color badgeColor;
  final double iconSize;

  const NotificationIcon({
    super.key,
    required this.onPressed,
    this.showBadge = true,
    this.badgeColor = Colors.red,
    this.iconSize = 28,
  });

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        IconButton(
          icon: Icon(
            Icons.notifications_outlined,
            size: iconSize,
          ),
          onPressed: onPressed,
        ),
        if (showBadge)
          Positioned(
            right: 8,
            top: 8,
            child: Container(
              width: 12,
              height: 12,
              decoration: BoxDecoration(
                color: badgeColor,
                shape: BoxShape.circle,
              ),
            ),
          ),
      ],
    );
  }
}