/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */

package org.apache.cayenne.modeler.dialog.db;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.CayenneDbSyncModule;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfigBuilder;
import org.apache.cayenne.dbsync.reverse.db.DbLoader;
import org.apache.cayenne.dbsync.reverse.db.DefaultDbLoaderDelegate;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.util.LongRunningTask;
import org.apache.cayenne.tools.configuration.ToolsModule;
import org.apache.cayenne.tools.dbimport.DbImportConfiguration;
import org.apache.cayenne.tools.dbimport.DbImportModule;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Stateful helper class that encapsulates access to DbLoader.
 */
public class DbLoaderHelper {

    // TODO: this is a temp hack... need to delegate to DbAdapter, or
    // configurable in preferences...
    private static final Collection<String> EXCLUDED_TABLES = Arrays.asList("AUTO_PK_SUPPORT", "auto_pk_support");

    private static Log LOGGER = LogFactory.getLog(DbLoaderHelper.class);

    protected boolean stoppingReverseEngineering;
    protected boolean existingMap;

    protected ProjectController mediator;
    protected String dbCatalog;
    protected DbLoader loader;
    protected DataMap dataMap;
    protected List<String> schemas;
    protected List<String> catalogs;
    protected DbAdapter adapter;
    protected DbImportConfiguration config;
    protected ReverseEngineering reverseEngineering;
    protected String loadStatusNote;

    /**
     * ObjEntities which were added to project during reverse engineering
     */
    protected List<ObjEntity> addedObjEntities;

    public DbLoaderHelper(ProjectController mediator, Connection connection, DbAdapter adapter,
                          DBConnectionInfo dbConnectionInfo, ReverseEngineering reverseEngineering) {
        this.mediator = mediator;
        try {
            this.dbCatalog = connection.getCatalog();
        } catch (SQLException e) {
            LOGGER.warn("Error getting catalog.", e);
        }
        this.adapter = adapter;
        this.reverseEngineering = reverseEngineering;

        this.config = new DbImportConfiguration();
        this.config.setAdapter(adapter.getClass().getName());
        this.config.setUsername(dbConnectionInfo.getUserName());
        this.config.setPassword(dbConnectionInfo.getPassword());
        this.config.setDriver(dbConnectionInfo.getJdbcDriver());
        this.config.setUrl(dbConnectionInfo.getUrl());
        try {
            this.dbCatalog = connection.getCatalog();
        } catch (SQLException e) {
            LOGGER.warn("Error getting catalog.", e);
        }
        try {
            this.loader = config.createLoader(adapter, connection, new LoaderDelegate());
        } catch (Throwable th) {
            processException(th, "Error creating DbLoader.");
        }
    }

    public boolean isStoppingReverseEngineering() {
        return stoppingReverseEngineering;
    }

    public void setStoppingReverseEngineering(boolean stopReverseEngineering) {
        this.stoppingReverseEngineering = stopReverseEngineering;
    }

    public DataMap getDataMap() {
        return dataMap;
    }

    /**
     * Performs reverse engineering of the DB using internal DbLoader. This
     * method should be invoked outside EventDispatchThread, or it will throw an
     * exception.
     */
    public void execute() {
        stoppingReverseEngineering = false;

        // load catalogs...
        if (adapter.supportsCatalogsOnReverseEngineering()) {
            LongRunningTask loadCatalogsTask = new LoadCatalogsTask(Application.getFrame(), "Loading Catalogs");
            loadCatalogsTask.startAndWait();
        }

        if (stoppingReverseEngineering) {
            return;
        }

        // load schemas...
        LongRunningTask loadSchemasTask = new LoadSchemasTask(Application.getFrame(), "Loading Schemas");
        loadSchemasTask.startAndWait();

        if (stoppingReverseEngineering) {
            return;
        }

        LongRunningTask loadDataMapTask = new LoadDataMapTask(Application.getFrame(), "Reengineering DB");
        loadDataMapTask.startAndWait();
    }

    protected void processException(final Throwable th, final String message) {
        LOGGER.info("Exception on reverse engineering", Util.unwindException(th));
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                JOptionPane.showMessageDialog(Application.getFrame(), th.getMessage(), message,
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    protected ProjectController getMediator() {
        return mediator;
    }

    protected DbLoader getLoader() {
        return loader;
    }

    private final class LoaderDelegate extends DefaultDbLoaderDelegate {

        @Override
        public void dbEntityAdded(DbEntity entity) {
            checkCanceled();

            loadStatusNote = "Importing table '" + entity.getName() + "'...";

            // TODO: hack to prevent PK tables from being visible... this should
            // really be
            // delegated to DbAdapter to decide...
            if (EXCLUDED_TABLES.contains(entity.getName()) && entity.getDataMap() != null) {
                entity.getDataMap().removeDbEntity(entity.getName());
            } else if (existingMap) {
                mediator.fireDbEntityEvent(new EntityEvent(this, entity, MapEvent.ADD));
            }
        }

        @Override
        public void objEntityAdded(ObjEntity entity) {
            checkCanceled();

            loadStatusNote = "Creating ObjEntity '" + entity.getName() + "'...";

            if (existingMap) {
                mediator.fireObjEntityEvent(new EntityEvent(this, entity, MapEvent.ADD));
            }
        }

        @Override
        public void dbEntityRemoved(DbEntity entity) {
            checkCanceled();

            if (existingMap) {
                mediator.fireDbEntityEvent(new EntityEvent(Application.getFrame(), entity, MapEvent.REMOVE));
            }
        }

        @Override
        public void objEntityRemoved(ObjEntity entity) {
            checkCanceled();

            if (existingMap) {
                mediator.fireObjEntityEvent(new EntityEvent(Application.getFrame(), entity, MapEvent.REMOVE));
            }
        }

        @Override
        public boolean dbRelationship(DbEntity entity) {
            checkCanceled();

            loadStatusNote = "Load relationships for '" + entity.getName() + "'...";

            return true;
        }

        @Override
        public boolean dbRelationshipLoaded(DbEntity entity, DbRelationship relationship) {
            checkCanceled();

            loadStatusNote = "Load relationship: '" + entity.getName() + "'; '" + relationship.getName() + "'...";

            return true;
        }

        void checkCanceled() {
            if (isStoppingReverseEngineering()) {
                throw new CayenneRuntimeException("Reengineering was canceled.");
            }
        }
    }

    abstract class DbLoaderTask extends LongRunningTask {

        public DbLoaderTask(JFrame frame, String title) {
            super(frame, title);
            setMinValue(0);
            setMaxValue(10);
        }

        @Override
        protected String getCurrentNote() {
            return loadStatusNote;
        }

        @Override
        protected int getCurrentValue() {
            return getMinValue();
        }

        @Override
        protected boolean isIndeterminate() {
            return true;
        }

        @Override
        public boolean isCanceled() {
            return isStoppingReverseEngineering();
        }

        @Override
        public void setCanceled(boolean b) {
            if (b) {
                loadStatusNote = "Canceling..";
            }

            setStoppingReverseEngineering(b);
        }
    }

    final class LoadSchemasTask extends DbLoaderTask {

        public LoadSchemasTask(JFrame frame, String title) {
            super(frame, title);
        }

        @Override
        protected void execute() {
            loadStatusNote = "Loading available schemas...";

            try {
                schemas = loader.loadSchemas();
            } catch (Throwable th) {
                processException(th, "Error Loading Schemas");
            }
        }
    }

    final class LoadCatalogsTask extends DbLoaderTask {

        public LoadCatalogsTask(JFrame frame, String title) {
            super(frame, title);
        }

        @Override
        protected void execute() {
            loadStatusNote = "Loading available catalogs...";

            try {
                catalogs = loader.loadCatalogs();
            } catch (Throwable th) {
                processException(th, "Error Loading Catalogs");
            }
        }
    }

    public final class LoadDataMapTask extends DbLoaderTask {

        public LoadDataMapTask(JFrame frame, String title) {
            super(frame, title);
        }

        @Override
        protected void execute() {

            loadStatusNote = "Preparing...";

            DbLoaderHelper.this.dataMap = mediator.getCurrentDataMap();
            DbLoaderHelper.this.existingMap = dataMap != null;

            if (!existingMap) {

                ConfigurationNode root = mediator.getProject().getRootNode();

                dataMap = new DataMap();
                dataMap.setName(NameBuilder.builder(dataMap, root).name());
            }

            if (isCanceled()) {
                return;
            }

            DataMap dataMap = mediator.getCurrentDataMap();
            DataChannelDescriptor dataChannelDescriptor = mediator.getCurrentDataChanel();
            if (dataMap.getReverseEngineering() != null) {
                if (dataMap.getReverseEngineering().getName() != null) {
                    reverseEngineering.setName(dataMap.getReverseEngineering().getName());
                    reverseEngineering.setConfigurationSource(dataMap.getReverseEngineering().getConfigurationSource());
                }
            } else {
                reverseEngineering.setName(NameBuilder.builder(reverseEngineering, dataChannelDescriptor).name());
            }

            if (dataMap.getConfigurationSource() != null) {
                config.setDataMapFile(new File(dataMap.getConfigurationSource().getURL().getPath()));
            }

            FiltersConfigBuilder filtersConfigBuilder = new FiltersConfigBuilder(reverseEngineering);
            config.getDbLoaderConfig().setFiltersConfig(filtersConfigBuilder.build());


            ModelerDbImportAction importAction = new ModelerDbImportAction(LOGGER, DbLoaderHelper.this);

            // TODO: we can keep all these things in the Modeler Injector instead of creating a new one?
            // we already have CayenneDbSyncModule in there
            Injector injector = DIBootstrap.createInjector(new CayenneDbSyncModule(), new ToolsModule(LOGGER), new DbImportModule());
            injector.injectMembers(importAction);
            try {
                importAction.execute(config);
                dataMap.setReverseEngineering(reverseEngineering);
            } catch (Exception e) {
                processException(e, "Error importing database schema.");
            }
        }
    }

}
