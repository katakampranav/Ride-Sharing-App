# Wallet Controller Implementation

## Overview
The WalletController provides REST API endpoints for wallet management operations including wallet status retrieval, payment method management, auto-reload configuration, and bank account linking.

## Endpoints

### 1. Get Wallet Status
**GET** `/users/{userId}/wallet`

Retrieves the current wallet status including balance, payment methods, and configuration.

**Authorization:** Requires `EMAIL_VERIFIED` authority

**Response:** `WalletResponse`
```json
{
  "walletId": "uuid",
  "userId": "uuid",
  "balance": 1000.00,
  "paymentMethods": [
    {
      "methodId": "uuid",
      "methodType": "CREDIT_CARD",
      "maskedIdentifier": "**** **** **** 1234",
      "isPrimary": true,
      "isVerified": true,
      "createdAt": "2024-01-01T10:00:00"
    }
  ],
  "autoReloadEnabled": true,
  "autoReloadThreshold": 100.00,
  "autoReloadAmount": 500.00,
  "bankLinked": true,
  "createdAt": "2024-01-01T10:00:00",
  "updatedAt": "2024-01-01T10:00:00"
}
```

### 2. Add Payment Method
**POST** `/users/{userId}/wallet/payment-methods`

Adds a new payment method to the wallet. Supports credit cards, bank accounts, and UPI.

**Authorization:** Requires `EMAIL_VERIFIED` authority

**Request Body:** `WalletRequest`
```json
{
  "methodType": "CREDIT_CARD",
  "identifier": "4111111111111111",
  "isPrimary": true,
  "metadata": {
    "cardExpiry": "12/25",
    "cardHolderName": "John Doe"
  }
}
```

**Response:** `WalletResponse` (HTTP 201 Created)

**Supported Payment Method Types:**
- `CREDIT_CARD`: Credit/debit card (13-19 digits)
- `BANK_ACCOUNT`: Bank account number (5-20 alphanumeric characters)
- `UPI`: UPI ID (format: user@provider)

### 3. Configure Auto-Reload
**PUT** `/users/{userId}/wallet/auto-reload`

Enables or disables auto-reload functionality with specified threshold and amount.

**Authorization:** Requires `EMAIL_VERIFIED` authority

**Request Body:** `AutoReloadRequest`
```json
{
  "enabled": true,
  "threshold": 100.00,
  "amount": 500.00
}
```

**Response:** `WalletResponse`

**Behavior:**
- When `enabled` is `true`: Enables auto-reload with the specified threshold and amount
- When `enabled` is `false`: Disables auto-reload (threshold and amount are ignored)
- Requires at least one verified payment method to enable auto-reload

### 4. Link Bank Account
**POST** `/users/{userId}/wallet/bank-account`

Links a bank account to the wallet for driver earnings withdrawal and QR code payments.

**Authorization:** Requires `EMAIL_VERIFIED` authority

**Request Body:** `BankAccountRequest`
```json
{
  "accountNumber": "1234567890",
  "bankName": "State Bank of India",
  "ifscCode": "SBIN0001234"
}
```

**Response:** `WalletResponse` (HTTP 201 Created)

**Validation:**
- Account number: 5-20 alphanumeric characters
- Bank name: Required
- IFSC code: Optional, must match format `XXXX0XXXXXX` if provided (for Indian banks)

## Security

All endpoints require:
1. **Authentication**: User must be authenticated
2. **Email Verification**: User must have verified their corporate email (`EMAIL_VERIFIED` authority)
3. **Authorization**: Users can only access their own wallet (or admin role)

## Error Handling

The controller handles the following error scenarios:
- **400 Bad Request**: Invalid input data or validation failures
- **401 Unauthorized**: User not authenticated
- **403 Forbidden**: User not authorized or email not verified
- **404 Not Found**: Wallet not found for the user
- **409 Conflict**: Payment method already exists or wallet already exists

## Related Requirements

- **Requirement 6.1**: Wallet setup requiring mobile and email verification
- **Requirement 6.2**: Multiple payment method support
- **Requirement 6.3**: Payment method verification
- **Requirement 6.4**: Auto-reload functionality
- **Requirement 6.5**: Driver earnings and QR code payments

## Integration

The controller integrates with:
- **WalletService**: Core wallet business logic
- **Spring Security**: Authentication and authorization
- **Bean Validation**: Request validation

## Testing

To test the endpoints:
1. Ensure user has both mobile and email verified
2. Use valid JWT token with `EMAIL_VERIFIED` authority
3. Test each endpoint with valid and invalid data
4. Verify error handling and validation messages

## Example Usage

### Initialize Wallet (via service)
```bash
# First, wallet must be initialized via WalletService.initializeWallet()
# This is typically done after email verification
```

### Get Wallet Status
```bash
curl -X GET http://localhost:8080/users/{userId}/wallet \
  -H "Authorization: Bearer {jwt-token}"
```

### Add Credit Card
```bash
curl -X POST http://localhost:8080/users/{userId}/wallet/payment-methods \
  -H "Authorization: Bearer {jwt-token}" \
  -H "Content-Type: application/json" \
  -d '{
    "methodType": "CREDIT_CARD",
    "identifier": "4111111111111111",
    "isPrimary": true,
    "metadata": {
      "cardExpiry": "12/25"
    }
  }'
```

### Enable Auto-Reload
```bash
curl -X PUT http://localhost:8080/users/{userId}/wallet/auto-reload \
  -H "Authorization: Bearer {jwt-token}" \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": true,
    "threshold": 100.00,
    "amount": 500.00
  }'
```

### Link Bank Account
```bash
curl -X POST http://localhost:8080/users/{userId}/wallet/bank-account \
  -H "Authorization: Bearer {jwt-token}" \
  -H "Content-Type: application/json" \
  -d '{
    "accountNumber": "1234567890",
    "bankName": "State Bank of India",
    "ifscCode": "SBIN0001234"
  }'
```

## Notes

- Payment methods are initially unverified and require verification workflow
- Auto-reload requires at least one verified payment method
- Bank account linking creates a payment method of type `BANK_ACCOUNT`
- All monetary amounts use `BigDecimal` for precision
- Identifiers are masked in responses for security
