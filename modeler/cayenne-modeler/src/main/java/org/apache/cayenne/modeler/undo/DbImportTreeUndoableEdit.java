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

package org.apache.cayenne.modeler.undo;

import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;
import org.apache.cayenne.modeler.editor.dbimport.DbImportTree;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import java.util.List;

/**
 * @since 4.1
 */
public class DbImportTreeUndoableEdit extends AbstractUndoableEdit {

    private ReverseEngineering previousReverseEngineering;
    private ReverseEngineering nextReverseEngineering;
    private DbImportTree tree;
    private ProjectController projectController;

    public DbImportTreeUndoableEdit(ReverseEngineering previousReverseEngineering, ReverseEngineering nextReverseEngineering,
                                    DbImportTree tree, ProjectController projectController) {
        this.tree = tree;
        this.previousReverseEngineering = previousReverseEngineering;
        this.nextReverseEngineering = nextReverseEngineering;
        this.projectController = projectController;
    }

    @Override
    public boolean canRedo() {
        return true;
    }

    @Override
    public void redo() throws CannotRedoException {
        tree.stopEditing();
        tree.setReverseEngineering(this.nextReverseEngineering);
        List<DbImportTreeNode> list = tree.getTreeExpandList();
        projectController.getApplication().getMetaData().add(projectController.getCurrentDataMap(), tree.getReverseEngineering());
        projectController.setDirty(true);
        tree.translateReverseEngineeringToTree(tree.getReverseEngineering(), false);
        tree.expandTree(list);
    }

    @Override
    public void undo() throws CannotUndoException {
        tree.stopEditing();
        tree.setReverseEngineering(this.previousReverseEngineering);
        List<DbImportTreeNode> list = tree.getTreeExpandList();
        projectController.getApplication().getMetaData().add(projectController.getCurrentDataMap(), tree.getReverseEngineering());
        projectController.setDirty(true);
        tree.translateReverseEngineeringToTree(tree.getReverseEngineering(), false);
        tree.expandTree(list);
    }
}
