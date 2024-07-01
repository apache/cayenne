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

import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.action.dbimport.TreeManipulationAction;
import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;
import org.apache.cayenne.modeler.editor.dbimport.DbImportSorter;

import java.awt.event.ActionEvent;
import java.util.List;

/**
 * @since 5.0
 */
public class SortNodesAction extends TreeManipulationAction {

    private static final String ACTION_NAME = "Sort";
    private static final String ICON_NAME = "icon-dbi-sort.png";

    public SortNodesAction(Application application) {
        super(ACTION_NAME, application);
    }

    public String getIconName() {
        return ICON_NAME;
    }

    @Override
    public void performAction(ActionEvent e) {
        tree.stopEditing();
        ReverseEngineering reverseEngineeringOldCopy = new ReverseEngineering(tree.getReverseEngineering());
        List<DbImportTreeNode> treeExpandList = tree.getTreeExpandList();
        DbImportSorter.sortSubtree(tree.getRootNode(), DbImportSorter.NODE_COMPARATOR_BY_TYPE_BY_NAME);
        putReverseEngineeringToUndoManager(reverseEngineeringOldCopy);
        getProjectController().setDirty(true);
        tree.reloadModelKeepingExpanded();
        tree.expandTree(treeExpandList);
    }

}
