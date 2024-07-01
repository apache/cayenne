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

package org.apache.cayenne.modeler.action.dbimport;

import org.apache.cayenne.dbsync.reverse.dbimport.FilterContainer;
import org.apache.cayenne.dbsync.reverse.dbimport.PatternParam;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;
import org.apache.cayenne.modeler.editor.dbimport.DbImportSorter;
import org.apache.cayenne.util.Util;

import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;

/**
 * @since 4.1
 */
public class EditNodeAction extends TreeManipulationAction {

    private static final String ACTION_NAME = "Rename";
    private static final String ICON_NAME = "icon-edit.png";

    private String actionName;

    public EditNodeAction(Application application) {
        super(ACTION_NAME, application);
    }

    public String getIconName() {
        return ICON_NAME;
    }

    @Override
    public void performAction(ActionEvent e) {
        if (tree.isEditing()) {
            return;
        }
        if (e != null) {
            if (tree.getSelectionPath() != null) {
                tree.startEditingAtPath(tree.getSelectionPath());
            }
        }
        if (actionName == null) {
            return;
        }
        if (tree.getSelectionPath() != null) {
            selectedElement = tree.getSelectedNode();
            parentElement = (DbImportTreeNode) selectedElement.getParent();
            if (parentElement != null) {
                Object selectedObject = selectedElement.getUserObject();
                ReverseEngineering reverseEngineeringOldCopy = new ReverseEngineering(tree.getReverseEngineering());
                if (!Util.isEmptyString(actionName)) {
                    if (selectedObject instanceof FilterContainer) {
                        ((FilterContainer) selectedObject).setName(actionName);
                    } else if (selectedObject instanceof PatternParam) {
                        ((PatternParam) selectedObject).setPattern(actionName);
                    }
                    updateModel(true);
                }
                if (!actionName.equals(EMPTY_NAME)) {
                    putReverseEngineeringToUndoManager(reverseEngineeringOldCopy);
                }
            }
            DbImportSorter.sortSingleNode(selectedElement.getParent(),DbImportSorter.NODE_COMPARATOR_BY_TYPE);
            tree.reloadModelKeepingExpanded();
            tree.setSelectionPath(new TreePath(selectedElement.getPath()));
            selectedElement = null;
        }
    }

    public void setActionName(String name) {
        this.actionName = name;
    }

}
