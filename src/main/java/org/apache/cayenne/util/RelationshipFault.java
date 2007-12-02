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

package org.apache.cayenne.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.query.RelationshipQuery;
import org.apache.cayenne.reflect.Property;

/**
 * An abstract superclass of lazily faulted to-one and to-many relationships.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public abstract class RelationshipFault {

    protected Persistent relationshipOwner;
    protected String relationshipName;

    protected RelationshipFault() {

    }

    public RelationshipFault(Persistent relationshipOwner, String relationshipName) {
        if (relationshipOwner == null) {
            throw new NullPointerException("'relationshipOwner' can't be null.");
        }

        if (relationshipName == null) {
            throw new NullPointerException("'relationshipName' can't be null.");
        }

        this.relationshipOwner = relationshipOwner;
        this.relationshipName = relationshipName;
    }

    public String getRelationshipName() {
        return relationshipName;
    }

    public Persistent getRelationshipOwner() {
        return relationshipOwner;
    }

    protected boolean isTransientParent() {
        int state = relationshipOwner.getPersistenceState();
        return state == PersistenceState.NEW || state == PersistenceState.TRANSIENT;
    }

    protected boolean isUncommittedParent() {
        int state = relationshipOwner.getPersistenceState();
        return state == PersistenceState.MODIFIED || state == PersistenceState.DELETED;
    }

    /**
     * Executes a query that returns related objects. Subclasses would invoke this method
     * whenever they need to resolve a fault.
     */
    protected List resolveFromDB() {
        // non-persistent objects shouldn't trigger a fetch
        if (isTransientParent()) {
            return new ArrayList();
        }

        List resolved = relationshipOwner.getObjectContext().performQuery(
                new RelationshipQuery(
                        relationshipOwner.getObjectId(),
                        relationshipName,
                        false));

        if (resolved.isEmpty()) {
            return resolved;
        }

        // see if reverse relationship is to-one and we can connect source to results....

        EntityResolver resolver = relationshipOwner
                .getObjectContext()
                .getEntityResolver();
        ObjEntity sourceEntity = resolver.getObjEntity(relationshipOwner
                .getObjectId()
                .getEntityName());

        ObjRelationship relationship = (ObjRelationship) sourceEntity
                .getRelationship(relationshipName);
        ObjRelationship reverse = relationship.getReverseRelationship();

        if (reverse != null && !reverse.isToMany()) {
            Property property = resolver.getClassDescriptor(
                    reverse.getSourceEntity().getName()).getProperty(reverse.getName());

            Iterator it = resolved.iterator();
            while (it.hasNext()) {
                property.writePropertyDirectly(it.next(), null, relationshipOwner);
            }
        }

        return resolved;
    }
}
