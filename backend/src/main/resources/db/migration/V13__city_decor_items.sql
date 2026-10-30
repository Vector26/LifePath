-- Re-introduce city gameplay tables for decor placement + decay loop
CREATE TABLE building_definitions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    type VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    cost INTEGER NOT NULL DEFAULT 0,
    sprite_key VARCHAR(100) NOT NULL,
    tile_gid INTEGER NOT NULL,
    ttl_seconds INTEGER NOT NULL DEFAULT 86400,
    decay_grace_seconds INTEGER NOT NULL DEFAULT 86400,
    maintenance_cost INTEGER NOT NULL DEFAULT 5
);

CREATE TABLE user_buildings (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL,
    building_definition_id UUID NOT NULL,
    grid_x INTEGER NOT NULL,
    grid_y INTEGER NOT NULL,
    placed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    decayed_at TIMESTAMP NULL,
    remove_after_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (building_definition_id) REFERENCES building_definitions(id),
    UNIQUE (user_id, grid_x, grid_y)
);

CREATE INDEX idx_user_buildings_user ON user_buildings(user_id);
CREATE INDEX idx_user_buildings_expires ON user_buildings(expires_at);
CREATE INDEX idx_user_buildings_remove_after ON user_buildings(remove_after_at);

INSERT INTO building_definitions (
    type, name, cost, sprite_key, tile_gid, ttl_seconds, decay_grace_seconds, maintenance_cost
) VALUES
('FLOWER_BED', 'Flower Bed', 20, 'flower_bed', 34, 86400, 86400, 6),
('BUSH', 'Bush', 25, 'bush', 273, 129600, 86400, 8),
('STONE', 'Decorative Stone', 30, 'stone', 286, 172800, 86400, 10),
('FOUNTAIN', 'Fountain', 60, 'fountain', 291, 259200, 172800, 15),
('LAMP_POST', 'Lamp Post', 40, 'lamp_post', 8, 172800, 86400, 12);
