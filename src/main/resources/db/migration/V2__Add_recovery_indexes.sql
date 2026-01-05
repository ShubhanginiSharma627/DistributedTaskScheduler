-- Additional indexes and optimizations for system recovery

-- Add index for efficient recovery of RUNNING tasks
CREATE INDEX IF NOT EXISTS idx_tasks_status_updated_at ON tasks (status, updated_at);

-- Add index for worker cleanup operations
CREATE INDEX IF NOT EXISTS idx_worker_heartbeats_registered_at ON worker_heartbeats (registered_at);

-- Add index for task execution history cleanup
CREATE INDEX IF NOT EXISTS idx_task_executions_completed_at ON task_executions (completed_at);

-- Create a view for system recovery monitoring
CREATE OR REPLACE VIEW system_recovery_status AS
SELECT 
    (SELECT COUNT(*) FROM tasks WHERE status = 'RUNNING') as running_tasks,
    (SELECT COUNT(*) FROM tasks WHERE status = 'PENDING') as pending_tasks,
    (SELECT COUNT(*) FROM tasks WHERE status = 'FAILED') as failed_tasks,
    (SELECT COUNT(*) FROM tasks WHERE status = 'SUCCESS') as successful_tasks,
    (SELECT COUNT(*) FROM worker_heartbeats) as active_workers,
    (SELECT COUNT(*) FROM tasks WHERE status = 'RUNNING' AND worker_id NOT IN (SELECT worker_id FROM worker_heartbeats)) as orphaned_tasks,
    now() as snapshot_time;

-- Add comment to track recovery operations
COMMENT ON VIEW system_recovery_status IS 'System recovery monitoring view for tracking task and worker states';