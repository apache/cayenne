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

import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;
import org.apache.cayenne.modeler.editor.dbimport.tree.NodeType;
import org.apache.cayenne.modeler.util.ModelerUtil;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 4.1
 */
public class DbImportTreeCellRenderer extends DefaultTreeCellRenderer {

    protected DbImportTreeNode node;
    private Map<NodeType, String> icons;
    private Map<NodeType, String> markedIcons;
    private Map<NodeType, String> transferableTreeIcons;

    public DbImportTreeCellRenderer() {
        super();
        initIcons();
        initPinnedIcons();
        initTransferableTreeIcons();
    }

    private void initTransferableTreeIcons() {
        transferableTreeIcons = new HashMap<>();
        transferableTreeIcons.put(NodeType.CATALOG, "icon-dbi-catalog.png");
        transferableTreeIcons.put(NodeType.SCHEMA, "icon-dbi-schema.png");
        transferableTreeIcons.put(NodeType.INCLUDE_TABLE, "icon-dbentity.png");
        transferableTreeIcons.put(NodeType.INCLUDE_PROCEDURE, "icon-stored-procedure.png");
        transferableTreeIcons.put(NodeType.INCLUDE_COLUMN, "icon-dbi-column.png");
    }

    private void initIcons() {
        icons = new HashMap<>();
        icons.put(NodeType.CATALOG, "icon-dbi-catalog.png");
        icons.put(NodeType.SCHEMA, "icon-dbi-schema.png");
        icons.put(NodeType.INCLUDE_TABLE, "icon-dbi-includeTable.png");
        icons.put(NodeType.EXCLUDE_TABLE, "icon-dbi-excludeTable.png");
        icons.put(NodeType.INCLUDE_COLUMN, "icon-dbi-includeColumn.png");
        icons.put(NodeType.EXCLUDE_COLUMN, "icon-dbi-excludeColumn.png");
        icons.put(NodeType.INCLUDE_PROCEDURE, "icon-dbi-includeProcedure.png");
        icons.put(NodeType.EXCLUDE_PROCEDURE, "icon-dbi-excludeProcedure.png");
    }

    private void initPinnedIcons() {
        markedIcons = new HashMap<>();
        markedIcons.put(NodeType.INCLUDE_TABLE, "icon-dbi-pinnedIncludeTable.png");
        markedIcons.put(NodeType.EXCLUDE_TABLE, "icon-dbi-pinnedExcludeTable.png");
        markedIcons.put(NodeType.INCLUDE_COLUMN, "icon-dbi-pinnedIncludeColumn.png");
        markedIcons.put(NodeType.EXCLUDE_COLUMN, "icon-dbi-pinnedExcludeColumn.png");
        markedIcons.put(NodeType.INCLUDE_PROCEDURE, "icon-dbi-pinnedIncludeProcedure.png");
        markedIcons.put(NodeType.EXCLUDE_PROCEDURE, "icon-dbi-pinnedExcludeProcedure.png");
    }

    private ImageIcon getIconByNodeType(NodeType type, boolean isTransferable) {
        String iconName = !isTransferable ? icons.get(type) : transferableTreeIcons.get(type);
        if (iconName == null) {
            return null;
        }
        return ModelerUtil.buildIcon(iconName);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree,
                                                  Object value,
                                                  boolean sel,
                                                  boolean expanded,
                                                  boolean leaf, int row,
                                                  boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        node = (DbImportTreeNode) value;
        if (node.isPinned() && node.getNodeType() != NodeType.CATALOG && node.getNodeType() != NodeType.SCHEMA) {
            setIcon(ModelerUtil.buildIcon(markedIcons.get(node.getNodeType())));
        } else {
            setIcon(getIconByNodeType(node.getNodeType(), ((DbImportTree) tree).isTransferable()));
        }
        return this;
    }
}
