-- Sim state - one per user
CREATE TABLE sim_state (
    user_id UUID PRIMARY KEY,
    total_currency INTEGER NOT NULL DEFAULT 0,
    current_stage VARCHAR(20) NOT NULL DEFAULT 'starter',
    companion_type VARCHAR(20) NOT NULL DEFAULT 'plant',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Sim milestones - predefined progression
CREATE TABLE sim_milestones (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    stage VARCHAR(20) NOT NULL,
    currency_threshold INTEGER NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000)
);

CREATE INDEX idx_sim_milestones_stage ON sim_milestones(stage);
