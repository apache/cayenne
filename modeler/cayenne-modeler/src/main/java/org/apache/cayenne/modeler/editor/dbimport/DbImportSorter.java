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
import org.apache.cayenne.dbsync.reverse.dbimport.FilterContainer;
import org.apache.cayenne.dbsync.reverse.dbimport.IncludeTable;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.dbsync.reverse.dbimport.Schema;
import org.apache.cayenne.dbsync.reverse.dbimport.SchemaContainer;
import org.apache.cayenne.modeler.dialog.db.load.DbImportTreeNode;
import org.apache.cayenne.modeler.editor.dbimport.tree.NodeType;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * @since 5.0
 */
public class DbImportSorter {
    private static final Comparator<DbImportTreeNode> NODE_COMPARATOR = Comparator
            .comparing(DbImportTreeNode::getNodeType)
            .thenComparing(DbImportTreeNode::getSimpleNodeName);

    public static void sortNode(DbImportTreeNode node) {
        sortNodeItems(node);
        syncUserObjectItems(node);
    }

    public static void sortNodeWithAllChildren(DbImportTreeNode node) {
        DbImportSorter.sortNode(node);
        for (int i = 0; i < node.getChildCount(); i++) {
            sortNodeWithAllChildren((DbImportTreeNode) node.getChildAt(i));
        }
    }

    private static void sortNodeItems(DbImportTreeNode node) {
        List<DbImportTreeNode> childNodes = node.getChildNodes();
        node.removeAllChildren();
        childNodes.sort(NODE_COMPARATOR);
        childNodes.forEach(node::add);
    }

    private static void syncUserObjectItems(DbImportTreeNode parentNode) {

        Object userObject = parentNode.getUserObject();

        if (parentNode.isReverseEngineering()) {
            syncCatalogs(parentNode, (ReverseEngineering) userObject);
            syncSchemas(parentNode, (ReverseEngineering) userObject);
            syncPatternParams(parentNode, (ReverseEngineering) userObject);
        }
        if (parentNode.isCatalog()) {
            syncSchemas(parentNode, (Catalog) userObject);
            syncPatternParams(parentNode, (Catalog) userObject);
        }
        if (parentNode.isSchema()) {
            syncPatternParams(parentNode, (Schema) userObject);
        }
        if (parentNode.isIncludeTable()) {
            syncTablesColumns(parentNode, (IncludeTable) userObject);
        }
    }

    private static void syncCatalogs(DbImportTreeNode node, ReverseEngineering reverseEngineering) {
        syncItemsInContainer(reverseEngineering.getCatalogs(),node,NodeType.CATALOG);
    }

    private static void syncSchemas(DbImportTreeNode node, SchemaContainer schemaContainer) {
        syncItemsInContainer(schemaContainer.getSchemas(),node,NodeType.SCHEMA);
    }

    private static void syncPatternParams(DbImportTreeNode node, FilterContainer container) {
        syncItemsInContainer(container.getIncludeTables(),node,NodeType.INCLUDE_TABLE);
        syncItemsInContainer(container.getExcludeTables(),node,NodeType.EXCLUDE_TABLE);
        syncItemsInContainer(container.getIncludeColumns(),node,NodeType.INCLUDE_COLUMN);
        syncItemsInContainer(container.getExcludeColumns(),node,NodeType.EXCLUDE_COLUMN);
        syncItemsInContainer(container.getIncludeProcedures(),node,NodeType.INCLUDE_PROCEDURE);
        syncItemsInContainer(container.getExcludeProcedures(),node,NodeType.EXCLUDE_PROCEDURE);
    }

    private static void syncTablesColumns(DbImportTreeNode node, IncludeTable table) {
        syncItemsInContainer(table.getIncludeColumns(),node,NodeType.INCLUDE_COLUMN);
        syncItemsInContainer(table.getExcludeColumns(),node,NodeType.EXCLUDE_COLUMN);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void syncItemsInContainer(Collection collection, DbImportTreeNode node, NodeType type){
        collection.clear();
        collection.addAll(node.getChildrenObjectsByType(type));
    }
}
