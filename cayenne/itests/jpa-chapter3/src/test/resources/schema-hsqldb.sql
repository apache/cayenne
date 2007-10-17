create table AUTO_PK_SUPPORT (TABLE_NAME CHAR(100) NOT NULL,  NEXT_ID INTEGER NOT NULL, PRIMARY KEY(TABLE_NAME));
insert into AUTO_PK_SUPPORT (TABLE_NAME, NEXT_ID) VALUES ('SimpleEntity', 1);
insert into AUTO_PK_SUPPORT (TABLE_NAME, NEXT_ID) VALUES ('CallbackEntity', 1);
insert into AUTO_PK_SUPPORT (TABLE_NAME, NEXT_ID) VALUES ('CallbackEntity2', 1);
insert into AUTO_PK_SUPPORT (TABLE_NAME, NEXT_ID) VALUES ('ListenerEntity1', 1);
insert into AUTO_PK_SUPPORT (TABLE_NAME, NEXT_ID) VALUES ('ListenerEntity2', 1);

create table SimpleEntity (id int not null, property1 VARCHAR(100), primary key(id));
create table CallbackEntity (id int not null, primary key(id));
create table CallbackEntity2 (id int not null, primary key(id));
create table ListenerEntity1 (id int not null, primary key(id));
create table ListenerEntity2 (id int not null, primary key(id));
