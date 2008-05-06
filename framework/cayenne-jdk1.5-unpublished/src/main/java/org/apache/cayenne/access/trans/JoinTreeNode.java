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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.JoinType;
import org.apache.cayenne.util.Util;

/**
 * @since 3.0
 * @author Andrus Adamchik
 */
final class JoinTreeNode {

    private String targetTableAlias;
    private String sourceTableAlias;
    private DbRelationship relationship;
    private String alias;
    private JoinType joinType;
    private Collection<JoinTreeNode> children;
    private SelectTranslator tableAliasSource;

    JoinTreeNode(SelectTranslator tableAliasSource) {
        this.tableAliasSource = tableAliasSource;
    }

    JoinTreeNode(SelectTranslator tableAliasSource, DbRelationship relationship,
            JoinType joinType, String alias) {
        this(tableAliasSource);
        this.relationship = relationship;
        this.alias = alias;
        this.joinType = joinType;
    }

    Collection<JoinTreeNode> getChildren() {
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
                tableAliasSource,
                relationship,
                joinType,
                alias);
        child.setSourceTableAlias(this.targetTableAlias);
        child.setTargetTableAlias(tableAliasSource
                .newAliasForTable((DbEntity) relationship.getTargetEntity()));
        children.add(child);
        return child;
    }

    private boolean equals(DbRelationship relationship, JoinType joinType, String alias) {
        return this.relationship == relationship
                && this.joinType == joinType
                && Util.nullSafeEquals(this.alias, alias);
    }

    String getTargetTableAlias() {
        return targetTableAlias;
    }

    void setTargetTableAlias(String targetTableAlias) {
        this.targetTableAlias = targetTableAlias;
    }

    String getSourceTableAlias() {
        return sourceTableAlias;
    }

    void setSourceTableAlias(String sourceTableAlias) {
        this.sourceTableAlias = sourceTableAlias;
    }

    DbRelationship getRelationship() {
        return relationship;
    }

    String getAlias() {
        return alias;
    }

    JoinType getJoinType() {
        return joinType;
    }
}