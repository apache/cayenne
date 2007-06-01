/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler.dialog.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.DbLoader;
import org.objectstyle.cayenne.access.DbLoaderDelegate;
import org.objectstyle.cayenne.dba.DbAdapter;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.event.DataMapEvent;
import org.objectstyle.cayenne.map.event.EntityEvent;
import org.objectstyle.cayenne.map.event.MapEvent;
import org.objectstyle.cayenne.modeler.Application;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.event.DataMapDisplayEvent;
import org.objectstyle.cayenne.modeler.util.LongRunningTask;
import org.objectstyle.cayenne.project.NamedObjectFactory;
import org.objectstyle.cayenne.util.Util;

/**
 * Stateful helper class that encapsulates access to DbLoader.
 * 
 * @author Andrei Adamchik
 */
public class DbLoaderHelper {

    private static final Logger logObj = Logger.getLogger(DbLoaderHelper.class);

    // TODO: this is a temp hack... need to delegate to DbAdapter, or configurable in
    // preferences...
    private static final Collection EXCLUDED_TABLES = Arrays.asList(new Object[] {
            "AUTO_PK_SUPPORT", "auto_pk_support"
    });

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
    protected String procedureNamePattern;
    protected List schemas;

    protected String loadStatusNote;

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
                    dialog.show();
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
        this.procedureNamePattern = dialog.getProcedureNamePattern();

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
            if (loader.getCon() != null) {
                loader.getCon().close();
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
                dialog.show();
                dialog.hide();
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
                        .fireDbEntityEvent(new EntityEvent(this, entity, EntityEvent.ADD));
            }
        }

        public void objEntityAdded(ObjEntity entity) {
            checkCanceled();

            loadStatusNote = "Creating ObjEntity '" + entity.getName() + "'...";

            if (existingMap) {
                mediator
                        .fireObjEntityEvent(new EntityEvent(this, entity, EntityEvent.ADD));
            }
        }

        public void dbEntityRemoved(DbEntity entity) {
            checkCanceled();

            if (existingMap) {
                mediator.fireDbEntityEvent(new EntityEvent(
                        Application.getFrame(),
                        entity,
                        EntityEvent.REMOVE));
            }
        }

        public void objEntityRemoved(ObjEntity entity) {
            checkCanceled();

            if (existingMap) {
                mediator.fireObjEntityEvent(new EntityEvent(Application
                        .getFrame(), entity, EntityEvent.REMOVE));
            }
        }

        public void setSchema(DbEntity entity, String schema) {
            // noop, deprecated in the interface
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

        protected String getCurrentNote() {
            return loadStatusNote;
        }

        protected int getCurrentValue() {
            return getMinValue();
        }

        protected boolean isIndeterminate() {
            return true;
        }

        public boolean isCanceled() {
            return isStoppingReverseEngineering();
        }

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
                loader.loadDataMapFromDB(schemaName, tableNamePattern, dataMap);
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