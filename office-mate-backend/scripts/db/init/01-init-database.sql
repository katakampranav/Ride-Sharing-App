-- Database initialization script
-- This script runs automatically when PostgreSQL container starts for the first time

-- Create database if it doesn't exist (handled by POSTGRES_DB env var)
-- This file is for additional initialization

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";  -- For text search
CREATE EXTENSION IF NOT EXISTS "btree_gist";  -- For advanced indexing

-- Create schemas for different environments if needed
-- CREATE SCHEMA IF NOT EXISTS dev;
-- CREATE SCHEMA IF NOT EXISTS test;

-- Set default search path
-- ALTER DATABASE officemate_dev SET search_path TO public;

-- Create read-only user for reporting (optional)
-- CREATE USER officemate_readonly WITH PASSWORD 'readonly_password';
-- GRANT CONNECT ON DATABASE officemate_dev TO officemate_readonly;
-- GRANT USAGE ON SCHEMA public TO officemate_readonly;
-- GRANT SELECT ON ALL TABLES IN SCHEMA public TO officemate_readonly;
-- ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO officemate_readonly;

-- Log initialization
DO $$
BEGIN
    RAISE NOTICE 'Database initialization completed successfully';
END $$;

