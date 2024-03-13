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
    public static final Comparator<DbImportTreeNode> NODE_COMPARATOR_BY_TYPE = Comparator
            .comparing(DbImportTreeNode::getNodeType);


    public static final Comparator<DbImportTreeNode> NODE_COMPARATOR_BY_TYPE_BY_NAME = Comparator
            .comparing(DbImportTreeNode::getNodeType)
            .thenComparing(DbImportTreeNode::getSimpleNodeName);

    public static void sortSingleNode(DbImportTreeNode node, Comparator<DbImportTreeNode> comparator) {
        sortNodeItems(node, comparator);
        syncUserObjectItems(node);
    }

    public static void sortSubtree(DbImportTreeNode root,Comparator<DbImportTreeNode> comparator) {
        sortSingleNode(root, comparator);
        root.getChildNodes().forEach(r -> sortSubtree(r, comparator));
    }

    private static void sortNodeItems(DbImportTreeNode node, Comparator<DbImportTreeNode> comparator) {
        List<DbImportTreeNode> childNodes = node.getChildNodes();
        node.removeAllChildren();
        childNodes.sort(comparator);
        childNodes.forEach(node::add);
    }

    public static void syncUserObjectItems(DbImportTreeNode parentNode) {

        Object userObject = parentNode.getUserObject();

        if (parentNode.isReverseEngineering()) {
            syncCatalogs(parentNode, (ReverseEngineering) userObject);
            syncSchemas(parentNode, (ReverseEngineering) userObject);
            syncPatternParamsInContainer(parentNode, (ReverseEngineering) userObject);
        }
        if (parentNode.isCatalog()) {
            syncSchemas(parentNode, (Catalog) userObject);
            syncPatternParamsInContainer(parentNode, (Catalog) userObject);
        }
        if (parentNode.isSchema()) {
            syncPatternParamsInContainer(parentNode, (Schema) userObject);
        }
        if (parentNode.isIncludeTable()) {
            syncTablesColumns(parentNode, (IncludeTable) userObject);
        }
    }

    private static void syncCatalogs(DbImportTreeNode node, ReverseEngineering reverseEngineering) {
        syncPatternParams(reverseEngineering.getCatalogs(), node, NodeType.CATALOG);
    }

    private static void syncSchemas(DbImportTreeNode node, SchemaContainer schemaContainer) {
        syncPatternParams(schemaContainer.getSchemas(), node, NodeType.SCHEMA);
    }

    private static void syncPatternParamsInContainer(DbImportTreeNode node, FilterContainer container) {
        syncPatternParams(container.getIncludeTables(), node, NodeType.INCLUDE_TABLE);
        syncPatternParams(container.getExcludeTables(), node, NodeType.EXCLUDE_TABLE);
        syncPatternParams(container.getIncludeColumns(), node, NodeType.INCLUDE_COLUMN);
        syncPatternParams(container.getExcludeColumns(), node, NodeType.EXCLUDE_COLUMN);
        syncPatternParams(container.getIncludeProcedures(), node, NodeType.INCLUDE_PROCEDURE);
        syncPatternParams(container.getExcludeProcedures(), node, NodeType.EXCLUDE_PROCEDURE);
    }

    private static void syncTablesColumns(DbImportTreeNode node, IncludeTable table) {
        syncPatternParams(table.getIncludeColumns(), node, NodeType.INCLUDE_COLUMN);
        syncPatternParams(table.getExcludeColumns(), node, NodeType.EXCLUDE_COLUMN);
    }

    private static <T> void syncPatternParams(Collection<T> collection, DbImportTreeNode node, NodeType type) {
        collection.clear();
        collection.addAll(node.getChildrenObjectsByType(type));
    }
}
