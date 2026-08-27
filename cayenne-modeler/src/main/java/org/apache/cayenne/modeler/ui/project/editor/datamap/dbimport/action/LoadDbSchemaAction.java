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

package org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.action;

import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.ui.action.DBConnectionAwareAction;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.tree.DbImportTreeNode;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.DatabaseSchemaLoader;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.DbImportTreeModel;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.DbImportView;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.DBSchemaPanel;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.PrintColumnsBiFunction;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.PrintTablesBiFunction;
import org.apache.cayenne.modeler.pref.dbconnector.DBConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.sql.SQLException;

public class LoadDbSchemaAction extends DBConnectionAwareAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadDbSchemaAction.class);

    private static final String ICON_NAME = "icon-dbi-refresh.png";
    private static final String ACTION_NAME = "Refresh Db Schema";

    private final DbImportView view;

    public LoadDbSchemaAction(Application application, DbImportView view) {
        super(ACTION_NAME, application);
        this.view = view;
    }

    public String getIconName() {
        return ICON_NAME;
    }

    @Override
    public void performAction(ActionEvent e) {
        loadDbSchema(null);
    }

    public void loadDbSchema(TreePath tablePath) {
        DBSchemaPanel sourceTargetPanel = view.getDraggableTreePanel();
        view.getLoadDbSchemaProgress().setVisible(true);
        view.getLoadDbSchemaButton().setEnabled(false);
        Thread thread = new Thread(() -> {
            LoadDbSchemaAction.this.setEnabled(false);
            sourceTargetPanel.getMoveButton().setEnabled(false);
            sourceTargetPanel.getMoveInvertButton().setEnabled(false);
            try {

                DBConnector connectionInfo = getConnector(
                        "Load Db Schema",
                        app.getFrame().getProjectSession().getSelectedDataMap());

                if (connectionInfo == null) {
                    return;
                }

                if (tablePath != null) {
                    Object userObject = ((DbImportTreeNode) tablePath.getLastPathComponent()).getUserObject();
                    if (userObject instanceof Catalog) {
                        Catalog catalog = (Catalog) userObject;
                        if (catalog.getSchemas().isEmpty()) {
                            loadTables(connectionInfo, tablePath);
                        }
                    } else if (userObject instanceof Schema) {
                        loadTables(connectionInfo, tablePath);
                    } else if (userObject instanceof IncludeTable) {
                        loadColumns(connectionInfo, tablePath);
                    } else {
                        loadTables(connectionInfo, tablePath);
                    }
                } else {
                    loadDataBase(connectionInfo);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        app.getFrame(),
                        ex.getMessage(),
                        "Error loading db schema",
                        JOptionPane.ERROR_MESSAGE);
                LOGGER.warn("Error loading db schema", ex);
            } finally {
                view.getLoadDbSchemaButton().setEnabled(true);
                view.getLoadDbSchemaProgress().setVisible(false);
            }
        });
        thread.start();
    }

    private void loadDataBase(DBConnector connectionInfo) throws Exception {
        DBSchemaPanel sourceTargetPanel = view.getDraggableTreePanel();
        ReverseEngineering databaseReverseEngineering = new DatabaseSchemaLoader(app.getDbAdapterFactory())
                .load(connectionInfo, app.getClassLoader());
        sourceTargetPanel.getSourceTree()
                .setEnabled(true);
        sourceTargetPanel.getSourceTree()
                .translateReverseEngineeringToTree(databaseReverseEngineering, true);
        sourceTargetPanel
                .bindReverseEngineeringToDatamap(getProjectSession().getSelectedDataMap(), databaseReverseEngineering);
        ((DbImportTreeModel) sourceTargetPanel.getSourceTree().getModel()).reload();
    }

    private void loadTables(DBConnector connectionInfo, TreePath tablePath) throws Exception {
        DBSchemaPanel sourceTargetPanel = view.getDraggableTreePanel();
        ReverseEngineering databaseReverseEngineering = new DatabaseSchemaLoader(app.getDbAdapterFactory())
                .loadTables(connectionInfo,
                        app.getClassLoader(),
                        tablePath,
                        view.getTableTypes());
        sourceTargetPanel.getSourceTree()
                .update(databaseReverseEngineering,
                        new PrintTablesBiFunction(sourceTargetPanel.getSourceTree()));
    }

    private void loadColumns(DBConnector connectionInfo, TreePath tablePath) throws SQLException {
        DBSchemaPanel sourceTargetPanel = view.getDraggableTreePanel();
        ReverseEngineering databaseReverseEngineering = new DatabaseSchemaLoader(app.getDbAdapterFactory())
                .loadColumns(connectionInfo, app.getClassLoader(), tablePath);
        sourceTargetPanel.getSourceTree()
                .update(databaseReverseEngineering,
                        new PrintColumnsBiFunction(sourceTargetPanel.getSourceTree()));
    }
}
