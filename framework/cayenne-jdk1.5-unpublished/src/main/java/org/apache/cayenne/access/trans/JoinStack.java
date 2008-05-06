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
package org.apache.cayenne.access.trans;

import java.io.IOException;
import java.util.List;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.JoinType;

/**
 * Encapsulates join reuse/split logic used in SelectQuery processing. All expression
 * path's that exist in the query (in the qualifier, etc.) are processed to produce a
 * combined join tree.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
class JoinStack {

    private JoinTreeNode rootNode;
    private JoinTreeNode topNode;

    private int aliasCounter;

    JoinStack() {
        this.rootNode = new JoinTreeNode(this);
        this.rootNode.setTargetTableAlias(newAlias());
        resetStack();
    }
    
    String getCurrentAlias() {
        return topNode.getTargetTableAlias();
    }

    /**
     * Returns the number of configured joins.
     */
    int size() {
        // do not count root as a join
        return rootNode.size() - 1;
    }

    void appendRoot(Appendable out, DbEntity rootEntity) throws IOException {
        out.append(rootEntity.getFullyQualifiedName());
        out.append(' ').append(rootNode.getTargetTableAlias());
    }

    /**
     * Appends all configured joins to the provided output object.
     */
    void appendJoins(Appendable out) throws IOException {

        // skip root, recursively append its children
        for (JoinTreeNode child : rootNode.getChildren()) {
            appendJoinSubtree(out, child);
        }
    }

    private void appendJoinSubtree(Appendable out, JoinTreeNode node) throws IOException {

        DbRelationship relationship = node.getRelationship();

        DbEntity targetEntity = (DbEntity) relationship.getTargetEntity();
        String srcAlias = node.getSourceTableAlias();
        String targetAlias = node.getTargetTableAlias();

        switch (node.getJoinType()) {
            case INNER:
                out.append(" JOIN");
                break;
            case LEFT_OUTER:
                out.append(" LEFT JOIN");
                break;
            default:
                throw new IllegalArgumentException("Unsupported join type: "
                        + node.getJoinType());
        }

        out.append(' ').append(targetEntity.getFullyQualifiedName()).append(' ').append(
                targetAlias).append(" ON (");

        List<DbJoin> joins = relationship.getJoins();
        int len = joins.size();
        for (int i = 0; i < len; i++) {
            DbJoin join = joins.get(i);

            if (i > 0) {
                out.append(" AND ");
            }

            out
                    .append(srcAlias)
                    .append('.')
                    .append(join.getSourceName())
                    .append(" = ")
                    .append(targetAlias)
                    .append('.')
                    .append(join.getTargetName());
        }

        out.append(')');

        for (JoinTreeNode child : node.getChildren()) {
            appendJoinSubtree(out, child);
        }
    }

    /**
     * Pops the stack all the way to the root node.
     */
    void resetStack() {
        topNode = rootNode;
    }

    /**
     * Finds or creates a JoinTreeNode for the given arguments and sets it as the next
     * current join.
     */
    void pushJoin(DbRelationship relationship, JoinType joinType, String alias) {
        topNode = topNode.findOrCreateChild(relationship, joinType, alias);
    }

    protected String newAlias() {
        return "t" + aliasCounter++;
    }
}
