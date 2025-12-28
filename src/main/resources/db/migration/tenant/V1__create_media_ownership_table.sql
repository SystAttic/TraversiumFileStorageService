--Create table
CREATE TABLE media_metadata (
    id UUID PRIMARY KEY,
    filename VARCHAR(100) NOT NULL UNIQUE,
    owner_id VARCHAR(100) NOT NULL,
    upload_time TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Index for faster lookups during delete/download
CREATE INDEX idx_media_filename ON media_metadata(filename);