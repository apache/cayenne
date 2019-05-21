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
 

CREATE TABLE Parent (
  id INTEGER NOT NULL,
  COL2 CHAR(25),
  COL3 DECIMAL(10,2),
  COL4 VARCHAR(25),
  COL5 DATE,

  PRIMARY KEY (id),
  UNIQUE (COL3)
);

CREATE TABLE Child (
  id INTEGER NOT NULL,
  Parent_id CHAR(25),
  COL3 DECIMAL(10,2),
  COL4 VARCHAR(25),
  COL5 DATE,

  PRIMARY KEY (id)
);