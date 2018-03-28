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

import javax.swing.JTree;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 4.1
 */
public abstract class TreeManipulationAction extends CayenneAction {

    static final String EMPTY_NAME = "";

    protected DbImportTree tree;
    protected DbImportTreeNode selectedElement;
    DbImportTreeNode parentElement;
    DbImportTreeNode foundNode;
    String insertableNodeName;
    Class insertableNodeClass;
    boolean isMultipleAction;
    private boolean movedFromDbSchema;
    private Map<Class, List<Class>> levels;
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
        parentElement = (DbImportTreeNode) selectedElement.getParent();
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

        List<Class> rootChilds = new ArrayList<>();
        rootChilds.add(Schema.class);
        rootChilds.add(IncludeTable.class);
        rootChilds.add(ExcludeTable.class);
        rootChilds.add(IncludeColumn.class);
        rootChilds.add(ExcludeColumn.class);
        rootChilds.add(IncludeProcedure.class);
        rootChilds.add(ExcludeProcedure.class);
        levels.put(ReverseEngineering.class, rootChilds);

        List<Class> catalogChilds = new ArrayList<>();
        catalogChilds.add(Schema.class);
        catalogChilds.add(IncludeTable.class);
        catalogChilds.add(ExcludeTable.class);
        catalogChilds.add(IncludeColumn.class);
        catalogChilds.add(ExcludeColumn.class);
        catalogChilds.add(IncludeProcedure.class);
        catalogChilds.add(ExcludeProcedure.class);
        levels.put(Catalog.class, catalogChilds);

        List<Class> schemaChilds = new ArrayList<>();
        schemaChilds.add(IncludeTable.class);
        schemaChilds.add(ExcludeTable.class);
        schemaChilds.add(IncludeColumn.class);
        schemaChilds.add(ExcludeColumn.class);
        schemaChilds.add(IncludeProcedure.class);
        schemaChilds.add(ExcludeProcedure.class);
        levels.put(Schema.class, schemaChilds);

        List<Class> includeTableChilds = new ArrayList<>();
        includeTableChilds.add(IncludeColumn.class);
        includeTableChilds.add(ExcludeColumn.class);
        levels.put(IncludeTable.class, includeTableChilds);
        levels.put(ExcludeTable.class, null);
        levels.put(IncludeColumn.class, null);
        levels.put(ExcludeColumn.class, null);
        levels.put(IncludeProcedure.class, null);
        levels.put(ExcludeProcedure.class, null);
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
        Class selectedObjectClass = node.getUserObject().getClass();
        List<Class> childs = levels.get(selectedObjectClass);
        return childs != null && childs.contains(insertableNodeClass);
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
        model.reload(updateSelected ? selectedElement : parentElement);
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
