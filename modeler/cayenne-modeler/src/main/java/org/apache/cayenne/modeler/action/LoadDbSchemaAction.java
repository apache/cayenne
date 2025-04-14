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

package org.apache.cayenne.modeler.action;

import javax.swing.JOptionPane;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.sql.SQLException;

import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;
import org.apache.cayenne.modeler.editor.dbimport.DatabaseSchemaLoader;
import org.apache.cayenne.modeler.editor.dbimport.DbImportModel;
import org.apache.cayenne.modeler.editor.dbimport.DbImportView;
import org.apache.cayenne.modeler.editor.dbimport.DraggableTreePanel;
import org.apache.cayenne.modeler.editor.dbimport.PrintColumnsBiFunction;
import org.apache.cayenne.modeler.editor.dbimport.PrintTablesBiFunction;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 4.1
 */
public class LoadDbSchemaAction extends DBConnectionAwareAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadDbSchemaAction.class);

    private static final String ICON_NAME = "icon-dbi-refresh.png";
    private static final String ACTION_NAME = "Refresh Db Schema";
    private DraggableTreePanel draggableTreePanel;

    LoadDbSchemaAction(Application application) {
        super(ACTION_NAME, application);
    }

    public String getIconName() {
        return ICON_NAME;
    }

    @Override
    public void performAction(ActionEvent e) {
        performAction(e, null);
    }

    public void performAction(ActionEvent e, TreePath tablePath) {
        final DbImportView rootParent = ((DbImportView) draggableTreePanel.getParent().getParent());
        rootParent.getLoadDbSchemaProgress().setVisible(true);
        rootParent.getLoadDbSchemaButton().setEnabled(false);
        Thread thread = new Thread(() -> {
            LoadDbSchemaAction.this.setEnabled(false);
            rootParent.lockToolbarButtons();
            draggableTreePanel.getMoveButton().setEnabled(false);
            draggableTreePanel.getMoveInvertButton().setEnabled(false);
            try {

                DBConnectionInfo connectionInfo = getConnectionInfo("Load Db Schema");
                if (connectionInfo == null) {
                    return;
                }

                if (tablePath != null) {
                    Object userObject = ((DbImportTreeNode) tablePath.getLastPathComponent()).getUserObject();
                    if (userObject instanceof Catalog) {
                        Catalog catalog = (Catalog) userObject;
                        if (catalog.getSchemas().isEmpty()) {
                            loadTables(connectionInfo, tablePath, rootParent);
                        }
                    } else if (userObject instanceof Schema) {
                        loadTables(connectionInfo, tablePath, rootParent);
                    } else if (userObject instanceof IncludeTable) {
                        loadColumns(connectionInfo, tablePath);
                    } else {
                        loadTables(connectionInfo, tablePath, rootParent);
                    }
                } else {
                    loadDataBase(connectionInfo);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        Application.getFrame(),
                        ex.getMessage(),
                        "Error loading db schema",
                        JOptionPane.ERROR_MESSAGE);
                LOGGER.warn("Error loading db schema", ex);
            } finally {
                rootParent.getLoadDbSchemaButton().setEnabled(true);
                rootParent.getLoadDbSchemaProgress().setVisible(false);
                rootParent.unlockToolbarButtons();
            }
        });
        thread.start();
    }

    private void loadDataBase(DBConnectionInfo connectionInfo) throws Exception {
        ReverseEngineering databaseReverseEngineering = new DatabaseSchemaLoader()
                .load(connectionInfo,
                        getApplication().getClassLoadingService());
        draggableTreePanel.getSourceTree()
                .setEnabled(true);
        draggableTreePanel.getSourceTree()
                .translateReverseEngineeringToTree(databaseReverseEngineering, true);
        draggableTreePanel
                .bindReverseEngineeringToDatamap(getProjectController().getCurrentDataMap(), databaseReverseEngineering);
        ((DbImportModel) draggableTreePanel.getSourceTree().getModel()).reload();
    }

    private void loadTables(DBConnectionInfo connectionInfo,
                            TreePath tablePath,
                            DbImportView rootParent) throws Exception {
        ReverseEngineering databaseReverseEngineering = new DatabaseSchemaLoader()
                .loadTables(connectionInfo,
                        getApplication().getClassLoadingService(),
                        tablePath,
                        rootParent.getTableTypes());
        draggableTreePanel.getSourceTree()
                .update(databaseReverseEngineering,
                        new PrintTablesBiFunction(draggableTreePanel.getSourceTree()));
    }

    private void loadColumns(DBConnectionInfo connectionInfo, TreePath tablePath) throws SQLException {
        ReverseEngineering databaseReverseEngineering = new DatabaseSchemaLoader()
                .loadColumns(connectionInfo, getApplication().getClassLoadingService(), tablePath);
        draggableTreePanel.getSourceTree()
                .update(databaseReverseEngineering,
                        new PrintColumnsBiFunction(draggableTreePanel.getSourceTree()));
    }

    public void setDraggableTreePanel(DraggableTreePanel draggableTreePanel) {
        this.draggableTreePanel = draggableTreePanel;
    }
}
