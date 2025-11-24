import 'package:flutter/material.dart';

class AuthTabSelector extends StatefulWidget {
  final bool initialIsRiderSelected;
  final ValueChanged<bool> onTabChanged;

  const AuthTabSelector({
    super.key,
    required this.initialIsRiderSelected,
    required this.onTabChanged,
  });

  @override
  State<AuthTabSelector> createState() => _AuthTabSelectorState();
}

class _AuthTabSelectorState extends State<AuthTabSelector> {
  late bool isRiderSelected;

  @override
  void initState() {
    super.initState();
    isRiderSelected = widget.initialIsRiderSelected;
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Row(
          children: [
            _buildTab('Rider', isRiderSelected),
            const SizedBox(width: 24),
            _buildTab('Driver', !isRiderSelected),
          ],
        ),
        const Divider(height: 2, color: Color(0xFFE0E0E0)),
      ],
    );
  }

  Widget _buildTab(String text, bool isSelected) {
    return GestureDetector(
      onTap: () {
        setState(() {
          isRiderSelected = (text == 'Rider');
          widget.onTabChanged(isRiderSelected);
        });
      },
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 12.0),
        decoration: BoxDecoration(
          border: Border(
            bottom: BorderSide(
              color: isSelected ? const Color(0xFF2563EB) : Colors.transparent,
              width: 3.0,
            ),
          ),
        ),
        child: Text(
          text,
          style: TextStyle(
            fontSize: 16,
            fontWeight: isSelected ? FontWeight.w600 : FontWeight.normal,
            color: isSelected ? const Color(0xFF2563EB) : Colors.black54,
          ),
        ),
      ),
    );
  }
}