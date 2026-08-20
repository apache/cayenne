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

package org.apache.cayenne.access.translator.select;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.JoinType;

/**
 * @since 4.2
 */
class TableTree {

    public static final String CURRENT_ALIAS = "__current_table_alias__";
    public static final CayennePath CURRENT_ALIAS_PATH = CayennePath.of("__current_table_alias__");

    /**
     * Tables mapped by db path it's spawned by.
     * Can be following:
     * - query root table
     * - tables from flattened attributes (including all intermediate tables)
     * - tables from attributes used in expressions (WHERE, HAVING, ORDER BY)
     * - tables from prefetches
     */
    private final Map<CayennePath, TableTreeNode> tableNodes;
    private final TableTree parentTree;
    private final TableTreeNode rootNode;

    private TableTreeNode activeNode;

    // alias bookkeeping, maintained only in the root tree (parentTree == null); nested trees
    // delegate generation upwards so that aliases stay unique across the whole statement
    private final Set<String> usedAliases;
    private final Map<String, Integer> nextAliasSuffix;

    TableTree(DbEntity root, TableTree parentTree) {
        this.parentTree = parentTree;
        this.usedAliases = new HashSet<>();
        this.nextAliasSuffix = new HashMap<>();
        this.tableNodes = new LinkedHashMap<>();
        this.rootNode = new TableTreeNode(root, nextTableAlias(root));
    }

    void addJoinTable(CayennePath path, DbRelationship relationship, JoinType joinType) {
        addJoinTable(path, relationship, joinType, null);
    }

    void addJoinTable(CayennePath path, DbRelationship relationship, JoinType joinType, Expression additionalQualifier) {
        TableTreeNode treeNode = tableNodes.get(path);
        if (treeNode != null) {
            return;
        }

        String alias = nextTableAlias(relationship.getTargetEntity());
        TableTreeNode node = new TableTreeNode(path, relationship, alias, joinType, additionalQualifier);
        tableNodes.put(path, node);
    }

    String aliasForPath(CayennePath attributePath) {
        if(attributePath.isEmpty()) {
            return rootNode.getTableAlias();
        }
        // should be resolved dynamically by the caller
        if(CURRENT_ALIAS_PATH.equals(attributePath)) {
            return CURRENT_ALIAS;
        }
        TableTreeNode node = tableNodes.get(attributePath);
        if (node == null) {
            throw new CayenneRuntimeException("No table for attribute '%s' found", attributePath);
        }
        return node.getTableAlias();
    }

    String nextTableAlias(DbEntity entity) {
        // delegate actual generation to parent if any, so that aliases stay unique across the
        // whole statement (including correlated subqueries)
        if(parentTree != null) {
            return parentTree.nextTableAlias(entity);
        }

        // the base is already guaranteed to be non-reserved (see DbEntity.getTableAliasBase), and a
        // numeric suffix can never produce a reserved word, so we only need to resolve collisions
        String base = entity == null ? "t" : entity.getTableAliasBase();
        int suffix = nextAliasSuffix.getOrDefault(base, 0);
        String candidate = suffix == 0 ? base : base + suffix;
        while (!usedAliases.add(candidate)) {
            candidate = base + (++suffix);
        }
        nextAliasSuffix.put(base, suffix + 1);
        return candidate;
    }

    TableTreeNode nonNullActiveNode() {
        if(activeNode == null) {
            throw new CayenneRuntimeException("No active TableTree node found");
        }
        return activeNode;
    }

    void setActiveNode(TableTreeNode activeNode) {
        this.activeNode = activeNode;
    }

    public int getNodeCount() {
        return tableNodes.size() + 1;
    }

    /**
     * Returns the number of distinct table aliases used across the whole statement, including nested
     * (subquery) trees, as tracked during translation. A value of 1 means the statement uses a single
     * table and aliases can be omitted from the generated SQL.
     */
    int totalAliasCount() {
        // all aliases are registered in the root tree, as nested trees delegate generation upwards
        return parentTree != null ? parentTree.totalAliasCount() : usedAliases.size();
    }

    boolean hasToManyJoin() {
        if(getNodeCount() <= 1) {
            return false;
        }

        AtomicBoolean atomicBoolean = new AtomicBoolean(false);
        visit(node -> {
            if(node.getRelationship() != null && node.getRelationship().isToMany()) {
                atomicBoolean.set(true);
            }
        });
        return atomicBoolean.get();
    }

    public void visit(TableNodeVisitor visitor) {
        visitor.visit(rootNode);

        // as we can spawn new nodes while processing existing,
        // we need multiple iterations until all rows are processed
        int initialSize = 0;
        int currentSize = tableNodes.size();
        while(initialSize != currentSize) {
            tableNodes.values().stream().skip(initialSize)
                    .collect(Collectors.toList()) // copy collection in case of concurrent modification in visitor
                    .forEach(visitor::visit);
            initialSize = currentSize;
            currentSize = tableNodes.size();
        }
    }

    @FunctionalInterface
    interface TableNodeVisitor {
        void visit(TableTreeNode node);
    }
}
