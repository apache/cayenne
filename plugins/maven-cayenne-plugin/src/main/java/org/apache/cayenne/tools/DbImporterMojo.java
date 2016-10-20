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

import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.XMLDataMapLoader;
import org.apache.cayenne.configuration.server.DataSourceFactory;
import org.apache.cayenne.configuration.server.DbAdapterFactory;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbimport.DefaultReverseEngineeringLoader;
import org.apache.cayenne.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.CayenneDbSyncModule;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfigBuilder;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.tools.configuration.ToolsModule;
import org.apache.cayenne.tools.dbimport.DbImportAction;
import org.apache.cayenne.tools.dbimport.DbImportConfiguration;
import org.apache.cayenne.tools.dbimport.DbImportModule;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import javax.sql.DataSource;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Maven mojo to reverse engineer datamap from DB.
 *
 * @phase generate-sources
 * @goal cdbimport
 * @since 3.0
 */
public class DbImporterMojo extends AbstractMojo {

    /**
     * Java class implementing org.apache.cayenne.dba.DbAdapter. This attribute
     * is optional, the default is AutoAdapter, i.e. Cayenne would try to guess
     * the DB type.
     *
     * @parameter adapter="adapter"
     * default-value="org.apache.cayenne.dba.AutoAdapter"
     */
    private String adapter;

    /**
     * A default package for ObjEntity Java classes. If not specified, and the
     * existing DataMap already has the default package, the existing package
     * will be used.
     *
     * @parameter defaultPackage="defaultPackage"
     * @since 4.0
     */
    private String defaultPackage;

    /**
     * A class of JDBC driver to use for the target database.
     *
     * @parameter driver="driver"
     * @required
     */
    private String driver;

    /**
     * DataMap XML file to use as a base for DB importing.
     *
     * @parameter map="map"
     * @required
     */
    private File map;

    /**
     * A comma-separated list of Perl5 patterns that defines which imported tables should have their primary key columns
     * mapped as ObjAttributes. "*" would indicate all tables.
     *
     * @parameter meaningfulPkTables="meaningfulPkTables"
     * @since 4.0
     */
    private String meaningfulPkTables;

    /**
     * Object layer naming generator implementation. Should be fully qualified Java class name implementing
     * "org.apache.cayenne.dbsync.naming.ObjectNameGenerator". The default is
     * "org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator".
     *
     * @parameter namingStrategy="namingStrategy"
     * default-value="org.apache.cayenne.dbsync.naming.DefaultObjectNameGenerator"
     */
    private String namingStrategy;

    /**
     * Database user password.
     *
     * @parameter password="password"
     */
    private String password;

    /**
     * An object that contains reverse engineering rules.
     *
     * @parameter reverseEngineering="reverseEngineering"
     */
    private ReverseEngineering reverseEngineering = new ReverseEngineering();


    /**
     * JDBC connection URL of a target database.
     *
     * @parameter url="url"
     * @required
     */
    private String url;

    /**
     * If true, would use primitives instead of numeric and boolean classes.
     *
     * @parameter usePrimitives="usePrimitives" default-value="true"
     */
    private boolean usePrimitives;

    /**
     * Database user name.
     *
     * @parameter username="username"
     */
    private String username;

    public void execute() throws MojoExecutionException, MojoFailureException {

        Log logger = new MavenLogger(this);

        DbImportConfiguration config = toParameters();
        config.setLogger(logger);
        Injector injector = DIBootstrap.createInjector(new CayenneDbSyncModule(), new ToolsModule(logger), new DbImportModule());

        validateDbImportConfiguration(config, injector);

        try {
            injector.getInstance(DbImportAction.class).execute(config);
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

    private void validateDbImportConfiguration(DbImportConfiguration config, Injector injector) throws MojoExecutionException {
        DataNodeDescriptor dataNodeDescriptor = config.createDataNodeDescriptor();
        DataSource dataSource = null;
        DbAdapter adapter = null;

        try {
            dataSource = injector.getInstance(DataSourceFactory.class).getDataSource(dataNodeDescriptor);
            adapter = injector.getInstance(DbAdapterFactory.class).createAdapter(dataNodeDescriptor, dataSource);

            if (!adapter.supportsCatalogsOnReverseEngineering() &&
                    reverseEngineering.getCatalogs() != null && !reverseEngineering.getCatalogs().isEmpty()) {
                String message = "Your database does not support catalogs on reverse engineering. " +
                        "It allows to connect to only one at the moment. Please don't note catalogs as param.";
                throw new MojoExecutionException(message);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error creating DataSource ("
                    + dataSource + ") or DbAdapter (" + adapter + ") for DataNodeDescriptor (" + dataNodeDescriptor + ")", e);
        }
    }

    DbImportConfiguration toParameters() {

        DbImportConfiguration config = new DbImportConfiguration();
        config.setAdapter(adapter);
        config.setDefaultPackage(defaultPackage);
        config.setDriver(driver);
        config.setDataMapFile(map);
        config.setMeaningfulPkTables(meaningfulPkTables);
        config.setNamingStrategy(namingStrategy);
        config.setPassword(password);
        config.setUrl(url);
        config.setUsername(username);
        config.setUsePrimitives(usePrimitives);
        config.setFiltersConfig(new FiltersConfigBuilder(reverseEngineering).build());
        config.setSkipRelationshipsLoading(reverseEngineering.getSkipRelationshipsLoading());
        config.setSkipPrimaryKeyLoading(reverseEngineering.getSkipPrimaryKeyLoading());
        config.setTableTypes(reverseEngineering.getTableTypes());

        return config;
    }

    public File getMap() {
        return map;
    }

    public void setMap(File map) {
        this.map = map;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public ReverseEngineering getReverseEngineering() {
        return reverseEngineering;
    }
}


