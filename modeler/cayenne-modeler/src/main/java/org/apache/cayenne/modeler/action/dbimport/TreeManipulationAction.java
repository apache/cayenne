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

import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;
import org.apache.cayenne.modeler.Application;
import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;
import org.apache.cayenne.modeler.editor.dbimport.DbImportModel;
import org.apache.cayenne.modeler.editor.dbimport.DbImportTree;
import org.apache.cayenne.modeler.undo.DbImportTreeUndoableEdit;
import org.apache.cayenne.modeler.util.CayenneAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JTree;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 4.1
 */
public abstract class TreeManipulationAction extends CayenneAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(TreeManipulationAction.class);

    static final String EMPTY_NAME = "";

    protected DbImportTree tree;
    protected DbImportTreeNode selectedElement;
    DbImportTreeNode parentElement;
    DbImportTreeNode foundNode;
    String insertableNodeName;
    Class<?> insertableNodeClass;
    boolean isMultipleAction;
    private boolean movedFromDbSchema;
    private Map<Class<?>, List<Class<?>>> levels;
    protected String name;
    protected boolean updateSelected;

    public TreeManipulationAction(String name, Application application) {
        super(name, application);
        initLevels();
    }

    void completeInserting(ReverseEngineering reverseEngineeringOldCopy) {
        if (!isMultipleAction) {
            updateAfterInsert();
        }
        if ((!isMultipleAction) && (!insertableNodeName.equals(EMPTY_NAME))) {
            putReverseEngineeringToUndoManager(reverseEngineeringOldCopy);
        }
    }

    private String getNodeName() {
        return insertableNodeName != null ? insertableNodeName : EMPTY_NAME;
    }

    protected ReverseEngineering prepareElements() {
        name = getNodeName();
        tree.stopEditing();
        if (tree.getSelectionPath() == null) {
            TreePath root = new TreePath(tree.getRootNode());
            tree.setSelectionPath(root);
        }
        if (foundNode == null) {
            selectedElement = tree.getSelectedNode();
        } else {
            selectedElement = foundNode;
        }
        parentElement = selectedElement.getParent();
        if (parentElement == null) {
            parentElement = selectedElement;
        }
        if (reverseEngineeringIsEmpty()) {
            tree.getRootNode().removeAllChildren();
        }
        return new ReverseEngineering(tree.getReverseEngineering());
    }

    protected void putReverseEngineeringToUndoManager(ReverseEngineering reverseEngineeringOldCopy) {
        ReverseEngineering reverseEngineeringNewCopy = new ReverseEngineering(tree.getReverseEngineering());
        DbImportTreeUndoableEdit undoableEdit = new DbImportTreeUndoableEdit(
                reverseEngineeringOldCopy, reverseEngineeringNewCopy, tree, getProjectController()
        );
        getProjectController().getApplication().getUndoManager().addEdit(undoableEdit);
    }

    boolean reverseEngineeringIsEmpty() {
        ReverseEngineering reverseEngineering = tree.getReverseEngineering();
        return ((reverseEngineering.getCatalogs().size() == 0) && (reverseEngineering.getSchemas().size() == 0)
                && (reverseEngineering.getIncludeTables().size() == 0) && (reverseEngineering.getExcludeTables().size() == 0)
                && (reverseEngineering.getIncludeColumns().size() == 0) && (reverseEngineering.getExcludeColumns().size() == 0)
                && (reverseEngineering.getIncludeProcedures().size() == 0) && (reverseEngineering.getExcludeProcedures().size() == 0));
    }

    private void initLevels() {
        levels = new HashMap<>();

        List<Class<?>> schemaChildren = Arrays.asList(
                IncludeTable.class, ExcludeTable.class,
                IncludeColumn.class, ExcludeColumn.class,
                IncludeProcedure.class, ExcludeProcedure.class
        );
        List<Class<?>> rootChildren = new ArrayList<>(schemaChildren);
        rootChildren.add(Schema.class);

        levels.put(ReverseEngineering.class, rootChildren);
        levels.put(Catalog.class, rootChildren);
        levels.put(Schema.class, schemaChildren);
        levels.put(IncludeTable.class, Arrays.asList(IncludeColumn.class, ExcludeColumn.class));
        levels.put(ExcludeTable.class, Collections.emptyList());
        levels.put(IncludeColumn.class, Collections.emptyList());
        levels.put(ExcludeColumn.class, Collections.emptyList());
        levels.put(IncludeProcedure.class, Collections.emptyList());
        levels.put(ExcludeProcedure.class, Collections.emptyList());
    }

    public void setTree(DbImportTree tree) {
        this.tree = tree;
    }

    public JTree getTree() {
        return tree;
    }

    boolean canBeInserted(DbImportTreeNode node) {
        if (node == null) {
            return false;
        }
        Class<?> selectedObjectClass = node.getUserObject().getClass();
        List<Class<?>> classes = levels.get(selectedObjectClass);
        if(classes == null) {
            LOGGER.warn("Trying to insert node of the unknown class '" + selectedObjectClass.getName() + "' to the dbimport tree.");
            return false;
        }
        return classes.contains(insertableNodeClass);
    }

    boolean canInsert() {
        if (selectedElement == null) {
            return true;
        }
        if (parentElement != null) {
            for (int i = 0; i < parentElement.getChildCount(); i++) {
                DbImportTreeNode child = (DbImportTreeNode) parentElement.getChildAt(i);
                if (child.getSimpleNodeName().equals(insertableNodeName)
                        && (child.getUserObject().getClass() == insertableNodeClass)) {
                    return false;
                }
            }
        }
        return true;
    }

    protected void updateModel(boolean updateSelected) {
        insertableNodeName = null;
        DbImportModel model = (DbImportModel) tree.getModel();
        getProjectController().setDirty(true);
        TreePath savedPath = null;
        if (!updateSelected) {
            savedPath = new TreePath(parentElement.getPath());
        }
        tree.reloadModelKeepingExpanded(updateSelected ? selectedElement : parentElement);
        if ((savedPath != null) && (parentElement.getUserObject().getClass() != ReverseEngineering.class)) {
            tree.setSelectionPath(savedPath);
        }
    }

    void updateAfterInsert() {
        updateModel(updateSelected);
        if (!movedFromDbSchema) {
            if (updateSelected) {
                tree.startEditingAtPath(new TreePath(((DbImportTreeNode) selectedElement.getLastChild()).getPath()));
            } else {
                tree.startEditingAtPath(new TreePath(((DbImportTreeNode) parentElement.getLastChild()).getPath()));
            }
        }
        resetActionFlags();
    }

    public void resetActionFlags() {
        movedFromDbSchema = false;
        isMultipleAction = false;
        insertableNodeName = "";
    }

    void setInsertableNodeName(String nodeName) {
        this.insertableNodeName = nodeName;
    }

    void setMultipleAction(boolean multipleAction) {
        isMultipleAction = multipleAction;
    }

    void setMovedFromDbSchema(boolean movedFromDbSchema) {
        this.movedFromDbSchema = movedFromDbSchema;
    }

    void setFoundNode(DbImportTreeNode node) {
        this.foundNode = node;
    }
}
