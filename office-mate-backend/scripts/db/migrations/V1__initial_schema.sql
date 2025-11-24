-- Initial database schema for Officemate application
-- Version: 1.0.0
-- Description: Creates core tables for user authentication, profiles, and related entities

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- User Accounts Table
-- ============================================
CREATE TABLE user_accounts (
    user_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    phone_number VARCHAR(20) UNIQUE NOT NULL,
    phone_verified BOOLEAN DEFAULT FALSE,
    corporate_email VARCHAR(255) UNIQUE,
    email_verified BOOLEAN DEFAULT FALSE,
    account_status VARCHAR(20) DEFAULT 'PENDING_EMAIL',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_accounts_phone ON user_accounts(phone_number);
CREATE INDEX idx_user_accounts_email ON user_accounts(corporate_email);
CREATE INDEX idx_user_accounts_status ON user_accounts(account_status);

-- ============================================
-- User Profiles Table
-- ============================================
CREATE TABLE user_profiles (
    user_id UUID PRIMARY KEY REFERENCES user_accounts(user_id) ON DELETE CASCADE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    profile_image_url VARCHAR(500),
    date_of_birth DATE,
    gender VARCHAR(10),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- Driver Profiles Table
-- ============================================
CREATE TABLE driver_profiles (
    driver_id UUID PRIMARY KEY REFERENCES user_profiles(user_id) ON DELETE CASCADE,
    license_number VARCHAR(50) NOT NULL,
    license_expiry DATE NOT NULL,
    license_verified BOOLEAN DEFAULT FALSE,
    max_detour_distance INTEGER DEFAULT 500,
    vehicle_type VARCHAR(20) NOT NULL,
    vehicle_make VARCHAR(50),
    vehicle_model VARCHAR(50),
    vehicle_year INTEGER,
    license_plate VARCHAR(20),
    vehicle_capacity INTEGER,
    fuel_type VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_driver_profiles_vehicle_type ON driver_profiles(vehicle_type);
CREATE INDEX idx_driver_profiles_verified ON driver_profiles(license_verified);

-- ============================================
-- Rider Profiles Table
-- ============================================
CREATE TABLE rider_profiles (
    rider_id UUID PRIMARY KEY REFERENCES user_profiles(user_id) ON DELETE CASCADE,
    gender_preference VARCHAR(20),
    vehicle_type_preferences TEXT[],
    favorite_drivers TEXT[],
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- Email Verifications Table
-- ============================================
CREATE TABLE email_verifications (
    verification_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES user_accounts(user_id) ON DELETE CASCADE,
    corporate_email VARCHAR(255) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    attempts INTEGER DEFAULT 0,
    verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP DEFAULT (CURRENT_TIMESTAMP + INTERVAL '10 minutes')
);

CREATE INDEX idx_email_verifications_user ON email_verifications(user_id);
CREATE INDEX idx_email_verifications_expires ON email_verifications(expires_at);
CREATE INDEX idx_email_verifications_email ON email_verifications(corporate_email);

-- ============================================
-- Wallets Table
-- ============================================
CREATE TABLE wallets (
    wallet_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID UNIQUE REFERENCES user_accounts(user_id) ON DELETE CASCADE,
    balance DECIMAL(10,2) DEFAULT 0.00,
    auto_reload_enabled BOOLEAN DEFAULT FALSE,
    auto_reload_threshold DECIMAL(10,2),
    auto_reload_amount DECIMAL(10,2),
    bank_linked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_wallets_user ON wallets(user_id);

-- ============================================
-- Payment Methods Table
-- ============================================
CREATE TABLE payment_methods (
    method_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    wallet_id UUID REFERENCES wallets(wallet_id) ON DELETE CASCADE,
    method_type VARCHAR(20) NOT NULL,
    identifier VARCHAR(255) NOT NULL,
    is_primary BOOLEAN DEFAULT FALSE,
    is_verified BOOLEAN DEFAULT FALSE,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payment_methods_wallet ON payment_methods(wallet_id);
CREATE INDEX idx_payment_methods_type ON payment_methods(method_type);

-- ============================================
-- Emergency Contacts Table
-- ============================================
CREATE TABLE emergency_contacts (
    contact_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES user_accounts(user_id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    relationship VARCHAR(50),
    is_primary BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_emergency_contacts_user ON emergency_contacts(user_id);

-- ============================================
-- Family Sharing Contacts Table
-- ============================================
CREATE TABLE family_sharing_contacts (
    sharing_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES user_accounts(user_id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20),
    email VARCHAR(255),
    receive_ride_updates BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_family_sharing_user ON family_sharing_contacts(user_id);

-- ============================================
-- Audit Logs Table
-- ============================================
CREATE TABLE audit_logs (
    log_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES user_accounts(user_id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id VARCHAR(255),
    details JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at);

-- ============================================
-- Email Change Audit Logs Table
-- ============================================
CREATE TABLE email_change_audit_logs (
    log_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES user_accounts(user_id) ON DELETE CASCADE,
    old_email VARCHAR(255),
    new_email VARCHAR(255),
    change_reason VARCHAR(255),
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_change_logs_user ON email_change_audit_logs(user_id);
CREATE INDEX idx_email_change_logs_created ON email_change_audit_logs(created_at);

-- ============================================
-- User Session Metadata Table
-- ============================================
CREATE TABLE user_session_metadata (
    session_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES user_accounts(user_id) ON DELETE CASCADE,
    device_type VARCHAR(20),
    device_id VARCHAR(255),
    app_version VARCHAR(20),
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);

CREATE INDEX idx_session_metadata_user ON user_session_metadata(user_id);
CREATE INDEX idx_session_metadata_expires ON user_session_metadata(expires_at);

-- ============================================
-- Triggers for updated_at timestamps
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_user_accounts_updated_at BEFORE UPDATE ON user_accounts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_profiles_updated_at BEFORE UPDATE ON user_profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_driver_profiles_updated_at BEFORE UPDATE ON driver_profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_rider_profiles_updated_at BEFORE UPDATE ON rider_profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_wallets_updated_at BEFORE UPDATE ON wallets
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- Comments for documentation
-- ============================================
COMMENT ON TABLE user_accounts IS 'Core user authentication and account information';
COMMENT ON TABLE user_profiles IS 'Basic user profile information';
COMMENT ON TABLE driver_profiles IS 'Driver-specific profile data including vehicle information';
COMMENT ON TABLE rider_profiles IS 'Rider-specific profile data including preferences';
COMMENT ON TABLE email_verifications IS 'Corporate email verification records with OTP';
COMMENT ON TABLE wallets IS 'User payment wallets for ride transactions';
COMMENT ON TABLE payment_methods IS 'Payment methods linked to user wallets';
COMMENT ON TABLE emergency_contacts IS 'Emergency contacts for safety features';
COMMENT ON TABLE family_sharing_contacts IS 'Family members for ride sharing notifications';
COMMENT ON TABLE audit_logs IS 'System-wide audit trail for security and compliance';
COMMENT ON TABLE email_change_audit_logs IS 'Audit trail for corporate email changes';
COMMENT ON TABLE user_session_metadata IS 'Session metadata for multi-device support';