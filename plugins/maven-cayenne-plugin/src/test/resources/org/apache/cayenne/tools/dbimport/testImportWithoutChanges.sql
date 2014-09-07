CREATE TABLE testImportWithoutChanges (
  COL1 INTEGER NOT NULL,
  COL2 CHAR(25),
  COL3 DECIMAL(10,2),
  COL4 VARCHAR(25),
  COL5 DATE,

  PRIMARY KEY (COL1),
  UNIQUE (COL3)
)