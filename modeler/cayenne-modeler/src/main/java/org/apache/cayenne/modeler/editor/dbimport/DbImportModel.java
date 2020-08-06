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

package org.apache.cayenne.modeler.editor.dbimport;

import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

/**
 * @since 4.1
 */
public class DbImportModel extends DefaultTreeModel {

    private DbImportTree dbSchemaTree;
    private boolean canBeCleaned;

    DbImportModel(TreeNode root) {
        super(root);
    }

    private void clearReverseEngineering(ReverseEngineering reverseEngineering) {
        reverseEngineering.getSchemas().clear();
        reverseEngineering.getCatalogs().clear();
        reverseEngineering.getIncludeTables().clear();
        reverseEngineering.getExcludeTables().clear();
        reverseEngineering.getIncludeColumns().clear();
        reverseEngineering.getExcludeColumns().clear();
        reverseEngineering.getIncludeProcedures().clear();
        reverseEngineering.getExcludeProcedures().clear();
    }

    private void preprocessTree() {
        DbImportTreeNode rootNode = (DbImportTreeNode) getRoot();
        if (rootNode.getChildCount() == 0) {
            ReverseEngineering reverseEngineering = ((ReverseEngineering) rootNode.getUserObject());
            if (canBeCleaned) {
                clearReverseEngineering(reverseEngineering);
            }
            rootNode.add(new DbImportTreeNode(("Configuration is empty.")));
        }
    }

    public void reload(TreeNode node) {
        preprocessTree();
        super.reload(node);
        dbSchemaTree.repaint();
    }

    public void setDbSchemaTree(DbImportTree dbSchemaTree) {
        this.dbSchemaTree = dbSchemaTree;
    }

    public void setCanBeCleaned(boolean canBeCleaned) {
        this.canBeCleaned = canBeCleaned;
    }
}
