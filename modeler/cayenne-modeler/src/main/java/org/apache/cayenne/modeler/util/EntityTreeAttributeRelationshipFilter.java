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

package org.apache.cayenne.modeler.util;

import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.Relationship;

public class EntityTreeAttributeRelationshipFilter implements EntityTreeFilter {

    public boolean attributeMatch(Object node, Attribute attr) {
        if (!(node instanceof Attribute)) {
            return true;
        }
        return false;
    }

    public boolean relationshipMatch(Object node, Relationship rel) {
        if (!(node instanceof Relationship)) {
            return true;
        }

        /**
         * We do not allow A->B->A chains, where relationships
         * are to-one
         */
        DbRelationship prev = (DbRelationship) node;
        return !(!rel.isToMany() && prev.getReverseRelationship() == rel);
    }
}
