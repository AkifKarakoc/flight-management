-- Create databases for Flight Management System
CREATE DATABASE IF NOT EXISTS reference_manager_db;
CREATE DATABASE IF NOT EXISTS flight_service_db;

-- Grant privileges to root user for both databases
GRANT ALL PRIVILEGES ON reference_manager_db.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON flight_service_db.* TO 'root'@'%';

-- Flush privileges to ensure they take effect
FLUSH PRIVILEGES;

-- Use reference_manager_db as default
USE reference_manager_db;