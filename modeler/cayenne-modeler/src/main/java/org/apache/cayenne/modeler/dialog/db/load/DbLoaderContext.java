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

package org.apache.cayenne.modeler.dialog.db.load;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.File;
import java.sql.Connection;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.dbsync.reverse.dbimport.DbImportConfiguration;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.reverse.dbload.DbLoaderDelegate;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfigBuilder;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ClassLoadingService;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.editor.dbimport.DbImportView;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.0
 */
public class DbLoaderContext {

    private static Logger LOGGER = LoggerFactory.getLogger(DbLoaderContext.class);

    private DbImportConfiguration config;
    private Connection connection;
    private ProjectController projectController;
    private boolean existingMap;
    private DataMap dataMap;
    private boolean stopping;
    private String loadStatusNote;
    private volatile boolean isInterrupted;

    private DataChannelMetaData metaData;

    public DbLoaderContext(DataChannelMetaData metaData) {
        this.metaData = metaData;
    }

    DataMap getDataMap() {
        return dataMap;
    }

    boolean isExistingDataMap() {
        return existingMap;
    }

    public void setProjectController(ProjectController projectController) {
        this.projectController = projectController;
    }

    ProjectController getProjectController() {
        return projectController;
    }

    void setConfig(DbImportConfiguration config) {
        this.config = config;
    }

    DbImportConfiguration getConfig() {
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

    public DataChannelMetaData getMetaData() {
        return metaData;
    }

    void setStopping(boolean stopping) {
        this.stopping = stopping;
    }

    String getStatusNote() {
        return loadStatusNote;
    }

    void setStatusNote(String loadStatusNote) {
        this.loadStatusNote = loadStatusNote;
    }

    private void fillReverseEngineeringFromView(ReverseEngineering reverseEngineering, DbImportView view) {
        reverseEngineering.setUsePrimitives(view.isUsePrimitives());
        reverseEngineering.setUseJava7Types(view.isUseJava7Typed());
        reverseEngineering.setForceDataMapCatalog(view.isForceDataMapCatalog());
        reverseEngineering.setForceDataMapSchema(view.isForceDataMapSchema());
        reverseEngineering.setSkipRelationshipsLoading(view.isSkipRelationshipsLoading());
        reverseEngineering.setSkipPrimaryKeyLoading(view.isSkipPrimaryKeyLoading());
        reverseEngineering.setMeaningfulPkTables(view.getMeaningfulPk());
        reverseEngineering.setNamingStrategy(view.getNamingStrategy());
        reverseEngineering.setStripFromTableNames(view.getStripFromTableNames());
    }

    public boolean buildConfig(DBConnectionInfo connectionInfo, DbImportView view, boolean headless) {
        if (connectionInfo == null) {
            return false;
        }
        // Build reverse engineering from metadata and dialog values
        ReverseEngineering metaReverseEngineering = metaData.get(getProjectController().getCurrentDataMap(), ReverseEngineering.class);
        if(metaReverseEngineering == null) {
            return false;
        }
        // skip this step for batch run from domain tab
        if(!headless){
            fillReverseEngineeringFromView(metaReverseEngineering, view);
        }
        // Create copy of metaReverseEngineering
        ReverseEngineering reverseEngineering = new ReverseEngineering(metaReverseEngineering);

        DbImportConfiguration config = new DbImportConfiguration() {
            @Override
            public DbLoaderDelegate createLoaderDelegate() {
                return new LoaderDelegate(DbLoaderContext.this);
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
            ClassLoadingService classLoadingService = Application.getInstance().getClassLoadingService();
            config.getDbLoaderConfig().setFiltersConfig(new FiltersConfigBuilder(reverseEngineering)
                    .dataSource(connectionInfo.makeDataSource(classLoadingService))
                    .dbAdapter(connectionInfo.makeAdapter(classLoadingService))
                    .build());
        } catch (Exception e) {
            processException(e, "Fail while building configs.");
        }
        config.setMeaningfulPkTables(reverseEngineering.getMeaningfulPkTables());
        config.setNamingStrategy(reverseEngineering.getNamingStrategy());
        config.setDefaultPackage(reverseEngineering.getDefaultPackage());
        config.setStripFromTableNames(reverseEngineering.getStripFromTableNames());
        config.setUsePrimitives(reverseEngineering.isUsePrimitives());
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
        dataMap = getProjectController().getCurrentDataMap();
        existingMap = dataMap != null;

        if (!existingMap) {
            ConfigurationNode root = getProjectController().getProject().getRootNode();
            dataMap = new DataMap();
            dataMap.setName(NameBuilder.builder(dataMap, root).name());
        }
        if (dataMap.getConfigurationSource() != null) {
            getConfig().setTargetDataMap(new File(dataMap.getConfigurationSource().getURL().getPath()));
        }
    }

    public void processWarn(final Throwable th, final String message) {
        LOGGER.warn(message, Util.unwindException(th));
    }

    public void processException(final Throwable th, final String message) {
        LOGGER.info("Exception on reverse engineering", Util.unwindException(th));
        isInterrupted = true;
        SwingUtilities.invokeLater(() -> JOptionPane
                .showMessageDialog(Application.getFrame(), th.getMessage(), message, JOptionPane.ERROR_MESSAGE));
    }

    public boolean isInterrupted() {
        return isInterrupted;
    }
}
