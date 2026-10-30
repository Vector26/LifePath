-- Seed default milestones per GAMIFICATION_ENGINE
INSERT INTO sim_milestones (id, stage, currency_threshold, title, description) VALUES
    (gen_random_uuid(), 'starter', 0, 'First step', 'Your journey begins'),
    (gen_random_uuid(), 'starter', 50, 'Getting started', 'Building momentum'),
    (gen_random_uuid(), 'stable', 200, 'First apartment', 'Your Sim moves into a cozy apartment'),
    (gen_random_uuid(), 'stable', 400, 'Comfortable', 'Life is looking good'),
    (gen_random_uuid(), 'thriving', 500, 'Thriving home', 'A place to flourish'),
    (gen_random_uuid(), 'thriving', 750, 'Halfway there', 'The best is yet to come'),
    (gen_random_uuid(), 'flourishing', 1000, 'Flourishing', 'Your Sim is thriving');
