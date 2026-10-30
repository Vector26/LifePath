-- Road cells placed by the user on the city grid
CREATE TABLE road_cells (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL,
    grid_x INTEGER NOT NULL,
    grid_y INTEGER NOT NULL,
    placed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE (user_id, grid_x, grid_y)
);

CREATE INDEX idx_road_cells_user ON road_cells(user_id);
