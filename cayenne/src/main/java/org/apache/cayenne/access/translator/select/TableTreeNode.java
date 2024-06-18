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

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.JoinType;

/**
 * @since 4.2
 */
class TableTreeNode {

    // path that spawned this node
    private final CayennePath attributePath;
    private final DbEntity entity;
    private final String tableAlias;
    private final JoinType joinType;

    // relationship that connects this node with parent (or null if this is root)
    private final DbRelationship relationship;

    private final Expression additionalQualifier;

    TableTreeNode(DbEntity entity, String tableAlias) {
        this.attributePath = CayennePath.EMPTY_PATH;
        this.entity = entity;
        this.tableAlias = tableAlias;
        this.relationship = null;
        this.joinType = null;
        this.additionalQualifier = null;
    }

    TableTreeNode(CayennePath path, DbRelationship relationship, String tableAlias, JoinType joinType, Expression additionalQualifier) {
        this.attributePath = path;
        this.entity = relationship.getTargetEntity();
        this.tableAlias = tableAlias;
        this.relationship = relationship;
        this.joinType = joinType;
        this.additionalQualifier = additionalQualifier;
    }

    public CayennePath getAttributePath() {
        return attributePath;
    }

    public DbEntity getEntity() {
        return entity;
    }

    public String getTableAlias() {
        return tableAlias;
    }

    public JoinType getJoinType() {
        return joinType;
    }

    public DbRelationship getRelationship() {
        return relationship;
    }

    public Expression getAdditionalQualifier() {
        return additionalQualifier;
    }
}
