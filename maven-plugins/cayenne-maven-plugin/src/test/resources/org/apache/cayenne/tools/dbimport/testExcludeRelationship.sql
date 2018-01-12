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

CREATE TABLE schema_01.TEST1(
    ID INTEGER NOT NULL,
    PRIMARY KEY (ID)
);

CREATE TABLE schema_01.TEST2(
    ID INTEGER NOT NULL,
    TEST1_ID INTEGER,
    PRIMARY KEY (ID)
);

CREATE TABLE schema_01.TEST3(
    ID INTEGER NOT NULL,
    PRIMARY KEY (ID)
);

CREATE TABLE schema_01.TEST4(
    ID INTEGER NOT NULL,
    TEST3_ID INTEGER,
    PRIMARY KEY (ID)
);

ALTER TABLE schema_01.TEST4
ADD FOREIGN KEY (TEST3_ID)
REFERENCES schema_01.TEST3 (ID)
;

CREATE TABLE schema_01.TEST5(
    ID INTEGER NOT NULL,
    PRIMARY KEY (ID)
);

CREATE TABLE schema_01.TEST6(
    ID INTEGER NOT NULL,
    TEST5_ID INTEGER,
    PRIMARY KEY (ID)
);

ALTER TABLE schema_01.TEST6
ADD FOREIGN KEY (TEST5_ID)
REFERENCES schema_01.TEST5 (ID)
;

CREATE TABLE schema_01.TEST7(
    ID INTEGER NOT NULL,
    PRIMARY KEY (ID)
);

CREATE TABLE schema_01.TEST8(
    ID INTEGER NOT NULL,
    TEST7_ID INTEGER,
    PRIMARY KEY (ID)
);

ALTER TABLE schema_01.TEST8
ADD FOREIGN KEY (TEST7_ID)
REFERENCES schema_01.TEST7 (ID)
;

CREATE TABLE schema_01.TEST9(
    ID INTEGER NOT NULL,
    PRIMARY KEY (ID)
);

CREATE TABLE schema_01.TEST10(
    ID INTEGER NOT NULL,
    TEST9_ID INTEGER,
    PRIMARY KEY (ID)
);

CREATE TABLE schema_01.TEST11(
    ID INTEGER NOT NULL,
    PRIMARY KEY (ID)
);

CREATE TABLE schema_01.TEST12(
    ID INTEGER NOT NULL,
    TEST11_ID INTEGER,
    PRIMARY KEY (ID)
);

ALTER TABLE schema_01.TEST12
ADD FOREIGN KEY (TEST11_ID)
REFERENCES schema_01.TEST11 (ID)
;