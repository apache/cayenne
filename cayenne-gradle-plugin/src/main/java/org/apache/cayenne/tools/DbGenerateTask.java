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

import groovy.lang.Closure;
import org.apache.cayenne.access.DbGenerator;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.runtime.DataSourceFactory;
import org.apache.cayenne.configuration.runtime.DbAdapterFactory;
import org.apache.cayenne.configuration.runtime.PkGeneratorFactoryProvider;
import org.apache.cayenne.datasource.DataSourceBuilder;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.dbsync.DbSyncModule;
import org.apache.cayenne.dbsync.reverse.configuration.ToolsModule;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.log.NoopJdbcEventLogger;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.tools.model.DataSourceConfig;
import org.apache.cayenne.util.Util;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import javax.sql.DataSource;

/**
 * @since 4.0
 */
public class DbGenerateTask extends BaseCayenneTask {

    /**
     * Java class implementing org.apache.cayenne.dba.DbAdapter. While this
     * attribute is optional (a generic JdbcAdapter is used if not set), it is
     * highly recommended to specify correct target adapter.
     */
    @Input
    @Optional
    private String adapter;

    /**
     * Connection properties.
     *
     * @since 4.0
     */
    @Internal
    private DataSourceConfig dataSource = new DataSourceConfig();

    /**
     * Defines whether cdbgen should drop the tables before attempting to create
     * new ones. Default is <code>false</code>.
     */
    @Input
    private boolean dropTables;

    /**
     * Defines whether cdbgen should drop Cayenne primary key support objects.
     * Default is <code>false</code>.
     */
    @Input
    private boolean dropPK;

    /**
     * Defines whether cdbgen should create new tables. Default is
     * <code>true</code>.
     */
    @Input
    private boolean createTables = true;

    /**
     * Defines whether cdbgen should create Cayenne-specific auto PK objects.
     * Default is <code>true</code>.
     */
    @Input
    private boolean createPK = true;

    /**
     * Defines whether cdbgen should create foreign key copnstraints. Default is
     * <code>true</code>.
     */
    @Input
    private boolean createFK = true;

    @InputFile
    public File getDataMapFile() {
        return super.getDataMapFile();
    }

    @TaskAction
    public void generateDb() throws GradleException {

        // check missing data source parameter
        dataSource.validate();

        getLogger().info("connection settings - [driver: {}, url: {}, username: {}]",
                dataSource.getDriver(), dataSource.getUrl(), dataSource.getUsername());

        getLogger().info("generator options - " +
                        "[dropTables: {}, dropPK: {}, createTables: {}, createPK: {}, createFK: {}]",
                dropTables, dropPK, createTables, createPK, createFK);

        Injector injector = DIBootstrap.createInjector(new DbSyncModule(), new ToolsModule(getLogger()));

        try {
            DataSource realDataSource = createDataSource();
            DataMap dataMap = loadDataMap(injector);
            DbGenerator generator = createGenerator(dataMap, injector, realDataSource);
            generator.runGenerator(realDataSource);
        } catch (Exception ex) {
            Throwable th = Util.unwindException(ex);
            String message = "Error generating database";
            if (th.getLocalizedMessage() != null) {
                message += ": " + th.getLocalizedMessage();
            }

            getLogger().error(message);
            throw new GradleException(message, th);
        }
    }

    DbGenerator createGenerator(DataMap dataMap, Injector injector, DataSource realDataSource) throws Exception {
        DbAdapter dbAdapter = createDbAdapter(injector, realDataSource);

        DbGenerator generator = new DbGenerator(dbAdapter, dataMap, NoopJdbcEventLogger.getInstance());
        generator.setShouldCreateFKConstraints(createFK);
        generator.setShouldCreatePKSupport(createPK);
        generator.setShouldCreateTables(createTables);
        generator.setShouldDropPKSupport(dropPK);
        generator.setShouldDropTables(dropTables);
        return generator;
    }

    DbAdapter createDbAdapter(Injector injector, DataSource realDataSource) throws Exception {
        DbAdapterFactory adapterFactory = injector.getInstance(DbAdapterFactory.class);
        DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor();
        nodeDescriptor.setAdapterType(adapter);

        return adapterFactory.createAdapter(nodeDescriptor, realDataSource);
    }

    DataSource createDataSource() {
        return DataSourceBuilder.url(dataSource.getUrl())
                .driver(dataSource.getDriver())
                .userName(dataSource.getUsername())
                .password(dataSource.getPassword())
                .build();
    }

    /**
     * Loads and returns DataMap based on <code>map</code> attribute.
     */
    DataMap loadDataMap(Injector injector) throws Exception {
        File dataMapFile = getDataMapFile();
        return injector.getInstance(DataMapLoader.class).load(new URLResource(dataMapFile.toURI().toURL()));
    }

    // setters and getters that will be used by .gradle scripts

    public String getAdapter() {
        return adapter;
    }

    public void setAdapter(String adapter) {
        this.adapter = adapter;
    }

    public void adapter(String adapter) {
        setAdapter(adapter);
    }

    public DataSourceConfig getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSourceConfig dataSource) {
        this.dataSource = dataSource;
    }

    public DataSourceConfig dataSource(Closure closure) {
        dataSource = new DataSourceConfig();
        getProject().configure(dataSource, closure);
        return dataSource;
    }

    public boolean isDropTables() {
        return dropTables;
    }

    public void setDropTables(boolean dropTables) {
        this.dropTables = dropTables;
    }

    public void dropTables(boolean dropTables) {
        setDropTables(dropTables);
    }

    public boolean isDropPK() {
        return dropPK;
    }

    public void setDropPK(boolean dropPK) {
        this.dropPK = dropPK;
    }

    public void dropPK(boolean dropPK) {
        setDropPK(dropPK);
    }

    public boolean isCreateTables() {
        return createTables;
    }

    public void setCreateTables(boolean createTables) {
        this.createTables = createTables;
    }

    public void createTables(boolean createTables) {
        setCreateTables(createTables);
    }

    public boolean isCreatePK() {
        return createPK;
    }

    public void setCreatePK(boolean createPK) {
        this.createPK = createPK;
    }

    public void createPK(boolean createPK) {
        setCreatePK(createPK);
    }

    public boolean isCreateFK() {
        return createFK;
    }

    public void setCreateFK(boolean createFK) {
        this.createFK = createFK;
    }

    public void createFK(boolean createFK) {
        setCreateFK(createFK);
    }
}