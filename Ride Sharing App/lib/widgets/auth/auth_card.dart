import 'package:flutter/material.dart';

class AuthCard extends StatelessWidget {
  final List<Widget> children;
  final EdgeInsetsGeometry padding;

  const AuthCard({
    super.key,
    required this.children,
    this.padding = const EdgeInsets.all(24.0),
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 4,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      color: Colors.white,
      child: Padding(
        padding: padding,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: children,
        ),
      ),
    );
  }
}