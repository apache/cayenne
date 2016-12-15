--  Licensed to the Apache Software Foundation (ASF) under one
--  or more contributor license agreements.  See the NOTICE file
--  distributed with this work for additional information
-- regarding copyright ownership.  The ASF licenses this file
--  to you under the Apache License, Version 2.0 (the
--  "License"); you may not use this file except in compliance
--  with the License.  You may obtain a copy of the License at
--
--    http://www.apache.org/licenses/LICENSE-2.0
--
--  Unless required by applicable law or agreed to in writing,
--  software distributed under the License is distributed on an
--  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
--  KIND, either express or implied.  See the License for the
--  specific language governing permissions and limitations
--  under the License.

CREATE SCHEMA schema_01;
SET SCHEMA schema_01;

CREATE TABLE schema_01.address (
    addrStr VARCHAR (250),
    id INTEGER  NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE schema_01.parent (
    id INTEGER  NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE schema_01.child (
    address_id INTEGER ,
    id INTEGER ,
    parent_id INTEGER,
    CONSTRAINT fk_address FOREIGN KEY (address_id) REFERENCES schema_01.address (id),
    CONSTRAINT fk_parent FOREIGN KEY (parent_id) REFERENCES schema_01.parent (id)
);