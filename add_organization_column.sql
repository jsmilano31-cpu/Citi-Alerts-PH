-- SQL query to add organization column to users table
ALTER TABLE users ADD COLUMN organization VARCHAR(255) DEFAULT NULL AFTER phone;

-- Optional: Add an index on organization for better query performance
CREATE INDEX idx_users_organization ON users(organization);

-- If you want to update existing moderators with a default organization
UPDATE users SET organization = 'Default Department' WHERE user_type = 'moderator' AND organization IS NULL;
