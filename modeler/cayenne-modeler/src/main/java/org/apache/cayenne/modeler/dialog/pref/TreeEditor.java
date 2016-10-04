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
package org.apache.cayenne.modeler.dialog.pref;

import org.apache.cayenne.modeler.dialog.db.model.DbCatalog;
import org.apache.cayenne.modeler.dialog.db.model.DbElement;
import org.apache.cayenne.modeler.dialog.db.model.DbEntity;
import org.apache.cayenne.modeler.dialog.db.model.DbModel;
import org.apache.cayenne.modeler.dialog.db.model.DbSchema;
import org.apache.cayenne.modeler.util.CayenneController;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * @since 4.0
 */
public class TreeEditor extends CayenneController {
    protected TreeView treeView;
    protected DefaultMutableTreeNode root;

    public TreeEditor(CayenneController parent) {
        super(parent);

        this.treeView = new TreeView(root);
    }

    @Override
    public TreeView getView() {
        return treeView;
    }

    public void setRoot(String dataSource) {
        root = new DefaultMutableTreeNode(dataSource);
        DefaultTreeModel model = (DefaultTreeModel) treeView.getTree().getModel();
        model.setRoot(root);
    }

    public void convertTreeViewIntoTreeNode(DbModel dbModel) {
        DefaultMutableTreeNode modelNode = new DefaultMutableTreeNode(dbModel);
        for (DbElement dbElement: dbModel.getElements()) {
            DefaultMutableTreeNode elementNode = new DefaultMutableTreeNode(dbElement);
            if (dbElement instanceof DbCatalog) {
                parseCatalog(dbElement, elementNode);
            }
            if (dbElement instanceof DbSchema) {
                parseSchema(dbElement, elementNode);
            }
            modelNode.add(elementNode);
        }
        root = modelNode;
        DefaultTreeModel model = (DefaultTreeModel) treeView.getTree().getModel();
        model.setRoot(root);
    }

    public void parseCatalog(DbElement catalog, DefaultMutableTreeNode elementNode) {
        for (DbElement dbElement: catalog.getElements()) {
            DefaultMutableTreeNode element = new DefaultMutableTreeNode(dbElement);
            if (dbElement instanceof DbSchema) {
                parseSchema(dbElement, element);
            }
            if (dbElement instanceof DbEntity){
                parseEntity(dbElement, element);
            }
            elementNode.add(element);
        }
    }

    public void parseSchema(DbElement schema, DefaultMutableTreeNode elementNode) {
        for (DbElement element: schema.getElements()) {
            DefaultMutableTreeNode entityNode = new DefaultMutableTreeNode(element);
            if (element instanceof DbEntity) {
                parseEntity(element, entityNode);
            }
            elementNode.add(entityNode);
        }
    }

    private void parseEntity(DbElement entity, DefaultMutableTreeNode entityNode) {
        for (DbElement column: entity.getElements()) {
            DefaultMutableTreeNode columnNode = new DefaultMutableTreeNode(column);
            entityNode.add(columnNode);
        }
    }
}
