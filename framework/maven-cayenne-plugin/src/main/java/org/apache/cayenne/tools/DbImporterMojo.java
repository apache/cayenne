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
import org.apache.cayenne.util.DeleteRuleUpdater;
import org.apache.cayenne.conn.DriverDataSource;
import org.apache.cayenne.CayenneException;
import org.apache.commons.logging.Log;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.PrintWriter;
import java.sql.Driver;
import java.util.List;
import java.util.ArrayList;

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
	private String map;

    /**
     * Indicates whether existing DB and object entities should be overwritten.
     * This is an all-or-nothing setting.  If you need finer granularity, please
     * use the Cayenne Modeler.
     *
     * @parameter expression="${cdbimport.overwriteExisting}" default-value="true"
     */
    private boolean overwriteExisting;

    /**
     * DB schema to use for DB importing.
     *
     * @parameter expression="${cdbimport.schemaName}"
     */
    private String schemaName;

    /**
     * Pattern for tables to import from DB.
     *
     * The default is to match against all tables.
     *
     * @parameter expression="${cdbimport.tablePattern}"
     */
    private String tablePattern;

    /**
     * Indicates whether stored procedures should be imported.
     * Default is <code>false</code>.
     *
     * @parameter expression="${cdbimport.importProcedures}" default-value="false"
     */
    private boolean importProcedures;

    /**
     * Pattern for stored procedures to import from DB.  This is only meaningful if
     * <code>importProcedures</code> is set to <code>true</code>.
     *
     * The default is to match against all stored procedures.
     *
     * @parameter expression="${cdbimport.procedurePattern}"
     */
    private String procedurePattern;

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

    /**
     * Maven logger.
     */
    private Log logger;

    /**
     * The DataMap file to use for importing.
     */
    private File mapFile;

    private List<ObjEntity> addedObjEntities = new ArrayList<ObjEntity>();

    public void execute() throws MojoExecutionException, MojoFailureException {

        logger = new MavenLogger(this);

        logger.info(String.format("connection settings - [driver: %s, url: %s, username: %s]", driver, url, username));

        logger.info(String.format("importer options - [map: %s, schemaName: %s, tablePattern: %s, driver: %s, url: %s, username: %s, password: %s]",
                map, schemaName, tablePattern, driver, url, username, password));

        try {
            final DbAdapter adapterInst = (adapter == null) ? new JdbcAdapter()
                                                                : (DbAdapter) Class.forName(adapter).newInstance();

            // load driver taking custom CLASSPATH into account...
            DriverDataSource dataSource = new DriverDataSource((Driver) Class.forName(driver).newInstance(), url, username, password);

            // Load the data map and run the db importer.
            final DbLoader loader = new DbLoader(dataSource.getConnection(), adapterInst, new LoaderDelegate());

            mapFile = new File(map);
            final DataMap dataMap = mapFile.exists() ? loadDataMap() : new DataMap();
            loader.loadDataMapFromDB(schemaName, tablePattern, dataMap);

            for (ObjEntity addedObjEntity : addedObjEntities) {
                    DeleteRuleUpdater.updateObjEntity(addedObjEntity);
                }

            if (importProcedures) {
                loader.loadProceduresFromDB(schemaName, procedurePattern, dataMap);
            }

            // Write the new DataMap out to disk.
            mapFile.delete();
            PrintWriter pw = new PrintWriter(mapFile);
            dataMap.encodeAsXML(pw);
            pw.close();
        } catch (final Exception ex) {
            final Throwable th = Util.unwindException(ex);

            String message = "Error importing database schema";

            if (th.getLocalizedMessage() != null) {
                message += ": " + th.getLocalizedMessage();
            }

            logger.error(message);
            throw new MojoExecutionException(message, th);
        }
    }

    final class LoaderDelegate implements DbLoaderDelegate {

        public boolean overwriteDbEntity(final DbEntity ent) throws CayenneException {
            return overwriteExisting;
        }

        public void dbEntityAdded(final DbEntity ent) {
            logger.info("Added DB entity: " + ent.getName());
            ent.getDataMap().addDbEntity(ent);
        }

        public void dbEntityRemoved(final DbEntity ent) {
            logger.info("Removed DB entity: " + ent.getName());
            ent.getDataMap().removeDbEntity(ent.getName());
        }

        public void objEntityAdded(final ObjEntity ent) {
            logger.info("Added obj entity: " + ent.getName());
            addedObjEntities.add(ent);
            ent.getDataMap().addObjEntity(ent);
        }

        public void objEntityRemoved(final ObjEntity ent) {
            logger.info("Removed obj entity: " + ent.getName());
            ent.getDataMap().removeObjEntity(ent.getName());
        }
    }

    /** Loads and returns DataMap based on <code>map</code> attribute. */
    protected DataMap loadDataMap() throws Exception {
        final InputSource in = new InputSource(mapFile.getCanonicalPath());
        return new MapLoader().loadDataMap(in);
    }
}
