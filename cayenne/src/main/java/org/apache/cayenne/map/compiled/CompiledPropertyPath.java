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
package org.apache.cayenne.map.compiled;

import java.util.List;

/**
 * A path resolved against an entity: the relationships it traverses, in order, and the attribute or embedded
 * attribute it ends in, if it ends in one rather than in a relationship.
 *
 * @param relationships     the relationships traversed, possibly none
 * @param attribute         the attribute the path ends in, or null
 * @param embeddedAttribute the embedded attribute the path ends in, or null
 * @since 6.0
 */
public record CompiledPropertyPath(
        List<CompiledRelationship> relationships,
        CompiledAttribute attribute,
        CompiledEmbeddedAttribute embeddedAttribute) {

    /**
     * Returns true if the path ends in a relationship, not in an attribute.
     */
    public boolean endsInRelationship() {
        return attribute == null && embeddedAttribute == null && !relationships.isEmpty();
    }

    /**
     * Returns the component the path ends in: the attribute, the embedded attribute or the last relationship.
     */
    public Object end() {
        if (attribute != null) {
            return attribute;
        }
        if (embeddedAttribute != null) {
            return embeddedAttribute;
        }
        return relationships.isEmpty() ? null : relationships.getLast();
    }
}
