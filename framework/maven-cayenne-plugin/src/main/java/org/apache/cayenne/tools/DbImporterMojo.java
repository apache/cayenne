package org.apache.cayenne.tools;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.access.DbLoader;
import org.apache.cayenne.access.DbLoaderDelegate;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.conn.DriverDataSource;
import org.apache.cayenne.CayenneException;
import org.apache.commons.logging.Log;
import org.xml.sax.InputSource;

import java.io.File;
import java.sql.Driver;

/**
 * Maven mojo to reverse engineer datamap from DB.
 *
 * @since 3.0
 *
 * @phase generate-sources
 * @goal cdbimport
 */
public class DbImporterMojo extends AbstractMojo {

    /**
	 * DataMap XML file to use as a base for DB importing.
	 *
	 * @parameter expression="${cdbimport.map}"
	 * @required
	 */
	private File map;

    /**
     * DB schema to use for DB importing.
     */
    private String schemaName;

    /**
     * Pattern for tables to import from DB.
     */
    private String tablePattern;

    /**
     * Java class implementing org.apache.cayenne.dba.DbAdapter.
     * While this attribute is optional (a generic JdbcAdapter is used if not set),
     * it is highly recommended to specify correct target adapter.
     *
     * @parameter expression="${cdbimport.adapter}"
     */
    private String adapter;
    
    /**
     * A class of JDBC driver to use for the target database.
     *
     * @parameter expression="${cdbimport.driver}"
     * @required
     */
    private String driver;

    /**
     * JDBC connection URL of a target database.
     *
     * @parameter expression="${cdbimport.url}"
     * @required
     */
    private String url;

    /**
     * Database user name.
     *
     * @parameter expression="${cdbimport.username}"
     */
    private String username;

    /**
     * Database user password.
     *
     * @parameter expression="${cdbimport.password}"
     */
    private String password;
    

    public void execute() throws MojoExecutionException, MojoFailureException {

        Log logger = new MavenLogger(this);

        logger.info(String.format("connection settings - [driver: %s, url: %s, username: %s]", driver, url, username));

        logger.info(String.format("importer options - [map: %s, schemaName: %s, tablePattern: %s, driver: %s, url: %s, username: %s, password: %s]",
                map, schemaName, tablePattern, driver, url, username, password));

        try {
            final DbAdapter adapterInst = (adapter == null) ? new JdbcAdapter()
                                                                : (DbAdapter) Class.forName(adapter).newInstance();

            // load driver taking custom CLASSPATH into account...
            DriverDataSource dataSource = new DriverDataSource((Driver) Class.forName(driver).newInstance(), url, username, password);

            // Load the data map and run the db importer.
            DataMap dataMap = loadDataMap();
            final DbLoader loader = new DbLoader(dataSource.getConnection(), adapterInst, new LoaderDelegate());
            loader.loadDataMapFromDB(schemaName, tablePattern, dataMap);
        } catch (Exception ex) {
            Throwable th = Util.unwindException(ex);

            String message = "Error importing database schema";

            if (th.getLocalizedMessage() != null) {
                message += ": " + th.getLocalizedMessage();
            }

            logger.error(message);
            throw new MojoExecutionException(message, th);
        }
    }

    final class LoaderDelegate implements DbLoaderDelegate {

        public boolean overwriteDbEntity(DbEntity ent) throws CayenneException {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void dbEntityAdded(DbEntity ent) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void dbEntityRemoved(DbEntity ent) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void objEntityAdded(ObjEntity ent) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void objEntityRemoved(ObjEntity ent) {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    /** Loads and returns DataMap based on <code>map</code> attribute. */
    protected DataMap loadDataMap() throws Exception {
        InputSource in = new InputSource(map.getCanonicalPath());
        return new MapLoader().loadDataMap(in);
    }
}
