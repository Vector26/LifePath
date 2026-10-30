-- Quests table
CREATE TABLE quests (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL,
    title VARCHAR(500) NOT NULL,
    notes VARCHAR(5000),
    priority_level VARCHAR(2) NOT NULL,
    parent_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    time_of_day VARCHAR(20),
    context_tag VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    deleted_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (parent_id) REFERENCES quests(id)
);

CREATE INDEX idx_quests_user_id ON quests(user_id);
CREATE INDEX idx_quests_user_status ON quests(user_id, status);
CREATE INDEX idx_quests_parent_id ON quests(parent_id);
CREATE INDEX idx_quests_deleted_at ON quests(deleted_at);

-- Blocked quests table
CREATE TABLE blocked_quests (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    quest_id UUID NOT NULL,
    reason VARCHAR(1000),
    unblock_trigger VARCHAR(20) NOT NULL,
    unblock_at TIMESTAMP,
    unblock_location VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unblocked_at TIMESTAMP,
    FOREIGN KEY (quest_id) REFERENCES quests(id)
);

CREATE INDEX idx_blocked_quests_quest_id ON blocked_quests(quest_id);
CREATE INDEX idx_blocked_quests_unblock ON blocked_quests(unblock_trigger, unblocked_at, unblock_at);
