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
import org.apache.cayenne.datasource.DataSourceBuilder;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
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

    @Input
    @Optional
    private String adapter;

    @Internal
    private DataSourceConfig dataSource = new DataSourceConfig();

    @Input
    private boolean dropTables;

    @Input
    private boolean dropPK;

    @Input
    private boolean createTables = true;

    @Input
    private boolean createPK = true;

    @Input
    private boolean createFK = true;

    @InputFile
    public File getDataMapFile() {
        return super.getDataMapFile();
    }

    @TaskAction
    public void generateDb() throws GradleException {

        dataSource.validate();

        getLogger().info("connection settings - [driver: {}, url: {}, username: {}]",
                dataSource.getDriver(), dataSource.getUrl(), dataSource.getUsername());

        getLogger().info("generator options - " +
                        "[dropTables: {}, dropPK: {}, createTables: {}, createPK: {}, createFK: {}]",
                dropTables, dropPK, createTables, createPK, createFK);

        Injector injector = DIBootstrap.createInjector(new DbSyncModule(), new ToolsModule(getLogger()));

        try {
            DbGenerator generator = createGenerator(loadDataMap(injector));
            generator.runGenerator(createDataSource());
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

    DbGenerator createGenerator(DataMap dataMap) {
        DbGenerator generator = new DbGenerator(createDbAdapter(), dataMap, NoopJdbcEventLogger.getInstance());
        generator.setShouldCreateFKConstraints(createFK);
        generator.setShouldCreatePKSupport(createPK);
        generator.setShouldCreateTables(createTables);
        generator.setShouldDropPKSupport(dropPK);
        generator.setShouldDropTables(dropTables);
        return generator;
    }

    DbAdapter createDbAdapter() {
        Injector injector = DIBootstrap.createInjector(new DbSyncModule(), new ToolsModule(getLogger()));
        AdhocObjectFactory objectFactory = injector.getInstance(AdhocObjectFactory.class);

        return (adapter == null)
                ? objectFactory.newInstance(DbAdapter.class, JdbcAdapter.class.getName())
                : objectFactory.newInstance(DbAdapter.class, adapter);
    }

    DataSource createDataSource() {
        return DataSourceBuilder.url(dataSource.getUrl())
                .driver(dataSource.getDriver())
                .userName(dataSource.getUsername())
                .password(dataSource.getPassword())
                .build();
    }

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