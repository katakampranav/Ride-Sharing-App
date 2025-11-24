-- Migration: Add session_metadata table for persistent session tracking
-- Version: V2
-- Description: Creates session_metadata table in PostgreSQL for audit trail and multi-device session management

-- Create session_metadata table
CREATE TABLE IF NOT EXISTS session_metadata (
    metadata_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id VARCHAR(100) NOT NULL,
    user_id UUID NOT NULL,
    device_type VARCHAR(20),
    device_id VARCHAR(255),
    app_version VARCHAR(50),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP,
    ended_at TIMESTAMP,
    termination_reason VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    mobile_verified BOOLEAN DEFAULT FALSE,
    email_verified BOOLEAN DEFAULT FALSE,
    
    -- Foreign key constraint
    CONSTRAINT fk_session_metadata_user FOREIGN KEY (user_id) 
        REFERENCES user_accounts(user_id) ON DELETE CASCADE
);

-- Create indexes for efficient querying
CREATE INDEX idx_session_metadata_user_id ON session_metadata(user_id);
CREATE INDEX idx_session_metadata_session_id ON session_metadata(session_id);
CREATE INDEX idx_session_metadata_created_at ON session_metadata(created_at);
CREATE INDEX idx_session_metadata_ended_at ON session_metadata(ended_at);
CREATE INDEX idx_session_metadata_device_id ON session_metadata(device_id);
CREATE INDEX idx_session_metadata_active ON session_metadata(is_active);

-- Add comment to table
COMMENT ON TABLE session_metadata IS 'Stores persistent session metadata for audit trail and multi-device session management';

-- Add comments to columns
COMMENT ON COLUMN session_metadata.session_id IS 'Session ID from Redis';
COMMENT ON COLUMN session_metadata.user_id IS 'User ID associated with this session';
COMMENT ON COLUMN session_metadata.device_type IS 'Type of device (IOS, ANDROID, WEB)';
COMMENT ON COLUMN session_metadata.device_id IS 'Unique device identifier';
COMMENT ON COLUMN session_metadata.app_version IS 'Application version';
COMMENT ON COLUMN session_metadata.ip_address IS 'IP address when session was created';
COMMENT ON COLUMN session_metadata.user_agent IS 'User agent string';
COMMENT ON COLUMN session_metadata.created_at IS 'Timestamp when session was created';
COMMENT ON COLUMN session_metadata.last_activity_at IS 'Timestamp of last activity';
COMMENT ON COLUMN session_metadata.ended_at IS 'Timestamp when session ended';
COMMENT ON COLUMN session_metadata.termination_reason IS 'Reason for session termination (USER_LOGOUT, EXPIRED, SECURITY_EVENT, etc.)';
COMMENT ON COLUMN session_metadata.is_active IS 'Flag indicating if session is currently active';
COMMENT ON COLUMN session_metadata.mobile_verified IS 'Mobile verification status at session creation';
COMMENT ON COLUMN session_metadata.email_verified IS 'Email verification status at session creation';
