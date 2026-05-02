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

CREATE TABLE APP.A (
  id INTEGER NOT NULL,

  PRIMARY KEY (id)
);

CREATE TABLE APP.B (
  id INTEGER NOT NULL,

  PRIMARY KEY (id)
);

CREATE TABLE APP.A_B (
  A_ID INTEGER NOT NULL,
  B_ID INTEGER NOT NULL,

  PRIMARY KEY (A_ID, B_ID),
  CONSTRAINT A_B_A FOREIGN KEY (A_ID) REFERENCES APP.A (ID),
  CONSTRAINT A_B_B FOREIGN KEY (B_ID) REFERENCES APP.B (ID)
);

CREATE TABLE APP.C (
  id INTEGER NOT NULL,

  PRIMARY KEY (id)
);

CREATE TABLE APP.X (
  id INTEGER NOT NULL,

  PRIMARY KEY (id)
);

CREATE TABLE APP.Y (
  id INTEGER NOT NULL,

  PRIMARY KEY (id)
);

CREATE TABLE APP.X_Y (
  X_ID INTEGER NOT NULL,
  Y_ID INTEGER NOT NULL,

  PRIMARY KEY (X_ID, Y_ID),
  CONSTRAINT X_Y_X FOREIGN KEY (X_ID) REFERENCES APP.X (ID),
  CONSTRAINT X_Y_Y FOREIGN KEY (Y_ID) REFERENCES APP.Y (ID)
);