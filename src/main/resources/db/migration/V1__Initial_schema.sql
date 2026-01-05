-- Initial schema for Distributed Task Scheduler


CREATE TABLE IF NOT EXISTS tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(50) NOT NULL CHECK (type IN ('HTTP', 'SHELL', 'DUMMY')),
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' 
        CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED')),
    schedule_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    worker_id VARCHAR(100),
    assigned_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    execution_output TEXT,
    execution_metadata JSONB
);

-- Create indexes for efficient task scheduling
CREATE INDEX IF NOT EXISTS idx_tasks_due ON tasks (schedule_at, status) 
    WHERE status = 'PENDING';
CREATE INDEX IF NOT EXISTS idx_tasks_worker ON tasks (worker_id, status) 
    WHERE status = 'RUNNING';
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks (status);
CREATE INDEX IF NOT EXISTS idx_tasks_created_at ON tasks (created_at);

-- Create worker_heartbeats table
CREATE TABLE IF NOT EXISTS worker_heartbeats (
    worker_id VARCHAR(100) PRIMARY KEY,
    last_heartbeat TIMESTAMP WITH TIME ZONE DEFAULT now(),
    worker_metadata JSONB,
    registered_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- Create index for heartbeat timeout detection
CREATE INDEX IF NOT EXISTS idx_heartbeat_timeout ON worker_heartbeats (last_heartbeat);

-- Create task_executions table for execution history
CREATE TABLE IF NOT EXISTS task_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    worker_id VARCHAR(100) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    completed_at TIMESTAMP WITH TIME ZONE,
    success BOOLEAN,
    output TEXT,
    error_message TEXT,
    execution_metadata JSONB
);

-- Create indexes for execution history queries
CREATE INDEX IF NOT EXISTS idx_executions_task ON task_executions (task_id);
CREATE INDEX IF NOT EXISTS idx_executions_worker ON task_executions (worker_id);
CREATE INDEX IF NOT EXISTS idx_executions_started_at ON task_executions (started_at);

-- Create trigger to update tasks.updated_at automatically
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_tasks_updated_at 
    BEFORE UPDATE ON tasks 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();