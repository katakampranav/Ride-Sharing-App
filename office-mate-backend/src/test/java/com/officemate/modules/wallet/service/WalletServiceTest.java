package com.officemate.modules.wallet.service;

import com.officemate.modules.auth.entity.UserAccount;
import com.officemate.modules.auth.repository.UserAccountRepository;
import com.officemate.modules.wallet.entity.PaymentMethod;
import com.officemate.modules.wallet.entity.Wallet;
import com.officemate.modules.wallet.repository.PaymentMethodRepository;
import com.officemate.modules.wallet.repository.WalletRepository;
import com.officemate.shared.dto.WalletRequest;
import com.officemate.shared.dto.WalletResponse;
import com.officemate.shared.enums.AccountStatus;
import com.officemate.shared.exception.ProfileAccessException;
import com.officemate.shared.exception.WalletException;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WalletService.
 * Tests wallet initialization, payment method management, auto-reload configuration,
 * and verification requirements.
 */
@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private WalletService walletService;

    private UUID testUserId;
    private UserAccount fullyVerifiedAccount;
    private UserAccount mobileOnlyAccount;
    private Wallet testWallet;
    private PaymentMethod testPaymentMethod;

    @BeforeEach
    void setUp() {
        testUserId = UUID.randomUUID();

        // Fully verified account
        fullyVerifiedAccount = UserAccount.builder()
            .userId(testUserId)
            .phoneNumber("+1234567890")
            .phoneVerified(true)
            .corporateEmail("user@company.com")
            .emailVerified(true)
            .accountStatus(AccountStatus.ACTIVE)
            .build();

        // Mobile only verified account
        mobileOnlyAccount = UserAccount.builder()
            .userId(testUserId)
            .phoneNumber("+1234567890")
            .phoneVerified(true)
            .emailVerified(false)
            .accountStatus(AccountStatus.PENDING_EMAIL)
            .build();

        // Test wallet
        testWallet = Wallet.builder()
            .walletId(UUID.randomUUID())
            .userAccount(fullyVerifiedAccount)
            .balance(BigDecimal.valueOf(100.00))
            .autoReloadEnabled(false)
            .bankLinked(false)
            .paymentMethods(new ArrayList<>())
            .build();

        // Test payment method
        testPaymentMethod = PaymentMethod.builder()
            .methodId(UUID.randomUUID())
            .wallet(testWallet)
            .methodType("CREDIT_CARD")
            .identifier("1234567890123456")
            .isPrimary(true)
            .isVerified(true)
            .metadata(new HashMap<>())
            .build();
    }

    // ========== Wallet Initialization Tests ==========

    @Test
    void testInitializeWallet_FullyVerified_Success() {
        // Arrange
        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(fullyVerifiedAccount));
        when(walletRepository.existsByUserAccountUserId(testUserId)).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        // Act
        WalletResponse response = walletService.initializeWallet(testUserId);

        // Assert
        assertNotNull(response);
        assertEquals(testWallet.getWalletId().toString(), response.getWalletId());
        assertEquals(testUserId.toString(), response.getUserId());
        assertEquals(BigDecimal.valueOf(100.00), response.getBalance());
        assertFalse(response.isAutoReloadEnabled());
        assertFalse(response.isBankLinked());
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void testInitializeWallet_NotFullyVerified_ThrowsException() {
        // Arrange
        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(mobileOnlyAccount));

        // Act & Assert
        ProfileAccessException exception = assertThrows(
            ProfileAccessException.class,
            () -> walletService.initializeWallet(testUserId)
        );

        assertTrue(exception.getMessage().contains("Both mobile and email verification required"));
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void testInitializeWallet_AlreadyExists_ThrowsException() {
        // Arrange
        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.of(fullyVerifiedAccount));
        when(walletRepository.existsByUserAccountUserId(testUserId)).thenReturn(true);

        // Act & Assert
        WalletException exception = assertThrows(
            WalletException.class,
            () -> walletService.initializeWallet(testUserId)
        );

        assertTrue(exception.getMessage().contains("Wallet already exists"));
        assertEquals("WALLET_ALREADY_EXISTS", exception.getErrorCode());
        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void testInitializeWallet_UserNotFound_ThrowsException() {
        // Arrange
        when(userAccountRepository.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            EntityNotFoundException.class,
            () -> walletService.initializeWallet(testUserId)
        );
    }

    // ========== Payment Method Management Tests ==========

    @Test
    void testAddPaymentMethod_CreditCard_Success() {
        // Arrange
        WalletRequest request = WalletRequest.builder()
            .methodType("CREDIT_CARD")
            .identifier("1234567890123456")
            .isPrimary(true)
            .metadata(Map.of("cardExpiry", "12/25"))
            .build();

        PaymentMethod newPaymentMethod = PaymentMethod.builder()
            .methodId(UUID.randomUUID())
            .wallet(testWallet)
            .methodType("CREDIT_CARD")
            .identifier("1234567890123456")
            .isPrimary(true)
            .isVerified(false)
            .metadata(Map.of("cardExpiry", "12/25"))
            .build();

        Wallet savedWallet = Wallet.builder()
            .walletId(testWallet.getWalletId())
            .userAccount(fullyVerifiedAccount)
            .balance(testWallet.getBalance())
            .autoReloadEnabled(false)
            .bankLinked(false)
            .paymentMethods(new ArrayList<>(List.of(newPaymentMethod)))
            .build();

        when(walletRepository.findByUserAccountUserIdWithPaymentMethods(testUserId))
            .thenReturn(Optional.of(testWallet));
        when(paymentMethodRepository.existsByWalletWalletIdAndIdentifier(any(), any())).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenReturn(savedWallet);

        // Act
        WalletResponse response = walletService.addPaymentMethod(testUserId, request);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getPaymentMethods().size());
        verify(paymentMethodRepository).unmarkAllAsPrimaryForWallet(testWallet.getWalletId());
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void testAddPaymentMethod_BankAccount_Success() {
        // Arrange
        WalletRequest request = WalletRequest.builder()
            .methodType("BANK_ACCOUNT")
            .identifier("ACCT123456789")
            .isPrimary(false)
            .metadata(Map.of("bankName", "Test Bank"))
            .build();

        PaymentMethod newPaymentMethod = PaymentMethod.builder()
            .methodId(UUID.randomUUID())
            .wallet(testWallet)
            .methodType("BANK_ACCOUNT")
            .identifier("ACCT123456789")
            .isPrimary(false)
            .isVerified(false)
            .metadata(Map.of("bankName", "Test Bank"))
            .build();

        Wallet savedWallet = Wallet.builder()
            .walletId(testWallet.getWalletId())
            .userAccount(fullyVerifiedAccount)
            .balance(testWallet.getBalance())
            .autoReloadEnabled(false)
            .bankLinked(false)
            .paymentMethods(new ArrayList<>(List.of(newPaymentMethod)))
            .build();

        when(walletRepository.findByUserAccountUserIdWithPaymentMethods(testUserId))
            .thenReturn(Optional.of(testWallet));
        when(paymentMethodRepository.existsByWalletWalletIdAndIdentifier(any(), any())).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenReturn(savedWallet);

        // Act
        WalletResponse response = walletService.addPaymentMethod(testUserId, request);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getPaymentMethods().size());
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void testAddPaymentMethod_UPI_Success() {
        // Arrange
        WalletRequest request = WalletRequest.builder()
            .methodType("UPI")
            .identifier("user@upi")
            .isPrimary(false)
            .build();

        PaymentMethod newPaymentMethod = PaymentMethod.builder()
            .methodId(UUID.randomUUID())
            .wallet(testWallet)
            .methodType("UPI")
            .identifier("user@upi")
            .isPrimary(false)
            .isVerified(false)
            .metadata(new HashMap<>())
            .build();

        Wallet savedWallet = Wallet.builder()
            .walletId(testWallet.getWalletId())
            .userAccount(fullyVerifiedAccount)
            .balance(testWallet.getBalance())
            .autoReloadEnabled(false)
            .bankLinked(false)
            .paymentMethods(new ArrayList<>(List.of(newPaymentMethod)))
            .build();

        when(walletRepository.findByUserAccountUserIdWithPaymentMethods(testUserId))
            .thenReturn(Optional.of(testWallet));
        when(paymentMethodRepository.existsByWalletWalletIdAndIdentifier(any(), any())).thenReturn(false);
        when(walletRepository.save(any(Wallet.class))).thenReturn(savedWallet);

        // Act
        WalletResponse response = walletService.addPaymentMethod(testUserId, request);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getPaymentMethods().size());
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void testAddPaymentMethod_InvalidType_ThrowsException() {
        // Arrange
        WalletRequest request = WalletRequest.builder()
            .methodType("INVALID_TYPE")
            .identifier("1234567890123456")
            .build();

        when(walletRepository.findByUserAccountUserIdWithPaymentMethods(testUserId))
            .thenReturn(Optional.of(testWallet));

        // Act & Assert
        WalletException exception = assertThrows(
            WalletException.class,
            () -> walletService.addPaymentMethod(testUserId, request)
        );

        assertTrue(exception.getMessage().contains("Invalid payment method type"));
        assertEquals("INVALID_METHOD_TYPE", exception.getErrorCode());
    }

    @Test
    void testAddPaymentMethod_InvalidCreditCardFormat_ThrowsException() {
        // Arrange
        WalletRequest request = WalletRequest.builder()
            .methodType("CREDIT_CARD")
            .identifier("123") // Too short
            .build();

        when(walletRepository.findByUserAccountUserIdWithPaymentMethods(testUserId))
            .thenReturn(Optional.of(testWallet));

        // Act & Assert
        WalletException exception = assertThrows(
            WalletException.class,
            () -> walletService.addPaymentMethod(testUserId, request)
        );

        assertTrue(exception.getMessage().contains("Invalid credit card number format"));
        assertEquals("INVALID_IDENTIFIER", exception.getErrorCode());
    }

    @Test
    void testAddPaymentMethod_InvalidUPIFormat_ThrowsException() {
        // Arrange
        WalletRequest request = WalletRequest.builder()
            .methodType("UPI")
            .identifier("invalidupi") // Missing @
            .build();

        when(walletRepository.findByUserAccountUserIdWithPaymentMethods(testUserId))
            .thenReturn(Optional.of(testWallet));

        // Act & Assert
        WalletException exception = assertThrows(
            WalletException.class,
            () -> walletService.addPaymentMethod(testUserId, request)
        );

        assertTrue(exception.getMessage().contains("Invalid UPI ID format"));
        assertEquals("INVALID_IDENTIFIER", exception.getErrorCode());
    }

    @Test
    void testAddPaymentMethod_Duplicate_ThrowsException() {
        // Arrange
        WalletRequest request = WalletRequest.builder()
            .methodType("CREDIT_CARD")
            .identifier("1234567890123456")
            .build();

        when(walletRepository.findByUserAccountUserIdWithPaymentMethods(testUserId))
            .thenReturn(Optional.of(testWallet));
        when(paymentMethodRepository.existsByWalletWalletIdAndIdentifier(any(), any())).thenReturn(true);

        // Act & Assert
        WalletException exception = assertThrows(
            WalletException.class,
            () -> walletService.addPaymentMethod(testUserId, request)
        );

        assertTrue(exception.getMessage().contains("Payment method already exists"));
        assertEquals("PAYMENT_METHOD_DUPLICATE", exception.getErrorCode());
    }

    // ========== Auto-Reload Tests ==========

    @Test
    void testEnableAutoReload_Success() {
        // Arrange
        testWallet.getPaymentMethods().add(testPaymentMethod);
        BigDecimal threshold = BigDecimal.valueOf(50.00);
        BigDecimal amount = BigDecimal.valueOf(100.00);

        when(walletRepository.findByUserAccountUserIdWithPaymentMethods(testUserId))
            .thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        // Act
        WalletResponse response = walletService.enableAutoReload(testUserId, threshold, amount);

        // Assert
        assertNotNull(response);
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void testEnableAutoReload_NoVerifiedPaymentMethod_ThrowsException() {
        // Arrange
        testPaymentMethod.setIsVerified(false);
        testWallet.getPaymentMethods().add(testPaymentMethod);
        BigDecimal threshold = BigDecimal.valueOf(50.00);
        BigDecimal amount = BigDecimal.valueOf(100.00);

        when(walletRepository.findByUserAccountUserIdWithPaymentMethods(testUserId))
            .thenReturn(Optional.of(testWallet));

        // Act & Assert
        WalletException exception = assertThrows(
            WalletException.class,
            () -> walletService.enableAutoReload(testUserId, threshold, amount)
        );

        assertTrue(exception.getMessage().contains("At least one verified payment method required"));
        assertEquals("NO_VERIFIED_PAYMENT_METHOD", exception.getErrorCode());
    }

    @Test
    void testEnableAutoReload_InvalidThreshold_ThrowsException() {
        // Arrange
        testWallet.getPaymentMethods().add(testPaymentMethod);
        BigDecimal threshold = BigDecimal.ZERO;
        BigDecimal amount = BigDecimal.valueOf(100.00);

        when(walletRepository.findByUserAccountUserIdWithPaymentMethods(testUserId))
            .thenReturn(Optional.of(testWallet));

        // Act & Assert
        WalletException exception = assertThrows(
            WalletException.class,
            () -> walletService.enableAutoReload(testUserId, threshold, amount)
        );

        assertTrue(exception.getMessage().contains("Auto-reload threshold must be greater than 0"));
        assertEquals("INVALID_THRESHOLD", exception.getErrorCode());
    }

    @Test
    void testEnableAutoReload_InvalidAmount_ThrowsException() {
        // Arrange
        testWallet.getPaymentMethods().add(testPaymentMethod);
        BigDecimal threshold = BigDecimal.valueOf(50.00);
        BigDecimal amount = BigDecimal.ZERO;

        when(walletRepository.findByUserAccountUserIdWithPaymentMethods(testUserId))
            .thenReturn(Optional.of(testWallet));

        // Act & Assert
        WalletException exception = assertThrows(
            WalletException.class,
            () -> walletService.enableAutoReload(testUserId, threshold, amount)
        );

        assertTrue(exception.getMessage().contains("Auto-reload amount must be greater than 0"));
        assertEquals("INVALID_AMOUNT", exception.getErrorCode());
    }

    @Test
    void testDisableAutoReload_Success() {
        // Arrange
        testWallet.setAutoReloadEnabled(true);
        when(walletRepository.findByUserAccountUserId(testUserId)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        // Act
        WalletResponse response = walletService.disableAutoReload(testUserId);

        // Assert
        assertNotNull(response);
        verify(walletRepository).save(any(Wallet.class));
    }

    // ========== Bank Account Linking Tests ==========

    @Test
    void testLinkBankAccount_Success() {
        // Arrange
        PaymentMethod bankPaymentMethod = PaymentMethod.builder()
            .methodId(UUID.randomUUID())
            .wallet(testWallet)
            .methodType("BANK_ACCOUNT")
            .identifier("ACCT123456789")
            .isPrimary(false)
            .isVerified(false)
            .metadata(Map.of("bankName", "Test Bank", "ifscCode", "IFSC0001234"))
            .build();

        Wallet savedWallet = Wallet.builder()
            .walletId(testWallet.getWalletId())
            .userAccount(fullyVerifiedAccount)
            .balance(testWallet.getBalance())
            .autoReloadEnabled(false)
            .bankLinked(true)
            .paymentMethods(new ArrayList<>(List.of(bankPaymentMethod)))
            .build();

        when(walletRepository.findByUserAccountUserId(testUserId)).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(savedWallet);

        // Act
        WalletResponse response = walletService.linkBankAccount(
            testUserId,
            "ACCT123456789",
            "Test Bank",
            "IFSC0001234"
        );

        // Assert
        assertNotNull(response);
        assertTrue(response.isBankLinked());
        assertEquals(1, response.getPaymentMethods().size());
        verify(walletRepository).save(any(Wallet.class));
    }

    // ========== Wallet Status Tests ==========

    @Test
    void testGetWalletStatus_Success() {
        // Arrange
        testWallet.getPaymentMethods().add(testPaymentMethod);
        when(walletRepository.findByUserAccountUserIdWithPaymentMethods(testUserId))
            .thenReturn(Optional.of(testWallet));

        // Act
        WalletResponse response = walletService.getWalletStatus(testUserId);

        // Assert
        assertNotNull(response);
        assertEquals(testWallet.getWalletId().toString(), response.getWalletId());
        assertEquals(testUserId.toString(), response.getUserId());
        assertEquals(1, response.getPaymentMethods().size());
    }

    @Test
    void testGetWalletStatus_NotFound_ThrowsException() {
        // Arrange
        when(walletRepository.findByUserAccountUserIdWithPaymentMethods(testUserId))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            EntityNotFoundException.class,
            () -> walletService.getWalletStatus(testUserId)
        );
    }

    // ========== Payment Method Verification Tests ==========

    @Test
    void testVerifyPaymentMethod_Success() {
        // Arrange
        testPaymentMethod.setIsVerified(false);
        testWallet.getPaymentMethods().add(testPaymentMethod);
        when(walletRepository.findByUserAccountUserIdWithPaymentMethods(testUserId))
            .thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        // Act
        WalletResponse response = walletService.verifyPaymentMethod(testUserId, testPaymentMethod.getMethodId());

        // Assert
        assertNotNull(response);
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void testVerifyPaymentMethod_NotFound_ThrowsException() {
        // Arrange
        UUID nonExistentMethodId = UUID.randomUUID();
        testWallet.getPaymentMethods().add(testPaymentMethod);
        when(walletRepository.findByUserAccountUserIdWithPaymentMethods(testUserId))
            .thenReturn(Optional.of(testWallet));

        // Act & Assert
        assertThrows(
            EntityNotFoundException.class,
            () -> walletService.verifyPaymentMethod(testUserId, nonExistentMethodId)
        );
    }

    // ========== Primary Payment Method Tests ==========

    @Test
    void testSetPrimaryPaymentMethod_Success() {
        // Arrange
        testWallet.getPaymentMethods().add(testPaymentMethod);
        when(walletRepository.findByUserAccountUserIdWithPaymentMethods(testUserId))
            .thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        // Act
        WalletResponse response = walletService.setPrimaryPaymentMethod(testUserId, testPaymentMethod.getMethodId());

        // Assert
        assertNotNull(response);
        verify(paymentMethodRepository).unmarkAllAsPrimaryForWallet(testWallet.getWalletId());
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void testSetPrimaryPaymentMethod_NotFound_ThrowsException() {
        // Arrange
        UUID nonExistentMethodId = UUID.randomUUID();
        testWallet.getPaymentMethods().add(testPaymentMethod);
        when(walletRepository.findByUserAccountUserIdWithPaymentMethods(testUserId))
            .thenReturn(Optional.of(testWallet));

        // Act & Assert
        assertThrows(
            EntityNotFoundException.class,
            () -> walletService.setPrimaryPaymentMethod(testUserId, nonExistentMethodId)
        );
    }
}
