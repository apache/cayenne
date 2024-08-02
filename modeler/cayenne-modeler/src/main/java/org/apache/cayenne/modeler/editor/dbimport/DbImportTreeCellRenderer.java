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

import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;
import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;
import org.apache.cayenne.modeler.util.ModelerUtil;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.Component;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 4.1
 */
public class DbImportTreeCellRenderer extends DefaultTreeCellRenderer {

    protected DbImportTreeNode node;
    private Map<Class<?>, String> icons;
    private Map<Class<?>, String> transferableTreeIcons;

    public DbImportTreeCellRenderer() {
        initIcons();
        initTransferableTreeIcons();
    }

    private void initTransferableTreeIcons() {
        transferableTreeIcons = new HashMap<>();
        transferableTreeIcons.put(Catalog.class, "icon-dbi-catalog.png");
        transferableTreeIcons.put(Schema.class, "icon-dbi-schema.png");
        transferableTreeIcons.put(IncludeTable.class, "icon-dbentity.png");
        transferableTreeIcons.put(IncludeProcedure.class, "icon-stored-procedure.png");
        transferableTreeIcons.put(IncludeColumn.class, "icon-dbi-column.png");
    }

    private void initIcons() {
        icons = new HashMap<>();
        icons.put(Catalog.class, "icon-dbi-catalog.png");
        icons.put(Schema.class, "icon-dbi-schema.png");
        icons.put(IncludeTable.class, "icon-dbi-includeTable.png");
        icons.put(ExcludeTable.class, "icon-dbi-excludeTable.png");
        icons.put(IncludeColumn.class, "icon-dbi-includeColumn.png");
        icons.put(ExcludeColumn.class, "icon-dbi-excludeColumn.png");
        icons.put(IncludeProcedure.class, "icon-dbi-includeProcedure.png");
        icons.put(ExcludeProcedure.class, "icon-dbi-excludeProcedure.png");
    }

    private ImageIcon getIconByNodeType(Class<?> nodeClass, boolean isTransferable) {
        String iconName = !isTransferable ? icons.get(nodeClass) : transferableTreeIcons.get(nodeClass);
        if (iconName == null) {
            return null;
        }
        return ModelerUtil.buildIcon(iconName);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean sel,
                                                  boolean expanded,
                                                  boolean leaf, int row,
                                                  boolean hasFocus) {

        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        node = (DbImportTreeNode) value;
        setIcon(getIconByNodeType(node.getUserObject().getClass(), ((DbImportTree) tree).isTransferable()));
        return value instanceof DbImportTreeNode.ExpandableEnforcerNode
                ? ExpandableEnforcer.getInstance()
                : this;
    }

    @Override
    public Icon getLeafIcon() {
        return null;
    }

    @Override
    public Icon getOpenIcon() {
        return null;
    }

    @Override
    public Icon getClosedIcon() {
        return null;
    }

    private static class ExpandableEnforcer extends JLabel {

        private static ExpandableEnforcer instance;

        public ExpandableEnforcer() {
            setPreferredSize(new Dimension(0, 0));
        }

        public static ExpandableEnforcer getInstance() {
            if (instance == null) {
                instance = new ExpandableEnforcer();
            }
            return instance;
        }

        @Override
        public Dimension getPreferredSize() {
            return super.getPreferredSize();
        }
    }
}
