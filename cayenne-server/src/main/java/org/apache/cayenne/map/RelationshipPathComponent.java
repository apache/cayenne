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
package org.apache.cayenne.map;

/**
 * @since 3.0
 */
class RelationshipPathComponent<A extends Attribute<?, A, R>, R extends Relationship<?, A, R>>
        implements PathComponent<A, R> {

    private final R relationship;
    private final JoinType joinType;
    private final boolean last;

    RelationshipPathComponent(R relationship, JoinType joinType, boolean last) {
        this.relationship = relationship;
        this.joinType = joinType;
        this.last = last;
    }

    public A getAttribute() {
        return null;
    }

    public R getRelationship() {
        return relationship;
    }

    public JoinType getJoinType() {
        return joinType;
    }

    public String getName() {
        return relationship.getName();
    }

    public boolean isLast() {
        return last;
    }

    public boolean isAlias() {
        return false;
    }

    public Iterable<PathComponent<A, R>> getAliasedPath() {
        return null;
    }
}
