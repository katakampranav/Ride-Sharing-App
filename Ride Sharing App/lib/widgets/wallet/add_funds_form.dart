import 'package:flutter/material.dart';
import 'package:my_app/widgets/auth/auth_button.dart';
import 'package:my_app/widgets/auth/auth_text_field.dart';

class AddFundsForm extends StatefulWidget {
  final Function(String amount, String method) onAddFunds;

  const AddFundsForm({
    super.key,
    required this.onAddFunds,
  });

  @override
  State<AddFundsForm> createState() => _AddFundsFormState();
}

class _AddFundsFormState extends State<AddFundsForm> {
  final TextEditingController _amountController = TextEditingController();
  String _selectedMethod = "Corporate Account";

  @override
  void dispose() {
    _amountController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: ConstrainedBox(
        constraints: BoxConstraints(
          minHeight: MediaQuery.of(context).size.height - 300,
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            AuthTextField(
              label: "Amount",
              hintText: "Enter amount",
              keyboardType: TextInputType.number,
              controller: _amountController,
            ),
            const SizedBox(height: 20),

            const Text(
              "Payment Method",
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w600,
                color: Colors.black87,
              ),
            ),
            const SizedBox(height: 8),

            // Payment options
            Card(
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
              child: Column(
                children: [
                  RadioListTile<String>(
                    value: "Corporate Account",
                    groupValue: _selectedMethod,
                    onChanged: (value) {
                      setState(() => _selectedMethod = value!);
                    },
                    title: const Text("Corporate Account"),
                    secondary: const Icon(
                      Icons.credit_card,
                      color: Colors.blue,
                    ),
                  ),
                  const Divider(height: 1),
                  RadioListTile<String>(
                    value: "UPI",
                    groupValue: _selectedMethod,
                    onChanged: (value) {
                      setState(() => _selectedMethod = value!);
                    },
                    title: const Text("UPI"),
                    secondary: const Icon(
                      Icons.account_balance_wallet,
                      color: Colors.green,
                    ),
                  ),
                ],
              ),
            ),

            const SizedBox(height: 20),

            // Add Funds button
            AuthButton(
              text: "Add Funds",
              onPressed: () {
                if (_amountController.text.isEmpty) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('Please enter an amount'),
                    ),
                  );
                  return;
                }
                widget.onAddFunds(_amountController.text, _selectedMethod);
              },
              isPrimary: true,
            ),

            SizedBox(height: MediaQuery.of(context).viewInsets.bottom > 0 ? 20 : 0),
          ],
        ),
      ),
    );
  }
}