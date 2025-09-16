CREATE SCHEMA IF NOT EXISTS uidam;

-- Create get_uuid() function for H2 compatibility
CREATE ALIAS IF NOT EXISTS get_uuid FOR "java.util.UUID.randomUUID";