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

import java.io.File;
import java.io.PrintWriter;
import java.sql.Driver;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.access.AbstractDbLoaderDelegate;
import org.apache.cayenne.access.DbLoader;
import org.apache.cayenne.configuration.ToolModule;
import org.apache.cayenne.conn.DriverDataSource;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.naming.NamingStrategy;
import org.apache.cayenne.util.DeleteRuleUpdater;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.xml.sax.InputSource;

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
     * Indicates whether existing DB and object entities should be overwritten.
     * This is an all-or-nothing setting. If you need finer granularity, please
     * use the Cayenne Modeler.
     * 
     * Default is <code>true</code>.
     * 
     * @parameter expression="${cdbimport.overwriteExisting}"
     *            default-value="true"
     */
    private boolean overwriteExisting;

    /**
     * DB schema to use for DB importing.
     * 
     * @parameter expression="${cdbimport.schemaName}"
     * @deprecated since 3.2 renamed to "schema"
     */
    private String schemaName;

    /**
     * DB schema to use for DB importing.
     * 
     * @parameter expression="${cdbimport.catalog}"
     * @since 3.2
     */
    private String catalog;

    /**
     * DB schema to use for DB importing.
     * 
     * @parameter expression="${cdbimport.schema}"
     * @since 3.2
     */
    private String schema;

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
     * @parameter expression="${cdbimport.importProcedures}"
     *            default-value="false"
     */
    private boolean importProcedures;

    /**
     * Pattern for stored procedures to import from DB. This is only meaningful
     * if <code>importProcedures</code> is set to <code>true</code>.
     * 
     * The default is to match against all stored procedures.
     * 
     * @parameter expression="${cdbimport.procedurePattern}"
     */
    private String procedurePattern;

    /**
     * Indicates whether primary keys should be mapped as meaningful attributes
     * in the object entities.
     * 
     * Default is <code>false</code>.
     * 
     * @parameter expression="${cdbimport.meaningfulPk}" default-value="false"
     */
    private boolean meaningfulPk;

    /**
     * Java class implementing org.apache.cayenne.map.naming.NamingStrategy.
     * This is used to specify how ObjEntities will be mapped from the imported
     * DB schema.
     * 
     * The default is a basic naming strategy.
     * 
     * @parameter expression="${cdbimport.namingStrategy}"
     *            default-value="org.apache.cayenne.map.naming.SmartNamingStrategy"
     */
    private String namingStrategy;

    /**
     * Java class implementing org.apache.cayenne.dba.DbAdapter. While this
     * attribute is optional (a generic JdbcAdapter is used if not set), it is
     * highly recommended to specify correct target adapter.
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

        getLog().debug(
                String.format(
                        "connection settings - [driver: %s, url: %s, username: %s, password: %s]",
                        driver, url, username, password));

        getLog().info(
                String.format(
                        "importer options - [map: %s, overwriteExisting: %s, schema: %s, tablePattern: %s, importProcedures: %s, procedurePattern: %s, meaningfulPk: %s, namingStrategy: %s]",
                        map, overwriteExisting, schema, tablePattern,
                        importProcedures, procedurePattern, meaningfulPk,
                        namingStrategy));

        try {
            doExecute();
        } catch (Exception ex) {
            Throwable th = Util.unwindException(ex);

            String message = "Error importing database schema";

            if (th.getLocalizedMessage() != null) {
                message += ": " + th.getLocalizedMessage();
            }

            getLog().error(message);
            throw new MojoExecutionException(message, th);
        }
    }

    private void doExecute() throws Exception {

        String schema = getSchema();

        Injector injector = DIBootstrap.createInjector(new ToolModule());
        AdhocObjectFactory objectFactory = injector
                .getInstance(AdhocObjectFactory.class);

        DbAdapter adapterInst = (adapter == null) ? (DbAdapter) objectFactory
                .newInstance(DbAdapter.class, JdbcAdapter.class.getName())
                : (DbAdapter) objectFactory.newInstance(DbAdapter.class,
                        adapter);

        // load driver taking custom CLASSPATH into account...
        DriverDataSource dataSource = new DriverDataSource((Driver) Class
                .forName(driver).newInstance(), url, username, password);

        // Load the data map and run the db importer.
        final LoaderDelegate loaderDelegate = new LoaderDelegate();
        final DbLoader loader = new DbLoader(dataSource.getConnection(),
                adapterInst, loaderDelegate);
        loader.setCreatingMeaningfulPK(meaningfulPk);

        if (namingStrategy != null) {
            NamingStrategy namingStrategyInst = (NamingStrategy) Class.forName(
                    namingStrategy).newInstance();
            loader.setNamingStrategy(namingStrategyInst);
        }

        DataMap dataMap = map.exists() ? loadDataMap() : new DataMap();
        String[] types = loader.getDefaultTableTypes();
        loader.load(dataMap, catalog, schema, tablePattern, types);

        for (ObjEntity addedObjEntity : loaderDelegate.getAddedObjEntities()) {
            DeleteRuleUpdater.updateObjEntity(addedObjEntity);
        }

        if (importProcedures) {
            loader.loadProcedures(dataMap, catalog, schema, procedurePattern);
        }

        // Write the new DataMap out to disk.
        map.delete();

        PrintWriter pw = new PrintWriter(map);
        XMLEncoder encoder = new XMLEncoder(pw, "\t");

        encoder.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        dataMap.encodeAsXML(encoder);

        pw.close();
    }

    private String getSchema() {
        if (schemaName != null) {
            getLog().warn(
                    "'schemaName' property is deprecated. Use 'schema' instead");
        }

        return schema != null ? schema : schemaName;
    }

    final class LoaderDelegate extends AbstractDbLoaderDelegate {

        public boolean overwriteDbEntity(final DbEntity ent)
                throws CayenneException {
            return overwriteExisting;
        }

        public void dbEntityAdded(final DbEntity ent) {
            super.dbEntityAdded(ent);
            getLog().info("Added DB entity: " + ent.getName());
        }

        public void dbEntityRemoved(final DbEntity ent) {
            super.dbEntityRemoved(ent);
            getLog().info("Removed DB entity: " + ent.getName());
        }

        public void objEntityAdded(final ObjEntity ent) {
            super.objEntityAdded(ent);
            getLog().info("Added obj entity: " + ent.getName());
        }

        public void objEntityRemoved(final ObjEntity ent) {
            super.objEntityRemoved(ent);
            getLog().info("Removed obj entity: " + ent.getName());
        }
    }

    /** Loads and returns DataMap based on <code>map</code> attribute. */
    protected DataMap loadDataMap() throws Exception {
        final InputSource in = new InputSource(map.getCanonicalPath());
        return new MapLoader().loadDataMap(in);
    }
}
