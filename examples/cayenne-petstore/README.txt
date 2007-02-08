Cayenne/Struts PetStore
=======================
(distributed under Apache license, version 2.0)


DESCRIPTION

"cayenne-petstore" is a classic Petstore Java application implemented with Cayenne and Struts.
By default application uses embedded Apache Derby database. 

It was ported from iBatis JPetstore-5.0 application by replacing iBatis with Cayenne. In its 
current reincarnation the application works but is not very polished. We demonstrated that 
porting to Cayenne is easy, even given the unfriendly DB design, however if we were to 
design the petstore from scratch, we would've done it differently in some aspects.


DEPLOYING

Just drop "dist/cayenne-petstore.war" in your favorite web container. 

Note that by default Derby (db engine used by cayenne-petstore) will attempt to 
create a database in the JVM "java.io.tmpdir" (whatever that is from the web container perspective;
Tomcat puts the database in $CATALINA_HOME/temp/). If you want a different location, unpack the war 
file and edit "WEB-INF/classes/derby.properties" setting "derby.system.home" to an absolute path.

If you want a different database alltogether, open "WEB-INF/cayenne.xml" file in 
CayenneModeler and do the needed changes (of course you can do them by hand as well).


INSPECTING SOURCE CODE

Petstore is a valid Eclispe project, just import it in Eclipse and do anything you want 
with it.