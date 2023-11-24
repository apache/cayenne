/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.tools;

import org.apache.cayenne.access.DbGenerator;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.runtime.DbAdapterFactory;
import org.apache.cayenne.datasource.DriverDataSource;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbsync.DbSyncModule;
import org.apache.cayenne.di.ClassLoaderManager;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.log.NoopJdbcEventLogger;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.dbsync.reverse.configuration.ToolsModule;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.util.Util;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.sql.Driver;
import javax.sql.DataSource;

/**
 * Maven mojo that generates database schema based on Cayenne mapping.
 * It is a logical counterpart of cdbimport mojo.
 *
 * @since 3.0
 */
@Mojo(name = "cdbgen", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.COMPILE)
public class DbGeneratorMojo extends AbstractMojo {

    /**
     * DataMap XML file to use as a schema descriptor.
     */
    @Parameter(required = true)
    private File map;

    /**
     * Java class implementing org.apache.cayenne.dba.DbAdapter. While this
     * attribute is optional (a generic JdbcAdapter is used if not set), it is
     * highly recommended to specify correct target adapter.
     */
    @Parameter
    private String adapter;

    /**
     * Connection properties.
     *
     * @see DbImportDataSourceConfig
     * @since 4.0
     */
    @Parameter
    private DbImportDataSourceConfig dataSource = new DbImportDataSourceConfig();

    /**
     * Defines whether cdbgen should drop the tables before attempting to create
     * new ones. Default is <code>false</code>.
     */
    @Parameter(defaultValue = "false")
    private boolean dropTables;

    /**
     * Defines whether cdbgen should drop Cayenne primary key support objects.
     * Default is <code>false</code>.
     */
    @Parameter(defaultValue = "false")
    private boolean dropPK;

    /**
     * Defines whether cdbgen should create new tables. Default is
     * <code>true</code>.
     */
    @Parameter(defaultValue = "true")
    private boolean createTables;

    /**
     * Defines whether cdbgen should create Cayenne-specific auto PK objects.
     * Default is <code>true</code>.
     */
    @Parameter(defaultValue = "true")
    private boolean createPK;

    /**
     * Defines whether cdbgen should create foreign key copnstraints. Default is
     * <code>true</code>.
     */
    @Parameter(defaultValue = "true")
    private boolean createFK;

    @Parameter(defaultValue = "${project}")
    private MavenProject project;

    public void execute() throws MojoExecutionException, MojoFailureException {

        Logger logger = new MavenLogger(this);

        // check missing data source parameters
        dataSource.validate();

        Injector injector = DIBootstrap.createInjector(new DbSyncModule(), new ToolsModule(logger),
                binder -> binder.bind(ClassLoaderManager.class).toInstance(new MavenPluginClassLoaderManager(project)));

        logger.info(String.format("connection settings - [driver: %s, url: %s, username: %s]",
                dataSource.getDriver(), dataSource.getUrl(), dataSource.getUsername()));

        logger.info(String.format(
                "generator options - [dropTables: %s, dropPK: %s, createTables: %s, createPK: %s, createFK: %s]",
                dropTables, dropPK, createTables, createPK, createFK));

        try {
            // load driver taking custom CLASSPATH into account...
            DriverDataSource driverDataSource = new DriverDataSource(
                    (Driver) Class.forName(dataSource.getDriver()).getDeclaredConstructor().newInstance(),
                    dataSource.getUrl(), dataSource.getUsername(), dataSource.getPassword()
            );

            final DbAdapter dbAdapter = createDbAdapter(injector, driverDataSource);

            // Load the data map and run the db generator.
            DataMap dataMap = loadDataMap(injector);
            DbGenerator generator = new DbGenerator(dbAdapter, dataMap, NoopJdbcEventLogger.getInstance());
            generator.setShouldCreateFKConstraints(createFK);
            generator.setShouldCreatePKSupport(createPK);
            generator.setShouldCreateTables(createTables);
            generator.setShouldDropPKSupport(dropPK);
            generator.setShouldDropTables(dropTables);

            generator.runGenerator(driverDataSource);
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

    private DbAdapter createDbAdapter(Injector injector, DataSource dataSource) throws Exception {
        DbAdapterFactory adapterFactory = injector.getInstance(DbAdapterFactory.class);

        DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor();
        nodeDescriptor.setAdapterType(adapter);

        return adapterFactory.createAdapter(nodeDescriptor, dataSource);
    }

    /**
     * Loads and returns DataMap based on <code>map</code> attribute.
     */
    private DataMap loadDataMap(Injector injector) throws Exception {
        return injector.getInstance(DataMapLoader.class).load(new URLResource(map.toURI().toURL()));
    }

}
