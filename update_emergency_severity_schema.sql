-- Update emergency_requests table to support severity assessment
-- Add severity assessment columns

ALTER TABLE emergency_requests 
ADD COLUMN severity_level INT DEFAULT 0,
ADD COLUMN severity_description VARCHAR(20) DEFAULT NULL,
ADD COLUMN has_injuries BOOLEAN DEFAULT FALSE,
ADD COLUMN area_accessible VARCHAR(10) DEFAULT NULL,
ADD COLUMN additional_notes TEXT DEFAULT NULL;

-- Add comments for documentation
ALTER TABLE emergency_requests 
MODIFY COLUMN severity_level INT DEFAULT 0 COMMENT 'Severity level from 1-5 (1=minor, 5=critical)',
MODIFY COLUMN severity_description VARCHAR(20) DEFAULT NULL COMMENT 'Minor, Moderate, or Severe',
MODIFY COLUMN has_injuries BOOLEAN DEFAULT FALSE COMMENT 'Whether injuries are reported',
MODIFY COLUMN area_accessible VARCHAR(10) DEFAULT NULL COMMENT 'Yes, No, or Blocked',
MODIFY COLUMN additional_notes TEXT DEFAULT NULL COMMENT 'Additional details from reporter';

-- Create index on severity_level for faster querying of high-priority emergencies
CREATE INDEX idx_emergency_severity ON emergency_requests(severity_level, status);

-- Create index on combined fields for responder filtering
CREATE INDEX idx_emergency_priority ON emergency_requests(severity_level, has_injuries, status, created_at);

-- Update existing records to have default severity assessment (optional)
-- UPDATE emergency_requests 
-- SET severity_level = 3, severity_description = 'Moderate' 
-- WHERE severity_level = 0 AND status IN ('pending', 'help_coming');

-- Example query for responders to get high-priority emergencies first
-- SELECT *, 
--        CASE 
--          WHEN severity_level >= 4 THEN 'CRITICAL'
--          WHEN severity_level = 3 THEN 'HIGH'
--          WHEN severity_level <= 2 THEN 'NORMAL'
--          ELSE 'UNASSESSED'
--        END as priority_label
-- FROM emergency_requests 
-- WHERE status = 'pending'
-- ORDER BY severity_level DESC, has_injuries DESC, created_at ASC;