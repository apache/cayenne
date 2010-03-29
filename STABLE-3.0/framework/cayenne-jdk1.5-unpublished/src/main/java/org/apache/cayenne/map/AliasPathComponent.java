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
package org.apache.cayenne.map;

import java.util.Collection;

/**
 * Represents an alias for the relationship path.
 * 
 * @since 3.0
 */
class AliasPathComponent<T extends Attribute, U extends Relationship> implements
        PathComponent<T, U> {

    private String alias;
    private Collection<PathComponent<T, U>> path;
    private boolean last;

    AliasPathComponent(String alias, Collection<PathComponent<T, U>> path, boolean last) {
        this.alias = alias;
        this.path = path;
        this.last = last;
    }

    public Iterable<PathComponent<T, U>> getAliasedPath() {
        return path;
    }

    public T getAttribute() {
        return null;
    }

    public JoinType getJoinType() {
        return JoinType.UNDEFINED;
    }

    public String getName() {
        return alias;
    }

    public U getRelationship() {
        return null;
    }

    public boolean isAlias() {
        return true;
    }

    public boolean isLast() {
        return last;
    }
}
