CREATE TABLE blocked_tasks (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    task_type VARCHAR(20) NOT NULL,
    task_id UUID NOT NULL,
    reason VARCHAR(1000),
    unblock_trigger VARCHAR(20) NOT NULL,
    unblock_at TIMESTAMP,
    unblock_location VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unblocked_at TIMESTAMP
);

CREATE INDEX idx_blocked_tasks_lookup
    ON blocked_tasks (user_id, task_type, unblocked_at, unblock_trigger, unblock_at);

CREATE INDEX idx_blocked_tasks_active_task
    ON blocked_tasks (user_id, task_type, task_id, unblocked_at);
