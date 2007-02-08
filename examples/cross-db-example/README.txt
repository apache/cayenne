Copyright 2005, Andrus Adamchik. 
This example is distributed under the terms of ObjectStyle open source license.

This example shows how Cayenne can be used with multiple
databases at once, even with a relationship between tables across DBs.

HOW TO RUN:

This example is self-contained Eclipse project, including Cayenne 1.2RC2 jar. 
Just import it in Eclipse (3.1 or newer is recommended) and run "test.Main" 
class as "Java Application". As in-memory HSQLDB is used, no extra DB setup is required.

MAPPING DETAILS:

You can open and inspect mapping in CayenneModeler. XML files are located
under "cross-db-example/src". Each database schema has its own DataMap, with
relationship spanning across two DataMaps.