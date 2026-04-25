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

package org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.runner;

import org.apache.cayenne.dbsync.reverse.dbimport.DbImportConfiguration;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.reverse.dbload.DbLoaderDelegate;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfigBuilder;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.service.classloader.ModelerClassLoader;
import org.apache.cayenne.modeler.ui.project.ProjectController;
import org.apache.cayenne.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.sql.Connection;

public class ModelerDbLoaderContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelerDbLoaderContext.class);

    private final ProjectController projectController;
    private final DataMap dataMap;
    private final Application application;

    private DbImportConfiguration config;
    private Connection connection;
    private boolean stopping;
    private String loadStatusNote;
    private volatile boolean isInterrupted;

    public ModelerDbLoaderContext(ProjectController projectController, Application application, DataMap dataMap) {
        this.projectController = projectController;
        this.application = application;
        this.dataMap = dataMap;
    }

    public DataMap getDataMap() {
        return dataMap;
    }

    public ProjectController getProjectController() {
        return projectController;
    }

    public Application getApplication() {
        return application;
    }

    void setConfig(DbImportConfiguration config) {
        this.config = config;
    }

    public DbImportConfiguration getConfig() {
        return config;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return connection;
    }

    public boolean isStopping() {
        return stopping;
    }

    public void setStopping(boolean stopping) {
        this.stopping = stopping;
    }

    public String getStatusNote() {
        return loadStatusNote;
    }

    public void setStatusNote(String loadStatusNote) {
        this.loadStatusNote = loadStatusNote;
    }

    public boolean buildConfig(DBConnectionInfo connectionInfo) {
        if (connectionInfo == null) {
            return false;
        }
        ReverseEngineering metaReverseEngineering = application.getMetaData().get(dataMap, ReverseEngineering.class);
        if(metaReverseEngineering == null) {
            return false;
        }
        // Create copy of metaReverseEngineering
        ReverseEngineering reverseEngineering = new ReverseEngineering(metaReverseEngineering);

        DbImportConfiguration config = new DbImportConfiguration() {
            @Override
            public DbLoaderDelegate createLoaderDelegate() {
                return new ModelerDbLoaderDelegate(ModelerDbLoaderContext.this);
            }
        };
        fillConfig(config, connectionInfo, reverseEngineering);
        setConfig(config);

        prepareDataMap();

        return true;
    }

    // Fill config from metadata reverseEngineering
    private void fillConfig(DbImportConfiguration config, DBConnectionInfo connectionInfo,
                            ReverseEngineering reverseEngineering) {
        config.setAdapter(connectionInfo.getDbAdapter());
        config.setUsername(connectionInfo.getUserName());
        config.setPassword(connectionInfo.getPassword());
        config.setDriver(connectionInfo.getJdbcDriver());
        config.setUrl(connectionInfo.getUrl());

        try {
            ModelerClassLoader classLoader = application.getClassLoader();
            config.getDbLoaderConfig().setFiltersConfig(new FiltersConfigBuilder(reverseEngineering)
                    .dataSource(connectionInfo.makeDataSource(classLoader))
                    .dbAdapter(connectionInfo.makeAdapter(classLoader))
                    .build());
        } catch (Exception e) {
            processException(e, "Fail while building configs.");
        }
        config.setMeaningfulPkTables(reverseEngineering.getMeaningfulPkTables());
        config.setNamingStrategy(reverseEngineering.getNamingStrategy());
        config.setDefaultPackage(reverseEngineering.getDefaultPackage());
        config.setStripFromTableNames(reverseEngineering.getStripFromTableNames());
        config.setUseJava7Types(reverseEngineering.isUseJava7Types());
        config.setForceDataMapCatalog(reverseEngineering.isForceDataMapCatalog());
        config.setForceDataMapSchema(reverseEngineering.isForceDataMapSchema());
        config.setSkipRelationshipsLoading(reverseEngineering.getSkipRelationshipsLoading());
        config.setSkipPrimaryKeyLoading(reverseEngineering.getSkipPrimaryKeyLoading());
        String[] tableTypes = reverseEngineering.getTableTypes();
        if(tableTypes.length != 0) {
            config.setTableTypes(tableTypes);
        } else {
            config.setTableTypes(new String[]{"TABLE", "VIEW"});
        }
    }

    private void prepareDataMap() {
        if (this.dataMap.getConfigurationSource() != null) {
            getConfig().setTargetDataMap(new File(dataMap.getConfigurationSource().getURL().getPath()));
        }
    }

    public void processException(final Throwable th, final String message) {
        LOGGER.info("Exception on reverse engineering", Util.unwindException(th));
        isInterrupted = true;
        SwingUtilities.invokeLater(() -> JOptionPane
                .showMessageDialog(projectController.getApplication().getFrameController().getView(), th.getMessage(), message, JOptionPane.ERROR_MESSAGE));
    }

    public boolean isInterrupted() {
        return isInterrupted;
    }
}
