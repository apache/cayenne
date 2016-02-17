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

import org.apache.cayenne.modeler.dialog.db.model.DBCatalog;
import org.apache.cayenne.modeler.dialog.db.model.DBColumn;
import org.apache.cayenne.modeler.dialog.db.model.DBEntity;
import org.apache.cayenne.modeler.dialog.db.model.DBProcedure;
import org.apache.cayenne.modeler.dialog.db.model.DBSchema;
import org.apache.cayenne.modeler.util.CellRenderers;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.Component;

/**
 * @since 4.0
 */
public class FilteredTreeCellRenderer extends DefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean sel,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus) {

        super.getTreeCellRendererComponent(
                tree, value, sel,
                expanded, leaf, row,
                hasFocus);
        if ((value != null) && (value instanceof DefaultMutableTreeNode)) {
            Object userObject = ((DefaultMutableTreeNode) value)
                    .getUserObject();
            ImageIcon imageIcon = CellRenderers.iconForObject(userObject);

            if (userObject instanceof DBCatalog) {
                setText(((DBCatalog) userObject).getName());
                setIcon(imageIcon);
            }

            if (userObject instanceof DBSchema) {
                setText(((DBSchema) userObject).getName());
                setIcon(imageIcon);
            }

            if (userObject instanceof DBEntity) {
                setText(((DBEntity) userObject).getName());
                setIcon(imageIcon);
            }

            if (userObject instanceof DBColumn) {
                setText(((DBColumn) userObject).getName());
                setIcon(imageIcon);
            }

            if (userObject instanceof DBProcedure) {
                setText(((DBProcedure) userObject).getName());
                setIcon(imageIcon);
            }
        }
        return this;
    }
}
