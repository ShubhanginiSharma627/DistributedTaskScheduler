-- Add version columns for optimistic locking

-- Add version column to tasks table for optimistic locking
ALTER TABLE tasks ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Add version column to worker_heartbeats table for optimistic locking  
ALTER TABLE worker_heartbeats ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Create indexes for version columns to improve performance
CREATE INDEX idx_tasks_version ON tasks (version);
CREATE INDEX idx_worker_heartbeats_version ON worker_heartbeats (version);

-- Add comments to document the purpose of version columns
COMMENT ON COLUMN tasks.version IS 'Version column for optimistic locking to prevent concurrent modification conflicts';
COMMENT ON COLUMN worker_heartbeats.version IS 'Version column for optimistic locking to prevent concurrent heartbeat update conflicts';