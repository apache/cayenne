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

package org.apache.cayenne.modeler.action.dbimport;

import org.apache.cayenne.dbsync.reverse.dbimport.FilterContainer;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;

import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;

/**
 * @since 4.1
 */
public class AddIncludeTableAction extends TreeManipulationAction {

    private static final String ACTION_NAME = "Add Include Table";
    private static final String ICON_NAME = "icon-dbi-includeTable.png";

    public AddIncludeTableAction(Application application) {
        super(ACTION_NAME, application);
        insertableNodeClass = IncludeTable.class;
    }

    public String getIconName() {
        return ICON_NAME;
    }

    @Override
    public void performAction(ActionEvent e) {
        ReverseEngineering reverseEngineeringOldCopy = prepareElements();
        if (reverseEngineeringIsEmpty()) {
            tree.getRootNode().removeAllChildren();
        }
        IncludeTable newTable = new IncludeTable(name);
        if (canBeInserted(selectedElement)) {
            ((FilterContainer) selectedElement.getUserObject()).addIncludeTable(newTable);
            selectedElement.add(new DbImportTreeNode(newTable));
            updateSelected = true;
        } else {
            if (parentElement == null) {
                parentElement = tree.getRootNode();
            }
            ((FilterContainer) parentElement.getUserObject()).addIncludeTable(newTable);
            parentElement.add(new DbImportTreeNode(newTable));
            updateSelected = false;
        }
        completeInserting(reverseEngineeringOldCopy);
    }
}
