package com.officemate.shared.exception;

/**
 * Exception thrown when wallet operations fail.
 * This includes wallet initialization, payment method errors, and transaction failures.
 */
public class WalletException extends RuntimeException {

    private final String errorCode;

    public WalletException(String message) {
        super(message);
        this.errorCode = "WALLET_ERROR";
    }

    public WalletException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public WalletException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "WALLET_ERROR";
    }

    public WalletException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
