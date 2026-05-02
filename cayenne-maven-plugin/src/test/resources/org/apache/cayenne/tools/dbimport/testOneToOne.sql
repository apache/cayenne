--  Licensed to the Apache Software Foundation (ASF) under one
--  or more contributor license agreements.  See the NOTICE file
--  distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
--  to you under the Apache License, Version 2.0 (the
--  "License"); you may not use this file except in compliance
--  with the License.  You may obtain a copy of the License at
--
--    https://www.apache.org/licenses/LICENSE-2.0
--
--  Unless required by applicable law or agreed to in writing,
--  software distributed under the License is distributed on an
--  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
--  KIND, either express or implied.  See the License for the
--  specific language governing permissions and limitations
--  under the License.

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
