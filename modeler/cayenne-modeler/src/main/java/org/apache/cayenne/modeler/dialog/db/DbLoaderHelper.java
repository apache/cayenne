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
import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.server.DataSourceFactory;
import org.apache.cayenne.configuration.server.DbAdapterFactory;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dbimport.Catalog;
import org.apache.cayenne.dbimport.IncludeProcedure;
import org.apache.cayenne.dbimport.IncludeTable;
import org.apache.cayenne.dbimport.ReverseEngineering;
import org.apache.cayenne.dbimport.Schema;
import org.apache.cayenne.dbsync.DbSyncModule;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactoryProvider;
import org.apache.cayenne.dbsync.naming.NameBuilder;
import org.apache.cayenne.dbsync.reverse.dbload.DbLoader;
import org.apache.cayenne.dbsync.reverse.dbload.DefaultDbLoaderDelegate;
import org.apache.cayenne.dbsync.reverse.filters.FiltersConfigBuilder;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.MapLoader;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.DbImportProjectSaver;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.apache.cayenne.modeler.util.LongRunningTask;
import org.apache.cayenne.modeler.util.ProjectUtil;
import org.apache.cayenne.tools.configuration.ToolsModule;
import org.apache.cayenne.tools.dbimport.DbImportAction;
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
import java.util.Collections;
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
    protected ProjectController projectController;
    protected Connection connection;
    protected DataMap dataMap;
    protected DbAdapter adapter;
    protected DbImportConfiguration config;
    protected String loadStatusNote;

    public DbLoaderHelper(ProjectController projectController,
                          Connection connection,
                          DbAdapter adapter,
                          DBConnectionInfo dbConnectionInfo) {

        this.projectController = projectController;
        this.adapter = adapter;

        this.config = new DbImportConfiguration();
        this.config.setAdapter(adapter.getClass().getName());
        this.config.setUsername(dbConnectionInfo.getUserName());
        this.config.setPassword(dbConnectionInfo.getPassword());
        this.config.setDriver(dbConnectionInfo.getJdbcDriver());
        this.config.setUrl(dbConnectionInfo.getUrl());

        this.connection = connection;
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
        List<String> catalogs = Collections.emptyList();
        if (adapter.supportsCatalogsOnReverseEngineering()) {
            catalogs = new LoadCatalogsTask(Application.getFrame(), "Loading Catalogs").startAndWait();
        }

        if (stoppingReverseEngineering) {
            return;
        }

        // load schemas...
        List<String> schemas = new LoadSchemasTask(Application.getFrame(), "Loading Schemas").startAndWait();

        if (stoppingReverseEngineering) {
            return;
        }

        // use this catalog as the default...
        String currentCatalog = null;
        try {
            currentCatalog = connection.getCatalog();
        } catch (SQLException e) {
            LOGGER.warn("Error getting catalog.", e);
        }

        String currentSchema = null;
        try {
            // 'getSchema' is Java 1.7 API ... hope this works with all drivers...
            currentSchema = connection.getSchema();
        } catch (SQLException e) {
            LOGGER.warn("Error getting schema.", e);
        }

        final DbLoaderOptionsDialog dialog = new DbLoaderOptionsDialog(
                schemas,
                catalogs,
                currentSchema,
                currentCatalog);

        try {
            // since we are not inside EventDispatcher Thread, must run it via SwingUtilities
            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    dialog.setVisible(true);
                    dialog.dispose();
                }
            });
        } catch (Throwable th) {
            processException(th, "Error Reengineering Database");
            return;
        }

        if (dialog.getChoice() == DbLoaderOptionsDialog.CANCEL) {
            return;
        }

        ReverseEngineering reverseEngineering = new ReverseEngineering();

        reverseEngineering.addCatalog(new Catalog(dialog.getSelectedCatalog()));
        reverseEngineering.addSchema(new Schema(dialog.getSelectedSchema()));
        reverseEngineering.addIncludeTable(new IncludeTable(dialog.getTableNamePattern()));
        reverseEngineering.addIncludeProcedure(new IncludeProcedure(dialog.getProcedureNamePattern()));

        config.setMeaningfulPkTables(dialog.getMeaningfulPk());
        config.setNamingStrategy(dialog.getNamingStrategy());

        new LoadDataMapTask(Application.getFrame(), "Reengineering DB", reverseEngineering).startAndWait();
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


    private final class LoaderDelegate extends DefaultDbLoaderDelegate {

        @Override
        public void dbEntityAdded(DbEntity entity) {
            checkCanceled();

            loadStatusNote = "Importing table '" + entity.getName() + "'...";

            // TODO: hack to prevent PK tables from being visible... this should
            // really be delegated to DbAdapter to decide...
            if (EXCLUDED_TABLES.contains(entity.getName()) && entity.getDataMap() != null) {
                entity.getDataMap().removeDbEntity(entity.getName());
            } else if (existingMap) {
                projectController.fireDbEntityEvent(new EntityEvent(this, entity, MapEvent.ADD));
            }
        }

        @Override
        public void dbEntityRemoved(DbEntity entity) {
            checkCanceled();

            if (existingMap) {
                projectController.fireDbEntityEvent(new EntityEvent(Application.getFrame(), entity, MapEvent.REMOVE));
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

    abstract class DbLoaderTask<T> extends LongRunningTask<T> {

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

    final class LoadCatalogsTask extends DbLoaderTask<List<String>> {

        public LoadCatalogsTask(JFrame frame, String title) {
            super(frame, title);
        }

        @Override
        protected void execute() {
            loadStatusNote = "Loading available catalogs...";

            try {
                result = DbLoader.loadCatalogs(connection);
            } catch (Throwable th) {
                processException(th, "Error Loading Catalogs");
            }
        }
    }

    final class LoadSchemasTask extends DbLoaderTask<List<String>> {

        public LoadSchemasTask(JFrame frame, String title) {
            super(frame, title);
        }

        @Override
        protected void execute() {
            loadStatusNote = "Loading available schemas...";

            try {
                result = DbLoader.loadSchemas(connection);
            } catch (Throwable th) {
                processException(th, "Error Loading Schemas");
            }
        }
    }

    public final class LoadDataMapTask extends DbLoaderTask {

        private ReverseEngineering reverseEngineering;

        public LoadDataMapTask(JFrame frame, String title, ReverseEngineering reverseEngineering) {
            super(frame, title);
            this.reverseEngineering = reverseEngineering;
        }

        @Override
        protected void execute() {

            loadStatusNote = "Preparing...";

            DbLoaderHelper.this.dataMap = projectController.getCurrentDataMap();
            DbLoaderHelper.this.existingMap = dataMap != null;

            if (!existingMap) {

                ConfigurationNode root = projectController.getProject().getRootNode();

                dataMap = new DataMap();
                dataMap.setName(NameBuilder.builder(dataMap, root).name());
            }

            if (isCanceled()) {
                return;
            }

            if (dataMap.getConfigurationSource() != null) {
                config.setTargetDataMap(new File(dataMap.getConfigurationSource().getURL().getPath()));
            }

            FiltersConfigBuilder filtersConfigBuilder = new FiltersConfigBuilder(reverseEngineering);
            config.getDbLoaderConfig().setFiltersConfig(filtersConfigBuilder.build());

            DbImportAction importAction = createAction(dataMap);
            try {
                importAction.execute(config);
            } catch (Exception e) {
                processException(e, "Error importing database schema.");
            }
            ProjectUtil.cleanObjMappings(dataMap);
        }

        DbImportAction createAction(DataMap targetDataMap) {
            Injector injector = DIBootstrap.createInjector(new DbSyncModule(),
                    new ToolsModule(LOGGER),
                    new DbImportModule());

            return new ModelerDbImportAction(
                    LOGGER,
                    new DbImportProjectSaver(projectController, injector.getInstance(ConfigurationNameMapper.class)),
                    injector.getInstance(DataSourceFactory.class),
                    injector.getInstance(DbAdapterFactory.class),
                    injector.getInstance(MapLoader.class),
                    injector.getInstance(MergerTokenFactoryProvider.class),
                    targetDataMap,
                    createDbLoader(config));
        }

        DbLoader createDbLoader(DbImportConfiguration configuration) {
            return new DbLoader(adapter, connection, configuration.getDbLoaderConfig(), new LoaderDelegate(), configuration.createNameGenerator());
        }
    }

}
