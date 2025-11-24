# Wallet Module

## Overview
The Wallet module handles payment wallet setup, management, and transaction processing for the corporate ride-sharing application.

## Responsibilities
- Payment wallet initialization and management
- Multiple payment method support (credit cards, bank accounts, UPI)
- Auto-reload functionality
- Driver earnings and withdrawal management
- QR code payment capabilities
- Payment method verification

## Package Structure
```
wallet/
├── entity/          # JPA entities (Wallet, PaymentMethod)
├── repository/      # Spring Data JPA repositories
├── service/         # Business logic for wallet and payment operations
└── controller/      # REST API endpoints for wallet management
```

## Key Components
- **WalletService**: Core wallet operations and balance management
- **PaymentMethodService**: Payment method management and verification
- **TransactionService**: Transaction processing and history

## Related Requirements
- Requirement 6.1: Wallet setup with verification requirement
- Requirement 6.2: Multiple payment method support
- Requirement 6.3: Payment method verification
- Requirement 6.4: Auto-reload functionality
- Requirement 6.5: Driver earnings and QR code payments
