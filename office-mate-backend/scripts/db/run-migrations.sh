#!/bin/bash

# Database migration script
# This script applies SQL migrations to the PostgreSQL database

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Default values
DB_HOST=${POSTGRES_HOST:-localhost}
DB_PORT=${POSTGRES_PORT:-5432}
DB_NAME=${POSTGRES_DB:-officemate_dev}
DB_USER=${POSTGRES_USER:-postgres}
DB_PASSWORD=${POSTGRES_PASSWORD:-postgres}
MIGRATIONS_DIR="scripts/db/migrations"

echo "=========================================="
echo "Database Migration Script"
echo "=========================================="
echo ""
echo "Database: $DB_NAME"
echo "Host: $DB_HOST:$DB_PORT"
echo "User: $DB_USER"
echo ""

# Check if psql is available
if ! command -v psql &> /dev/null; then
    echo -e "${RED}Error: psql is not installed${NC}"
    echo "Please install PostgreSQL client tools"
    exit 1
fi

# Check if migrations directory exists
if [ ! -d "$MIGRATIONS_DIR" ]; then
    echo -e "${RED}Error: Migrations directory not found: $MIGRATIONS_DIR${NC}"
    exit 1
fi

# Test database connection
echo "Testing database connection..."
export PGPASSWORD=$DB_PASSWORD
if ! psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "SELECT 1" > /dev/null 2>&1; then
    echo -e "${RED}Error: Cannot connect to database${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Database connection successful${NC}"
echo ""

# Create migrations tracking table if it doesn't exist
echo "Creating migrations tracking table..."
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME << EOF
CREATE TABLE IF NOT EXISTS schema_migrations (
    version VARCHAR(255) PRIMARY KEY,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    description TEXT
);
EOF
echo -e "${GREEN}✓ Migrations tracking table ready${NC}"
echo ""

# Get list of applied migrations
APPLIED_MIGRATIONS=$(psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "SELECT version FROM schema_migrations ORDER BY version")

# Apply migrations
echo "Applying migrations..."
MIGRATION_COUNT=0
for migration_file in $(ls $MIGRATIONS_DIR/*.sql | sort); do
    MIGRATION_NAME=$(basename $migration_file .sql)
    
    # Check if migration has already been applied
    if echo "$APPLIED_MIGRATIONS" | grep -q "$MIGRATION_NAME"; then
        echo "  ⊘ $MIGRATION_NAME (already applied)"
        continue
    fi
    
    echo "  → Applying $MIGRATION_NAME..."
    
    # Apply migration
    if psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -f $migration_file; then
        # Record migration
        psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c \
            "INSERT INTO schema_migrations (version, description) VALUES ('$MIGRATION_NAME', 'Applied from $migration_file')"
        echo -e "  ${GREEN}✓ $MIGRATION_NAME applied successfully${NC}"
        MIGRATION_COUNT=$((MIGRATION_COUNT + 1))
    else
        echo -e "  ${RED}✗ Failed to apply $MIGRATION_NAME${NC}"
        exit 1
    fi
done

echo ""
if [ $MIGRATION_COUNT -eq 0 ]; then
    echo -e "${GREEN}No new migrations to apply${NC}"
else
    echo -e "${GREEN}Successfully applied $MIGRATION_COUNT migration(s)${NC}"
fi

# Display migration history
echo ""
echo "Migration history:"
psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c \
    "SELECT version, applied_at, description FROM schema_migrations ORDER BY applied_at DESC LIMIT 10"

echo ""
echo "=========================================="
echo -e "${GREEN}Migration Complete!${NC}"
echo "=========================================="

