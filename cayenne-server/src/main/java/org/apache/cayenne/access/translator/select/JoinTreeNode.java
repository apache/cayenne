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
package org.apache.cayenne.access.translator.select;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.JoinType;
import org.apache.cayenne.util.Util;

/**
 * @since 3.0
 */
public final class JoinTreeNode {

    private String targetTableAlias;
    private String sourceTableAlias;
    private DbRelationship relationship;
    private String alias;
    private JoinType joinType;
    private Collection<JoinTreeNode> children;
    private JoinStack joinProcessor;
    
    /**
     * Parent join
     */
    private JoinTreeNode parent;

    JoinTreeNode(JoinStack joinProcessor) {
        this.joinProcessor = joinProcessor;
    }

    JoinTreeNode(JoinStack joinProcessor, DbRelationship relationship,
            JoinType joinType, String alias, JoinTreeNode parent) {
        this(joinProcessor);
        this.relationship = relationship;
        this.alias = alias;
        this.joinType = joinType;
        this.parent = parent;
    }

    int size() {
        int i = 1;

        if (children != null) {
            for (JoinTreeNode child : children) {
                i += child.size();
            }
        }

        return i;
    }

    public Collection<JoinTreeNode> getChildren() {
        return children != null ? children : Collections.<JoinTreeNode> emptyList();
    }

    JoinTreeNode findOrCreateChild(
            DbRelationship relationship,
            JoinType joinType,
            String alias) {

        if (children == null) {
            children = new ArrayList<JoinTreeNode>(4);
        }
        else {
            for (JoinTreeNode child : children) {

                if (child.equals(relationship, joinType, alias)) {
                    return child;
                }
            }
        }

        JoinTreeNode child = new JoinTreeNode(
                joinProcessor,
                relationship,
                joinType,
                alias,
                this);
        child.setSourceTableAlias(this.targetTableAlias);
        child.setTargetTableAlias(joinProcessor.newAlias());
        children.add(child);
        return child;
    }

    private boolean equals(DbRelationship relationship, JoinType joinType, String alias) {
        return this.relationship == relationship
                && this.joinType == joinType
                && Util.nullSafeEquals(this.alias, alias);
    }

    public String getTargetTableAlias() {
        return targetTableAlias;
    }

    void setTargetTableAlias(String targetTableAlias) {
        this.targetTableAlias = targetTableAlias;
    }

    public String getSourceTableAlias() {
        return sourceTableAlias;
    }

    void setSourceTableAlias(String sourceTableAlias) {
        this.sourceTableAlias = sourceTableAlias;
    }

    public DbRelationship getRelationship() {
        return relationship;
    }

    String getAlias() {
        return alias;
    }

    public JoinType getJoinType() {
        return joinType;
    }
    
    /**
     * @return parent join
     */
    public JoinTreeNode getParent() {
        return parent;
    }
}
