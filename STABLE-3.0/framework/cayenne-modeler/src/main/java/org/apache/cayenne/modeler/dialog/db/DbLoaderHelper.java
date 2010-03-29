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

package org.apache.cayenne.modeler.dialog.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.cayenne.CayenneException;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DbLoader;
import org.apache.cayenne.access.DbLoaderDelegate;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.event.DataMapEvent;
import org.apache.cayenne.map.event.EntityEvent;
import org.apache.cayenne.map.event.MapEvent;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.event.DataMapDisplayEvent;
import org.apache.cayenne.modeler.util.LongRunningTask;
import org.apache.cayenne.project.NamedObjectFactory;
import org.apache.cayenne.util.DeleteRuleUpdater;
import org.apache.cayenne.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Stateful helper class that encapsulates access to DbLoader.
 * 
 */
public class DbLoaderHelper {

    private static Log logObj = LogFactory.getLog(DbLoaderHelper.class);

    // TODO: this is a temp hack... need to delegate to DbAdapter, or configurable in
    // preferences...
    private static final Collection EXCLUDED_TABLES = Arrays.asList("AUTO_PK_SUPPORT", "auto_pk_support");

    static DbLoaderMergeDialog mergeDialog;

    protected boolean overwritePreferenceSet;
    protected boolean overwritingEntities;
    protected boolean stoppingReverseEngineering;
    protected boolean existingMap;

    protected ProjectController mediator;
    protected String dbUserName;
    protected DbLoader loader;
    protected DataMap dataMap;
    protected String schemaName;
    protected String tableNamePattern;
    protected boolean loadProcedures;
    protected boolean meaningfulPk;
    protected String procedureNamePattern;
    protected List schemas;

    protected String loadStatusNote;
    
    /**
     * Obj Entities which were added to project during reverse engineering 
     */
    protected List<ObjEntity> addedObjEntities;

    static synchronized DbLoaderMergeDialog getMergeDialogInstance() {
        if (mergeDialog == null) {
            mergeDialog = new DbLoaderMergeDialog(Application.getFrame());
        }

        return mergeDialog;
    }

    public DbLoaderHelper(ProjectController mediator, Connection connection,
            DbAdapter adapter, String dbUserName) {
        this.dbUserName = dbUserName;
        this.mediator = mediator;
        this.loader = new DbLoader(connection, adapter, new LoaderDelegate());
    }

    public void setOverwritingEntities(boolean overwritePreference) {
        this.overwritingEntities = overwritePreference;
    }

    public void setOverwritePreferenceSet(boolean overwritePreferenceSet) {
        this.overwritePreferenceSet = overwritePreferenceSet;
    }

    public void setStoppingReverseEngineering(boolean stopReverseEngineering) {
        this.stoppingReverseEngineering = stopReverseEngineering;
    }

    public boolean isOverwritePreferenceSet() {
        return overwritePreferenceSet;
    }

    public boolean isOverwritingEntities() {
        return overwritingEntities;
    }

    public boolean isStoppingReverseEngineering() {
        return stoppingReverseEngineering;
    }

    /**
     * Performs reverse engineering of the DB using internal DbLoader. This method should
     * be invoked outside EventDispatchThread, or it will throw an exception.
     */
    public void execute() {
        stoppingReverseEngineering = false;

        // load schemas...
        LongRunningTask loadSchemasTask = new LoadSchemasTask(Application
                .getFrame(), "Loading Schemas");

        loadSchemasTask.startAndWait();

        if (stoppingReverseEngineering) {
            return;
        }

        final DbLoaderOptionsDialog dialog = new DbLoaderOptionsDialog(
                schemas,
                dbUserName,
                false);

        try {
            // since we are not inside EventDisptahcer Thread, must run it via
            // SwingUtilities
            SwingUtilities.invokeAndWait(new Runnable() {

                public void run() {
                    dialog.setVisible(true);
                    dialog.dispose();
                }
            });
        }
        catch (Throwable th) {
            processException(th, "Error Reengineering Database");
            return;
        }

        if (dialog.getChoice() == DbLoaderOptionsDialog.CANCEL) {
            return;
        }

        this.schemaName = dialog.getSelectedSchema();
        this.tableNamePattern = dialog.getTableNamePattern();
        this.loadProcedures = dialog.isLoadingProcedures();
        this.meaningfulPk = dialog.isMeaningfulPk();
        this.procedureNamePattern = dialog.getProcedureNamePattern();
        this.addedObjEntities = new ArrayList<ObjEntity>();
        
        this.loader.setNamingStrategy(dialog.getNamingStrategy());

        // load DataMap...
        LongRunningTask loadDataMapTask = new LoadDataMapTask(Application
                .getFrame(), "Reengineering DB");
        loadDataMapTask.startAndWait();
    }

    protected void processException(final Throwable th, final String message) {
        logObj.info("Exception on reverse engineering", Util.unwindException(th));
        cleanup();
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                JOptionPane.showMessageDialog(Application.getFrame(), th
                        .getMessage(), message, JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    protected void cleanup() {
        loadStatusNote = "Closing connection...";
        try {
            if (loader.getConnection() != null) {
                loader.getConnection().close();
            }
        }
        catch (SQLException e) {
            logObj.warn("Error closing connection.", e);
        }
    }

    final class LoaderDelegate implements DbLoaderDelegate {

        public boolean overwriteDbEntity(DbEntity ent) throws CayenneException {
            checkCanceled();

            if (!overwritePreferenceSet) {
                DbLoaderMergeDialog dialog = DbLoaderHelper.getMergeDialogInstance();
                dialog.initFromModel(DbLoaderHelper.this, ent.getName());
                dialog.centerWindow();
                dialog.setVisible(true);
                dialog.setVisible(false);
            }

            if (stoppingReverseEngineering) {
                throw new CayenneException("Should stop DB import.");
            }

            return overwritingEntities;
        }

        public void dbEntityAdded(DbEntity entity) {
            checkCanceled();

            loadStatusNote = "Importing table '" + entity.getName() + "'...";

            // TODO: hack to prevent PK tables from being visible... this should really be
            // delegated to DbAdapter to decide...
            if (EXCLUDED_TABLES.contains(entity.getName()) && entity.getDataMap() != null) {
                entity.getDataMap().removeDbEntity(entity.getName());
            }
            else if (existingMap) {
                mediator
                        .fireDbEntityEvent(new EntityEvent(this, entity, MapEvent.ADD));
            }
        }

        public void objEntityAdded(ObjEntity entity) {
            checkCanceled();

            loadStatusNote = "Creating ObjEntity '" + entity.getName() + "'...";
            addedObjEntities.add(entity);

            if (existingMap) {
                mediator.fireObjEntityEvent(new EntityEvent(this, entity, MapEvent.ADD));
            }
        }

        public void dbEntityRemoved(DbEntity entity) {
            checkCanceled();

            if (existingMap) {
                mediator.fireDbEntityEvent(new EntityEvent(
                        Application.getFrame(),
                        entity,
                        MapEvent.REMOVE));
            }
        }

        public void objEntityRemoved(ObjEntity entity) {
            checkCanceled();

            if (existingMap) {
                mediator.fireObjEntityEvent(new EntityEvent(Application
                        .getFrame(), entity, MapEvent.REMOVE));
            }
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
                schemas = loader.getSchemas();
            }
            catch (Throwable th) {
                processException(th, "Error Loading Schemas");
            }
        }
    }

    final class LoadDataMapTask extends DbLoaderTask {

        public LoadDataMapTask(JFrame frame, String title) {
            super(frame, title);
        }

        @Override
        protected void execute() {

            loadStatusNote = "Preparing...";

            DbLoaderHelper.this.dataMap = mediator.getCurrentDataMap();
            DbLoaderHelper.this.existingMap = dataMap != null;

            if (!existingMap) {
                dataMap = (DataMap) NamedObjectFactory.createObject(DataMap.class, null);
                dataMap.setName(NamedObjectFactory.createName(DataMap.class, mediator
                        .getCurrentDataDomain()));
                dataMap.setDefaultSchema(schemaName);
            }

            if (isCanceled()) {
                return;
            }

            loadStatusNote = "Importing tables...";

            try {
                loader.setCreatingMeaningfulPK(meaningfulPk);
                loader.loadDataMapFromDB(schemaName, tableNamePattern, dataMap);
                
                /**
                 * Update default rules for relationships 
                 */
                for (ObjEntity addedObjEntity : addedObjEntities) {
                    DeleteRuleUpdater.updateObjEntity(addedObjEntity);
                }
            }
            catch (Throwable th) {
                if (!isCanceled()) {
                    processException(th, "Error Reengineering Database");
                }
            }

            if (loadProcedures) {
                loadStatusNote = "Importing procedures...";
                try {
                    loader
                            .loadProceduresFromDB(
                                    schemaName,
                                    procedureNamePattern,
                                    dataMap);
                }
                catch (Throwable th) {
                    if (!isCanceled()) {
                        processException(th, "Error Reengineering Database");
                    }
                }
            }

            cleanup();

            // fire up events
            loadStatusNote = "Updating view...";
            if (mediator.getCurrentDataMap() != null) {
                mediator.fireDataMapEvent(new DataMapEvent(
                        Application.getFrame(),
                        dataMap,
                        MapEvent.CHANGE));
                mediator.fireDataMapDisplayEvent(new DataMapDisplayEvent(
                        Application.getFrame(),
                        dataMap,
                        mediator.getCurrentDataDomain(),
                        mediator.getCurrentDataNode()));
            }
            else {
                mediator.addDataMap(Application.getFrame(), dataMap);
            }
        }
    }
}
