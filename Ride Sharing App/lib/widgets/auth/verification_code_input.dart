import 'package:flutter/material.dart';

class VerificationCodeInput extends StatefulWidget {
  final int length;
  final ValueChanged<String> onCodeChanged;
  final ValueChanged<String>? onCodeCompleted;

  const VerificationCodeInput({
    super.key,
    this.length = 5,
    required this.onCodeChanged,
    this.onCodeCompleted,
  });

  @override
  State<VerificationCodeInput> createState() => _VerificationCodeInputState();
}

class _VerificationCodeInputState extends State<VerificationCodeInput> {
  final List<TextEditingController> _controllers = [];
  final List<FocusNode> _focusNodes = [];

  @override
  void initState() {
    super.initState();
    _controllers.addAll(List.generate(widget.length, (index) => TextEditingController()));
    _focusNodes.addAll(List.generate(widget.length, (index) => FocusNode()));
  }

  @override
  void dispose() {
    for (var controller in _controllers) {
      controller.dispose();
    }
    for (var focusNode in _focusNodes) {
      focusNode.dispose();
    }
    super.dispose();
  }

  void _onChanged(String value, int index) {
    if (value.isNotEmpty) {
      if (index < widget.length - 1) {
        _focusNodes[index + 1].requestFocus();
      } else {
        _focusNodes[index].unfocus();
        
        // Get complete code
        final completeCode = _controllers.map((c) => c.text).join();
        widget.onCodeCompleted?.call(completeCode);
      }
    } else if (value.isEmpty && index > 0) {
      _focusNodes[index - 1].requestFocus();
    }

    // Notify about code change
    final currentCode = _controllers.map((c) => c.text).join();
    widget.onCodeChanged(currentCode);
  }

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: List.generate(
        widget.length,
        (index) => _buildCodeField(index),
      ),
    );
  }

  Widget _buildCodeField(int index) {
    return SizedBox(
      width: 65,
      child: Container(
        margin: const EdgeInsets.symmetric(horizontal: 8),
        decoration: BoxDecoration(
          border: Border.all(color: Colors.black, width: 2),
          borderRadius: BorderRadius.circular(8),
        ),
        child: TextFormField(
          controller: _controllers[index],
          focusNode: _focusNodes[index],
          keyboardType: TextInputType.number,
          textAlign: TextAlign.center,
          style: const TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.bold,
            color: Colors.black,
          ),
          maxLength: 1,
          onChanged: (value) => _onChanged(value, index),
          decoration: const InputDecoration(
            border: InputBorder.none,
            counterText: "",
          ),
        ),
      ),
    );
  }
}