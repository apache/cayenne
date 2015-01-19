CREATE TABLE player (
  id INTEGER NOT NULL,

  PRIMARY KEY (id)
);


-- one-to-one relationship
CREATE TABLE player_info (
  player_id INTEGER NOT NULL,

  PRIMARY KEY (player_id),
  CONSTRAINT fk_player_info FOREIGN KEY (player_id) REFERENCES player (id)
);

-- one-to-many relationship
CREATE TABLE pick_schedule (
  id INTEGER NOT NULL,
  owner_id INTEGER,
  selected_player_id INTEGER,
  PRIMARY KEY (id),

  CONSTRAINT fk_pick_schedule_player1 FOREIGN KEY (selected_player_id) REFERENCES player (id)
)
