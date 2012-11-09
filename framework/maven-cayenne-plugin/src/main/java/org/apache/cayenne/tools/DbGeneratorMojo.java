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
import java.sql.Driver;

import org.apache.cayenne.access.DbGenerator;
import org.apache.cayenne.conn.DriverDataSource;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.log.NoopJdbcEventLogger;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.tools.configuration.ToolsModule;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.xml.sax.InputSource;

/**
 * Maven mojo to perform class generation from data map. This class is a Maven
 * adapter to DefaultClassGenerator class.
 * 
 * @since 3.0
 * 
 * @phase pre-integration-test
 * @goal cdbgen
 */
public class DbGeneratorMojo extends AbstractMojo {

    /**
     * DataMap XML file to use as a schema descriptor.
     * 
     * @parameter expression="${cdbgen.map}"
     * @required
     */
    private File map;

    /**
     * Java class implementing org.apache.cayenne.dba.DbAdapter. While this
     * attribute is optional (a generic JdbcAdapter is used if not set), it is
     * highly recommended to specify correct target adapter.
     * 
     * @parameter expression="${cdbgen.adapter}"
     */
    private String adapter;

    /**
     * A class of JDBC driver to use for the target database.
     * 
     * @parameter expression="${cdbgen.driver}"
     * @required
     */
    private String driver;

    /**
     * JDBC connection URL of a target database.
     * 
     * @parameter expression="${cdbgen.url}"
     * @required
     */
    private String url;

    /**
     * Database user name.
     * 
     * @parameter expression="${cdbgen.username}"
     */
    private String username;

    /**
     * Database user password.
     * 
     * @parameter expression="${cdbgen.password}"
     */
    private String password;

    /**
     * Defines whether cdbgen should drop the tables before attempting to create
     * new ones. Default is <code>false</code>.
     * 
     * @parameter expression="${cdbgen.dropTables}" default-value="false"
     */
    private boolean dropTables;

    /**
     * Defines whether cdbgen should drop Cayenne primary key support objects.
     * Default is <code>false</code>.
     * 
     * @parameter expression="${cdbgen.dropPK}" default-value="false"
     */
    private boolean dropPK;

    /**
     * Defines whether cdbgen should create new tables. Default is
     * <code>true</code>.
     * 
     * @parameter expression="${cdbgen.createTables}" default-value="true"
     */
    private boolean createTables;

    /**
     * Defines whether cdbgen should create Cayenne-specific auto PK objects.
     * Default is <code>true</code>.
     * 
     * @parameter expression="${cdbgen.createPK}" default-value="true"
     */
    private boolean createPK;

    /**
     * Defines whether cdbgen should create foreign key copnstraints. Default is
     * <code>true</code>.
     * 
     * @parameter expression="${cdbgen.createFK}' default-value="true"
     */
    private boolean createFK;

    public void execute() throws MojoExecutionException, MojoFailureException {

        Log logger = new MavenLogger(this);
        Injector injector = DIBootstrap.createInjector(new ToolsModule(logger));
        AdhocObjectFactory objectFactory = injector.getInstance(AdhocObjectFactory.class);

        logger.info(String.format("connection settings - [driver: %s, url: %s, username: %s]", driver, url, username));

        logger.info(String.format(
                "generator options - [dropTables: %s, dropPK: %s, createTables: %s, createPK: %s, createFK: %s]",
                dropTables, dropPK, createTables, createPK, createFK));

        try {
            final DbAdapter adapterInst = (adapter == null) ? (DbAdapter) objectFactory.newInstance(DbAdapter.class,
                    JdbcAdapter.class.getName()) : (DbAdapter) objectFactory.newInstance(DbAdapter.class, adapter);

            // Load the data map and run the db generator.
            DataMap dataMap = loadDataMap();
            DbGenerator generator = new DbGenerator(adapterInst, dataMap, NoopJdbcEventLogger.getInstance());
            generator.setShouldCreateFKConstraints(createFK);
            generator.setShouldCreatePKSupport(createPK);
            generator.setShouldCreateTables(createTables);
            generator.setShouldDropPKSupport(dropPK);
            generator.setShouldDropTables(dropTables);

            // load driver taking custom CLASSPATH into account...
            DriverDataSource dataSource = new DriverDataSource((Driver) Class.forName(driver).newInstance(), url,
                    username, password);

            generator.runGenerator(dataSource);
        } catch (Exception ex) {
            Throwable th = Util.unwindException(ex);

            String message = "Error generating database";

            if (th.getLocalizedMessage() != null) {
                message += ": " + th.getLocalizedMessage();
            }

            logger.error(message);
            throw new MojoExecutionException(message, th);
        }
    }

    /** Loads and returns DataMap based on <code>map</code> attribute. */
    protected DataMap loadDataMap() throws Exception {
        InputSource in = new InputSource(map.getCanonicalPath());
        return new MapLoader().loadDataMap(in);
    }

}
