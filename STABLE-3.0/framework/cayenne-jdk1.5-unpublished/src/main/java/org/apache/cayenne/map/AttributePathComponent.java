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

/**
 * @since 3.0
 */
class AttributePathComponent<T extends Attribute, U extends Relationship> implements
        PathComponent<T, U> {

    private T attribute;

    AttributePathComponent(T attribute) {
        this.attribute = attribute;
    }

    public T getAttribute() {
        return attribute;
    }

    public U getRelationship() {
        return null;
    }

    public JoinType getJoinType() {
        return JoinType.UNDEFINED;
    }

    public String getName() {
        return attribute.getName();
    }
    
    public boolean isLast() {
        return true;
    }
    
    public boolean isAlias() {
        return false;
    }
    
    public Iterable<PathComponent<T, U>> getAliasedPath() {
        return null;
    }
}
