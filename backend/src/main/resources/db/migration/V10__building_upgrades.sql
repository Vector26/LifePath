-- Add upgrade columns to building_definitions
ALTER TABLE building_definitions ADD COLUMN upgrade_target_id UUID REFERENCES building_definitions(id);
ALTER TABLE building_definitions ADD COLUMN upgrade_cost INTEGER NOT NULL DEFAULT 0;

-- Tent -> House (cost 40), House -> Market (cost 150), Fountain -> Park (cost 75)
UPDATE building_definitions SET upgrade_target_id = (SELECT id FROM building_definitions b2 WHERE b2.type = 'HOUSE'), upgrade_cost = 40 WHERE type = 'TENT';
UPDATE building_definitions SET upgrade_target_id = (SELECT id FROM building_definitions b2 WHERE b2.type = 'MARKET'), upgrade_cost = 150 WHERE type = 'HOUSE';
UPDATE building_definitions SET upgrade_target_id = (SELECT id FROM building_definitions b2 WHERE b2.type = 'PARK'), upgrade_cost = 75 WHERE type = 'FOUNTAIN';
