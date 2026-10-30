-- City state - one per user, replaces sim_state
CREATE TABLE city_state (
    user_id UUID PRIMARY KEY,
    coins INTEGER NOT NULL DEFAULT 0,
    premium_currency INTEGER NOT NULL DEFAULT 0,
    city_name VARCHAR(100) NOT NULL DEFAULT 'My City',
    population INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Building definitions - predefined catalog of available buildings
CREATE TABLE building_definitions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    type VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    cost INTEGER NOT NULL DEFAULT 0,
    cost_premium INTEGER NOT NULL DEFAULT 0,
    size_w INTEGER NOT NULL DEFAULT 1,
    size_h INTEGER NOT NULL DEFAULT 1,
    sprite_key VARCHAR(100) NOT NULL,
    population_bonus INTEGER NOT NULL DEFAULT 0,
    description VARCHAR(500)
);

-- User's placed buildings
CREATE TABLE user_buildings (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL,
    building_definition_id UUID NOT NULL,
    grid_x INTEGER NOT NULL,
    grid_y INTEGER NOT NULL,
    placed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (building_definition_id) REFERENCES building_definitions(id),
    UNIQUE (user_id, grid_x, grid_y)
);

CREATE INDEX idx_user_buildings_user ON user_buildings(user_id);

-- Migrate existing sim currency to city coins
INSERT INTO city_state (user_id, coins, created_at, updated_at)
SELECT user_id, total_currency, created_at, updated_at FROM sim_state;

-- Seed building definitions
INSERT INTO building_definitions (type, name, cost, size_w, size_h, sprite_key, population_bonus, description) VALUES
('TENT',        'Tent',         10,  1, 1, 'tent',        1,  'A humble beginning. Every city starts somewhere.'),
('HOUSE',       'House',        50,  2, 2, 'house',       5,  'A cozy home for your citizens.'),
('FARM',        'Farm',        100,  3, 2, 'farm',        2,  'Produces food to sustain your growing population.'),
('MARKET',      'Market',      200,  2, 2, 'market',      8,  'A bustling marketplace that attracts settlers.'),
('WORKSHOP',    'Workshop',    300,  2, 2, 'workshop',    4,  'Crafters gather here to build great things.'),
('LIBRARY',     'Library',     500,  2, 3, 'library',    10,  'Knowledge is power. Inspires your citizens.'),
('PARK',        'Park',        150,  2, 2, 'park',        3,  'Green space for rest and reflection.'),
('FOUNTAIN',    'Fountain',     75,  1, 1, 'fountain',    2,  'A decorative centerpiece for your city.'),
('TOWER',       'Watch Tower', 400,  1, 2, 'tower',       6,  'Keeps your city safe and attracts brave settlers.'),
('CASTLE',      'Castle',     1000,  3, 3, 'castle',     20,  'The crown jewel of a flourishing city.');
