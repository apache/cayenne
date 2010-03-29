create table AUTO_PK_SUPPORT (TABLE_NAME CHAR(100) NOT NULL,  NEXT_ID INTEGER NOT NULL, PRIMARY KEY(TABLE_NAME));
insert into AUTO_PK_SUPPORT (TABLE_NAME, NEXT_ID) VALUES ('IdEntity', 1);
insert into AUTO_PK_SUPPORT (TABLE_NAME, NEXT_ID) VALUES ('IdColumnEntity', 1);
insert into AUTO_PK_SUPPORT (TABLE_NAME, NEXT_ID) VALUES ('BasicEntity', 1);
insert into AUTO_PK_SUPPORT (TABLE_NAME, NEXT_ID) VALUES ('PrimaryTable', 1);

create table IdEntity (id int not null, primary key(id));
create table IdColumnEntity (idcolumn int not null, primary key(idcolumn));
create table BasicEntity (id int not null, basicDefault VARCHAR(100), basicDefaultInt INTEGER, basicEager VARCHAR(100), basicLazy VARCHAR(100), primary key(id));
create table PrimaryTable(id int not null, primaryTableProperty VARCHAR(100), primary key (id));
create table SecondaryTable(id int not null, secondaryTableProperty VARCHAR(100), primary key (id));