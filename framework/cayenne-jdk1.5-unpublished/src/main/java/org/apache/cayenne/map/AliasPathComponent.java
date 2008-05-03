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

import java.util.Collections;
import java.util.Iterator;

/**
 * Represents an alias for the relationship path.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
class AliasPathComponent<T extends Attribute, U extends Relationship> implements
        PathComponent<T, U> {

    private Entity root;
    private String alias;
    private String path;
    private boolean last;

    AliasPathComponent(Entity root, String alias, String path, boolean last) {
        this.root = root;
        this.alias = alias;
        this.path = path;
        this.last = last;
    }

    public Iterable<PathComponent<T, U>> getAliasedPath() {
        return new Iterable<PathComponent<T, U>>() {

            // suppress warning until we parameterize Entity as Entity<T extends
            // Attribute, U extends Relationship>
            @SuppressWarnings("unchecked")
            public Iterator iterator() {
                return new PathComponentIterator(root, path, Collections
                        .<String, String> emptyMap());
            }
        };
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
