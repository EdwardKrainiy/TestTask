-- PostgreSQL initialization script
-- This script runs when PostgreSQL container starts for the first time

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Set timezone
SET timezone = 'UTC';

-- Log successful initialization
SELECT 'PostgreSQL database initialized successfully' as status; 