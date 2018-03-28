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

import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.FilterContainer;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.PatternParam;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;

import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;

/**
 * @since 4.1
 */
public abstract class AddPatternParamAction extends TreeManipulationAction {

    private Class paramClass;

    AddPatternParamAction(String name, Application application) {
        super(name, application);
    }

    private void addPatternParamToContainer(Class paramClass, Object selectedObject, String name, DbImportTreeNode node) {
        FilterContainer container = (FilterContainer) selectedObject;
        PatternParam element = null;
        if (paramClass == ExcludeTable.class) {
            element = new ExcludeTable(name);
            container.addExcludeTable((ExcludeTable) element);
        } else if (paramClass == IncludeColumn.class) {
            element = new IncludeColumn(name);
            container.addIncludeColumn((IncludeColumn) element);
        } else if (paramClass == ExcludeColumn.class) {
            element = new ExcludeColumn(name);
            container.addExcludeColumn((ExcludeColumn) element);
        } else if (paramClass == IncludeProcedure.class) {
            element = new IncludeProcedure(name);
            container.addIncludeProcedure((IncludeProcedure) element);
        } else if (paramClass == ExcludeProcedure.class) {
            element = new ExcludeProcedure(name);
            container.addExcludeProcedure((ExcludeProcedure) element);
        }
        node.add(new DbImportTreeNode(element));
    }

    private void addPatternParamToIncludeTable(Class paramClass, Object selectedObject, String name, DbImportTreeNode node) {
        IncludeTable includeTable = (IncludeTable) selectedObject;
        PatternParam element = null;
        if (paramClass == IncludeColumn.class) {
            element = new IncludeColumn(name);
            includeTable.addIncludeColumn((IncludeColumn) element);

        } else if (paramClass == ExcludeColumn.class) {
            element = new ExcludeColumn(name);
            includeTable.addExcludeColumn((ExcludeColumn) element);
        }
        node.add(new DbImportTreeNode(element));
    }

    @Override
    public void performAction(ActionEvent e) {
        ReverseEngineering reverseEngineeringOldCopy = prepareElements();
        Object selectedObject;
        if (reverseEngineeringIsEmpty()) {
            tree.getRootNode().removeAllChildren();
        }
        if (canBeInserted(selectedElement)) {
            selectedObject = selectedElement.getUserObject();
            if (selectedObject instanceof FilterContainer) {
                addPatternParamToContainer(paramClass, selectedObject, name, selectedElement);
            } else if (selectedObject instanceof IncludeTable) {
                addPatternParamToIncludeTable(paramClass, selectedObject, name, selectedElement);
            }
            updateSelected = true;
        } else {
            if (parentElement == null) {
                parentElement = tree.getRootNode();
            }
            selectedObject = parentElement.getUserObject();
            if (selectedObject instanceof FilterContainer) {
                addPatternParamToContainer(paramClass, selectedObject, name, parentElement);
            } else if (selectedObject instanceof IncludeTable) {
                addPatternParamToIncludeTable(paramClass, selectedObject, name, parentElement);
            }
            updateSelected = false;
        }
        completeInserting(reverseEngineeringOldCopy);
    }

    public void setParamClass(Class paramClass) {
        this.paramClass = paramClass;
    }
}
