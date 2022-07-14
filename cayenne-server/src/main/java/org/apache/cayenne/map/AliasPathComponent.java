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

import java.util.Collection;

/**
 * Represents an alias for the relationship path.
 * 
 * @since 3.0
 */
class AliasPathComponent<A extends Attribute<?, A, R>, R extends Relationship<?, A, R>> implements PathComponent<A, R> {

    private final String alias;
    private final Collection<PathComponent<A, R>> path;
    private final boolean last;

    AliasPathComponent(String alias, Collection<PathComponent<A, R>> path, boolean last) {
        this.alias = alias;
        this.path = path;
        this.last = last;
    }

    public Iterable<PathComponent<A, R>> getAliasedPath() {
        return path;
    }

    public A getAttribute() {
        return null;
    }

    public JoinType getJoinType() {
        return JoinType.UNDEFINED;
    }

    public String getName() {
        return alias;
    }

    public R getRelationship() {
        return null;
    }

    public boolean isAlias() {
        return true;
    }

    public boolean isLast() {
        return last;
    }
}
