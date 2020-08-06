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

package org.apache.cayenne.modeler.dialog.db.load;

import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.modeler.action.dbimport.DeleteNodeAction;
import org.apache.cayenne.modeler.action.dbimport.EditNodeAction;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTree;

/**
 * @since 4.1
 */
public class DefaultPopUpMenu extends JPopupMenu {

    protected JMenuItem rename;
    protected JMenuItem delete;
    protected DbImportTreeNode selectedElement;
    protected DbImportTreeNode parentElement;
    protected JTree tree;
    protected ProjectController projectController;

    public DefaultPopUpMenu() {
        rename = new JMenuItem("Rename");
        delete = new JMenuItem("Delete");
        this.add(rename);
        this.add(delete);
        initListeners();
    }

    private void initListeners() {
        rename.addActionListener(e -> {
            if ((selectedElement != null) && (parentElement != null)) {
                projectController.getApplication().getActionManager().getAction(EditNodeAction.class).actionPerformed(e);
            }
        });
        delete.addActionListener(e -> {
            if ((selectedElement != null) && (parentElement != null)) {
                projectController.getApplication().getActionManager().getAction(DeleteNodeAction.class).actionPerformed(e);
            }
        });
    }

    public void setSelectedElement(DbImportTreeNode selectedElement) {
        this.selectedElement = selectedElement;
    }

    public void setParentElement(DbImportTreeNode parentElement) {
        this.parentElement = parentElement;
    }

    public void setTree(JTree tree) {
        this.tree = tree;
    }

    public void setProjectController(ProjectController projectController) {
        this.projectController = projectController;
    }
}
