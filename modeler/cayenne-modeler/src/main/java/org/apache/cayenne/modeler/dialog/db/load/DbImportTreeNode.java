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

import org.apache.cayenne.dbsync.reverse.dbimport.Catalog;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.ExcludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.FilterContainer;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeColumn;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeProcedure;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.PatternParam;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;
import org.apache.cayenne.modeler.editor.dbimport.tree.NodeType;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @since 4.1
 */
public class DbImportTreeNode extends DefaultMutableTreeNode {

    private boolean isLoaded;

    public DbImportTreeNode() {
        this(null);
    }

    public DbImportTreeNode(Object userObject) {
        this(userObject, true);
    }

    private DbImportTreeNode(Object userObject, boolean allowsChildren) {
        super();
        this.userObject = userObject;
        this.allowsChildren = allowsChildren;
        parent = null;

        if (userObject != null && (isCatalog() || isSchema() || isIncludeTable())) {
            add(new ExpandableEnforcerNode());
        }
    }

    public boolean isIncludeTable() {
        return (getUserObject().getClass() == IncludeTable.class);
    }

    public boolean isExcludeTable() {
        return (getUserObject().getClass() == ExcludeTable.class);
    }

    public boolean isIncludeColumn() {
        return (getUserObject().getClass() == IncludeColumn.class);
    }

    public boolean isExcludeColumn() {
        return (getUserObject().getClass() == ExcludeColumn.class);
    }

    public boolean isExcludeProcedure() {
        return (getUserObject().getClass() == ExcludeProcedure.class);
    }

    public boolean isIncludeProcedure() {
        return (getUserObject().getClass() == IncludeProcedure.class);
    }

    public boolean isLabel() {
        return (getUserObject().getClass() == String.class);
    }

    public boolean isSchema() {
        return (getUserObject().getClass() == Schema.class);
    }

    public boolean isCatalog() {
        return (getUserObject().getClass() == Catalog.class);
    }

    public boolean isReverseEngineering() {
        return (getUserObject().getClass() == ReverseEngineering.class);
    }

    public DbImportTreeNode(Object userObject) {
        this(userObject, true);
    }

    // Compare parents chain
    public boolean parentsIsEqual(DbImportTreeNode reverseEngineeringNode) {
        ArrayList<DbImportTreeNode> reverseEngineeringNodeParents;
        if (reverseEngineeringNode == null) {
                reverseEngineeringNodeParents = new ArrayList<>();
        } else {
             reverseEngineeringNodeParents = reverseEngineeringNode.getParents();
        }
        ArrayList<DbImportTreeNode> dbNodeParents = getParents();
        for (DbImportTreeNode node : reverseEngineeringNodeParents) {
            int deleteIndex = -1;
            for (int i = 0; i < dbNodeParents.size(); i++) {
                if (node.getSimpleNodeName().equals(dbNodeParents.get(i).getSimpleNodeName())) {
                    deleteIndex = i;
                    break;
                }
            }
            if (deleteIndex != -1) {
                dbNodeParents.remove(deleteIndex);
            } else {
                return false;
            }
        }
        return true;
    }

    // Create parents chain
    public ArrayList<DbImportTreeNode> getParents() {
        ArrayList<DbImportTreeNode> parents = new ArrayList<>();
        DbImportTreeNode tmpNode = this;
        while (tmpNode.getParent() != null) {
            parents.add(tmpNode.getParent());
            tmpNode = tmpNode.getParent();
        }
        return parents;
    }

    @Override
    public DbImportTreeNode getParent() {
        return (DbImportTreeNode)super.getParent();
    }

    protected String getFormattedName(String className, String nodeName) {
        if (nodeName == null) {
            return className;
        } else {
            return String.format("%s", nodeName);
        }
    }

    protected String getNodeName() {
        if (userObject instanceof FilterContainer) {
            return getFormattedName(userObject.getClass().getSimpleName(), ((FilterContainer) userObject).getName());
        } else if (userObject instanceof PatternParam) {
            return getFormattedName(userObject.getClass().getSimpleName(), ((PatternParam) userObject).getPattern());
        } else if (userObject != null) {
            return userObject.toString();
        }
        return "";
    }

    public String getSimpleNodeName() {
        if (userObject instanceof ReverseEngineering) {
            return "";
        } else if (userObject instanceof FilterContainer) {
            return ((FilterContainer) userObject).getName();
        } else if (userObject instanceof PatternParam) {
            return ((PatternParam) userObject).getPattern();
        }
        return "";
    }

    public String toString() {
        if (userObject == null) {
            return "";
        } else if (userObject instanceof ReverseEngineering) {
            return "Reverse Engineering Configuration:";
        } else {
            return getNodeName();
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        DbImportTreeNode objNode = (DbImportTreeNode) obj;
        if (!objNode.getSimpleNodeName().equals(this.getSimpleNodeName())) {
            return false;
        }
        if (objNode.getUserObject().getClass() != this.getUserObject().getClass()) {
            return false;
        }
        return true;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public void setLoaded(boolean loaded) {
        isLoaded = loaded;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<DbImportTreeNode> getChildNodes() {
        return (List) Collections.list(this.children());
    }

    public NodeType getNodeType() {
        if (userObject instanceof Catalog) {
            return NodeType.CATALOG;
        } else if (userObject instanceof Schema) {
            return NodeType.SCHEMA;
        } else if (userObject instanceof IncludeTable) {
            return NodeType.INCLUDE_TABLE;
        } else if (userObject instanceof ExcludeTable) {
            return NodeType.EXCLUDE_TABLE;
        } else if (userObject instanceof IncludeColumn) {
            return NodeType.INCLUDE_COLUMN;
        } else if (userObject instanceof ExcludeColumn) {
            return NodeType.EXCLUDE_COLUMN;
        } else if (userObject instanceof IncludeProcedure) {
            return NodeType.INCLUDE_PROCEDURE;
        } else if (userObject instanceof ExcludeProcedure) {
            return NodeType.EXCLUDE_PROCEDURE;
        }
        return NodeType.UNKNOWN;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getChildrenObjectsByType(NodeType type) {
        return getChildNodes()
                .stream()
                .filter(n -> type.equals(n.getNodeType()))
                .map(filteredNode -> (T) filteredNode.getUserObject())
                .collect(Collectors.toList());
    }

    @Override
    public DbImportTreeNode getLastChild() {
        return (DbImportTreeNode) super.getLastChild();
    }

    public static class ExpandableEnforcerNode extends DbImportTreeNode {

        public ExpandableEnforcerNode() {
            super("", false);
        }
    }
}
