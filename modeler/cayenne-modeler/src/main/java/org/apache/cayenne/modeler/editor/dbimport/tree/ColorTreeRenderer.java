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

package org.apache.cayenne.modeler.editor.dbimport.tree;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JTree;

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
import org.apache.cayenne.dbsync.reverse.dbimport.SchemaContainer;
import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;
import org.apache.cayenne.modeler.editor.dbimport.DbImportTree;
import org.apache.cayenne.modeler.editor.dbimport.DbImportTreeCellRenderer;

/**
 * @since 4.1
 */
public class ColorTreeRenderer extends DbImportTreeCellRenderer {

    private DbImportTree reverseEngineeringTree;

    public ColorTreeRenderer() {
        super();
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                  boolean selected, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

        if (this.node.isLabel() || this.selected) {
            setForeground(Color.BLACK);
            return this;
        }

        Node<?> logicalTreeNode = getLogicalTreeNode();
        if(logicalTreeNode == null) {
            return this;
        }

        Status status = logicalTreeNode.getStatus(reverseEngineeringTree.getReverseEngineering());
        ReverseEngineering mask = getMask();
        if (mask != null) {
            status = logicalTreeNode.getStatus(mask);
        }

        setForeground(status.getColor());
        return this;
    }

    private ReverseEngineering merge(ReverseEngineering config, Object object) {
        if(mergeAsFilterContainer(config, object)) {
            return config;
        } else if(mergeAsSchemaContainer(config, object)) {
            return config;
        } else if(object instanceof Catalog) {
            config.addCatalog((Catalog)object);
        }
        return config;
    }

    private Object merge(Catalog catalog, Object object) {
        if(mergeAsFilterContainer(catalog, object)) {
            return catalog;
        }
        mergeAsSchemaContainer(catalog, object);
        return catalog;
    }

    private Object merge(Schema schema, Object object) {
        mergeAsFilterContainer(schema, object);
        return schema;
    }

    private boolean mergeAsSchemaContainer(SchemaContainer container, Object object) {
        if(object instanceof Schema) {
            container.addSchema((Schema)object);
            return true;
        }
        return false;
    }

    private boolean mergeAsFilterContainer(FilterContainer container, Object object) {
        if(object instanceof IncludeTable) {
            container.addIncludeTable((IncludeTable)object);
            container.addIncludeProcedure(new IncludeProcedure("tmp include to disable include all behaviour"));
            return true;
        } else if(object instanceof ExcludeTable) {
            container.addExcludeTable((ExcludeTable)object);
            container.addIncludeProcedure(new IncludeProcedure("tmp include to disable include all behaviour"));
            container.addIncludeTable(new IncludeTable("tmp include to disable include all behaviour"));
            return true;
        } else if(object instanceof IncludeProcedure) {
            container.addIncludeProcedure((IncludeProcedure)object);
            return true;
        } else if(object instanceof ExcludeProcedure) {
            container.addExcludeProcedure((ExcludeProcedure)object);
            container.addIncludeProcedure(new IncludeProcedure("tmp include to disable include all behaviour"));
            return true;
        } else if(object instanceof IncludeColumn) {
            container.addIncludeColumn((IncludeColumn)object);
            return true;
        } else if(object instanceof ExcludeColumn) {
            container.addExcludeColumn((ExcludeColumn)object);
            return true;
        }
        return false;
    }

    private IncludeTable merge(IncludeTable includeTable, Object object) {
        if(object instanceof IncludeColumn) {
            includeTable.addIncludeColumn((IncludeColumn)object);
        } else if(object instanceof ExcludeColumn) {
            includeTable.addExcludeColumn((ExcludeColumn)object);
            includeTable.addIncludeColumn(new IncludeColumn("tmp include to disable include all behaviour"));
        }
        return includeTable;
    }

    private Object addToMask(Object object, DbImportTreeNode node) {
        if(object == null) {
            return node.getUserObject();
        }
        String rule = getObjectValue(node.getUserObject());
        if(node.isCatalog()) {
            return merge(new Catalog(rule), object);
        } else if(node.isSchema()) {
            return merge(new Schema(rule), object);
        } else if(node.isIncludeTable()) {
            return merge(new IncludeTable(rule), object);
        } else if(node.isExcludeTable()) {
            return new ExcludeTable(rule);
        } else if(node.isIncludeColumn()) {
            return new IncludeColumn(rule);
        } else if(node.isExcludeColumn()) {
            return new ExcludeColumn(rule);
        } else if(node.isIncludeProcedure()) {
            return new IncludeProcedure(rule);
        } else if(node.isExcludeProcedure()) {
            return new ExcludeProcedure(rule);
        }

        return object;
    }

    private ReverseEngineering getMask() {
        DbImportTreeNode selectedNode = reverseEngineeringTree.getSelectedNode();
        if(selectedNode == null) {
            return null;
        }

        if(selectedNode.isReverseEngineering()) {
            return reverseEngineeringTree.getReverseEngineering();
        }

        ReverseEngineering config = new ReverseEngineering();
        Object configNode = null;
        while(!selectedNode.isReverseEngineering()) {
            configNode = addToMask(configNode, selectedNode);
            selectedNode = selectedNode.getParent();
        }
        return merge(config, configNode);
    }

    private Node<?> getLogicalTreeNode() {
        List<Object> path = new ArrayList<>();
        DbImportTreeNode parent = node;
        while (parent != null) {
            path.add(parent.getUserObject());
            parent = parent.getParent();
        }
        Collections.reverse(path);

        Node<?> logicalParent = null;
        for(Object object : path) {
            logicalParent = toLogicalNode(getObjectType(object), getObjectValue(object), logicalParent);
        }
        return logicalParent;
    }

    private Node<?> toLogicalNode(ObjectType type, String value, Node<?> logicalParent) {
        switch (type) {
            case CATALOG:
                return new CatalogNode(value);
            case SCHEMA:
                return new SchemaNode(value, (CatalogNode)logicalParent);
            case TABLE:
                if(logicalParent instanceof CatalogNode) {
                    return new CatalogTableNode(value, (CatalogNode)logicalParent);
                } else {
                    return new SchemaTableNode(value, (SchemaNode)logicalParent);
                }
            case COLUMN:
                return new ColumnNode(value, (TableNode<?>) logicalParent);
            case PROCEDURE:
                if(logicalParent instanceof CatalogNode) {
                    return new CatalogProcedureNode(value, (CatalogNode)logicalParent);
                } else {
                    return new SchemaProcedureNode(value, (SchemaNode)logicalParent);
                }
            default:
                return null;
        }
    }

    private ObjectType getObjectType(Object object) {
        if(object instanceof Catalog) {
            return ObjectType.CATALOG;
        } else if(object instanceof Schema) {
            return ObjectType.SCHEMA;
        } else if(object instanceof IncludeTable || object instanceof ExcludeTable) {
            return ObjectType.TABLE;
        } else if(object instanceof IncludeColumn || object instanceof ExcludeColumn) {
            return ObjectType.COLUMN;
        } else if(object instanceof IncludeProcedure || object instanceof ExcludeProcedure) {
            return ObjectType.PROCEDURE;
        }
        return ObjectType.UNKNOWN;
    }

    private String getObjectValue(Object object) {
        if(object instanceof FilterContainer) {
            return ((FilterContainer) object).getName();
        } else if(object instanceof PatternParam) {
            return ((PatternParam) object).getPattern();
        }
        return "";
    }

    public void setReverseEngineeringTree(DbImportTree reverseEngineeringTree) {
        this.reverseEngineeringTree = reverseEngineeringTree;
    }

}
