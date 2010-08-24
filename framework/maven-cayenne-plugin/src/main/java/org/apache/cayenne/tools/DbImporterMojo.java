/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.tools;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.map.naming.NamingStrategy;
import org.apache.cayenne.access.DbLoader;
import org.apache.cayenne.access.AbstractDbLoaderDelegate;
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
     * Default is <code>true</code>.
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
     *
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
     * Indicates whether primary keys should be mapped as meaningful attributes in the object entities.
     *
     * Default is <code>false</code>.
     *
     * @parameter expression="${cdbimport.meaningfulPk}" default-value="false"
     */
    private boolean meaningfulPk;

    /**
     * Java class implementing org.apache.cayenne.map.naming.NamingStrategy.
     * This is used to specify how ObjEntities will be mapped from the imported DB schema.
     *
     * The default is a basic naming strategy.
     *
     * @parameter expression="${cdbimport.namingStrategy}" default-value="org.apache.cayenne.map.naming.SmartNamingStrategy"
     */
    private String namingStrategy;

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

    public void execute() throws MojoExecutionException, MojoFailureException {

        logger = new MavenLogger(this);

        logger.debug(String.format("connection settings - [driver: %s, url: %s, username: %s, password: %s]", driver, url, username, password));

        logger.info(String.format("importer options - [map: %s, overwriteExisting: %s, schemaName: %s, tablePattern: %s, importProcedures: %s, procedurePattern: %s, meaningfulPk: %s, namingStrategy: %s]",
                map, overwriteExisting, schemaName, tablePattern, importProcedures, procedurePattern, meaningfulPk, namingStrategy));

        try {
            final DbAdapter adapterInst = (adapter == null) ? new JdbcAdapter()
                                                                : (DbAdapter) Class.forName(adapter).newInstance();

            // load driver taking custom CLASSPATH into account...
            DriverDataSource dataSource = new DriverDataSource((Driver) Class.forName(driver).newInstance(), url, username, password);

            // Load the data map and run the db importer.
            final LoaderDelegate loaderDelegate = new LoaderDelegate();
            final DbLoader loader = new DbLoader(dataSource.getConnection(), adapterInst, loaderDelegate);
            loader.setCreatingMeaningfulPK(meaningfulPk);

            if (namingStrategy != null) {
                final NamingStrategy namingStrategyInst = (NamingStrategy) Class.forName(namingStrategy).newInstance();
                loader.setNamingStrategy(namingStrategyInst);
            }

            mapFile = new File(map);
            final DataMap dataMap = mapFile.exists() ? loadDataMap() : new DataMap();
            loader.loadDataMapFromDB(schemaName, tablePattern, dataMap);

            for (ObjEntity addedObjEntity : loaderDelegate.getAddedObjEntities()) {
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

    final class LoaderDelegate extends AbstractDbLoaderDelegate {
       
        public boolean overwriteDbEntity(final DbEntity ent) throws CayenneException {
            return overwriteExisting;
        }

        public void dbEntityAdded(final DbEntity ent) {
            super.dbEntityAdded(ent);
            logger.info("Added DB entity: " + ent.getName());
        }

        public void dbEntityRemoved(final DbEntity ent) {
            super.dbEntityRemoved(ent);
            logger.info("Removed DB entity: " + ent.getName());
        }

        public void objEntityAdded(final ObjEntity ent) {
            super.objEntityAdded(ent);
            logger.info("Added obj entity: " + ent.getName());
        }

        public void objEntityRemoved(final ObjEntity ent) {
            super.objEntityRemoved(ent);
            logger.info("Removed obj entity: " + ent.getName());
        }
    }

    /** Loads and returns DataMap based on <code>map</code> attribute. */
    protected DataMap loadDataMap() throws Exception {
        final InputSource in = new InputSource(mapFile.getCanonicalPath());
        return new MapLoader().loadDataMap(in);
    }
}
