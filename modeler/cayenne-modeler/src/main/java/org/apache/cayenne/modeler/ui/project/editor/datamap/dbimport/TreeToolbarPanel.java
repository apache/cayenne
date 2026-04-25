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

package org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport;

import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.action.DbImportActions;
import org.apache.cayenne.modeler.ui.project.editor.datamap.dbimport.tree.DbImportTreeNode;

import javax.swing.JButton;
import javax.swing.JToolBar;
import javax.swing.border.EmptyBorder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TreeToolbarPanel extends JToolBar {

    private final JButton schemaButton;
    private final JButton catalogButton;
    private final JButton includeTableButton;
    private final JButton excludeTableButton;
    private final JButton includeColumnButton;
    private final JButton excludeColumnButton;
    private final JButton includeProcedureButton;
    private final JButton excludeProcedureButton;
    private final JButton editButton;
    private final JButton deleteButton;
    private final JButton configureButton;
    private final JButton sortButton;
    private final DbImportTree reverseEngineeringTree;

    private final Map<Class<?>, List<JButton>> levels;

    TreeToolbarPanel(DbImportTree reverseEngineeringTree, DbImportActions actions) {
        this.reverseEngineeringTree = reverseEngineeringTree;

        this.schemaButton = actions.getAddSchemaAction().buildButton(0);
        this.catalogButton = actions.getAddCatalogAction().buildButton(0);
        this.includeTableButton = actions.getAddIncludeTableAction().buildButton(1);
        this.excludeTableButton = actions.getAddExcludeTableAction().buildButton(2);
        this.includeColumnButton = actions.getAddIncludeColumnAction().buildButton(2);
        this.excludeColumnButton = actions.getAddExcludeColumnAction().buildButton(2);
        this.includeProcedureButton = actions.getAddIncludeProcedureAction().buildButton(2);
        this.excludeProcedureButton = actions.getAddExcludeProcedureAction().buildButton(3);
        this.sortButton = actions.getSortNodesAction().buildButton(0);
        this.editButton = actions.getEditNodeAction().buildButton(0);
        this.deleteButton = actions.getDeleteNodeAction().buildButton(0);
        this.configureButton = actions.getGetDbConnectionAction().buildButton(0);

        this.levels = initLevels();
        addButtons();
        this.setBorder(new EmptyBorder(0, 0, 0, 0));
    }

    void unlockButtons() {
        changeToolbarButtonsState(true);
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
    }

    private boolean isLabelSelected() {
        DbImportTreeNode selectedNode = reverseEngineeringTree.getSelectedNode();
        if (selectedNode.getUserObject().getClass() == String.class) {
            return true;
        }
        return false;
    }

    void lockButtons() {
        if ((reverseEngineeringTree.getLastSelectedPathComponent() != null) && (!isLabelSelected())) {
            DbImportTreeNode selectedNode = ((DbImportTreeNode) reverseEngineeringTree.getLastSelectedPathComponent());
            DbImportTreeNode parentNode = (DbImportTreeNode) selectedNode.getParent();
            if (parentNode != null) {
                lockButtons(parentNode.getUserObject());
            } else {
                unlockButtons();
            }
        } else {
            changeToolbarButtonsState(true);
            editButton.setEnabled(false);
            deleteButton.setEnabled(false);
        }
        if (reverseEngineeringTree.getSelectionPaths() != null) {
            if (reverseEngineeringTree.getSelectionPaths().length > 1) {
                changeToolbarButtonsState(false);
                deleteButton.setEnabled(true);
            }
        }
    }

    private Map<Class<?>, List<JButton>> initLevels() {
        Map<Class<?>, List<JButton>> levels = new HashMap<>();

        List<JButton> rootLevelButtons = new ArrayList<>();
        rootLevelButtons.add(catalogButton);
        rootLevelButtons.add(schemaButton);
        rootLevelButtons.add(includeTableButton);
        rootLevelButtons.add(excludeTableButton);
        rootLevelButtons.add(includeColumnButton);
        rootLevelButtons.add(excludeColumnButton);
        rootLevelButtons.add(includeProcedureButton);
        rootLevelButtons.add(excludeProcedureButton);

        List<JButton> catalogLevelButtons = new ArrayList<>();
        catalogLevelButtons.add(schemaButton);
        catalogLevelButtons.add(includeTableButton);
        catalogLevelButtons.add(excludeTableButton);
        catalogLevelButtons.add(includeColumnButton);
        catalogLevelButtons.add(excludeColumnButton);
        catalogLevelButtons.add(includeProcedureButton);
        catalogLevelButtons.add(excludeProcedureButton);

        List<JButton> schemaLevelButtons = new ArrayList<>();
        schemaLevelButtons.add(includeTableButton);
        schemaLevelButtons.add(excludeTableButton);
        schemaLevelButtons.add(includeColumnButton);
        schemaLevelButtons.add(excludeColumnButton);
        schemaLevelButtons.add(includeProcedureButton);
        schemaLevelButtons.add(excludeProcedureButton);

        List<JButton> includeTableLevelButtons = new ArrayList<>();
        includeTableLevelButtons.add(includeColumnButton);
        includeTableLevelButtons.add(excludeColumnButton);

        levels.put(ReverseEngineering.class, rootLevelButtons);
        levels.put(Catalog.class, catalogLevelButtons);
        levels.put(Schema.class, schemaLevelButtons);
        levels.put(IncludeTable.class, includeTableLevelButtons);
        return levels;
    }

    private void addButtons() {
        this.setFloatable(false);
        this.add(catalogButton);
        this.add(schemaButton);
        this.addSeparator();
        this.add(includeTableButton);
        this.add(excludeTableButton);
        this.add(includeColumnButton);
        this.add(excludeColumnButton);
        this.add(includeProcedureButton);
        this.add(excludeProcedureButton);
        this.add(editButton);
        this.add(sortButton);
        this.addSeparator();
        this.add(deleteButton);
        this.add(configureButton);
    }

    void changeToolbarButtonsState(boolean state) {
        schemaButton.setEnabled(state);
        catalogButton.setEnabled(state);
        includeTableButton.setEnabled(state);
        excludeTableButton.setEnabled(state);
        includeColumnButton.setEnabled(state);
        excludeColumnButton.setEnabled(state);
        includeProcedureButton.setEnabled(state);
        excludeProcedureButton.setEnabled(state);
        editButton.setEnabled(state);
        deleteButton.setEnabled(state);
    }

    private void lockButtons(Object userObject) {
        changeToolbarButtonsState(false);
        List<JButton> buttons = levels.get(userObject.getClass());
        for (JButton button : buttons) {
            button.setEnabled(true);
        }
        editButton.setEnabled(true);
        deleteButton.setEnabled(true);
    }
}
