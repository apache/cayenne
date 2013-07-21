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

package org.apache.cayenne;

import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyDescriptor;
import org.apache.cayenne.reflect.ToManyMapProperty;

/**
 * A convenience base superclass for concrete Persistent objects. Provides implementation
 * of properties declared in Persistent interface.
 * <h4>POJO Note</h4>
 * <p>
 * If having PersistentObject as a superclass presents a problem in an application, source
 * code of this class can be copied verbatim to a custom class generation template.
 * Desired superclass can be set in CayenneModeler.
 * </p>
 * 
 * @since 1.2
 */
public abstract class PersistentObject implements Persistent {

    protected ObjectId objectId;
    protected int persistenceState;
    protected transient ObjectContext objectContext;

    /**
     * Creates a new transient object.
     */
    public PersistentObject() {
        this.persistenceState = PersistenceState.TRANSIENT;
    }

    public int getPersistenceState() {
        return persistenceState;
    }

    public void setPersistenceState(int persistenceState) {
        this.persistenceState = persistenceState;

        if (persistenceState == PersistenceState.TRANSIENT) {
            setObjectContext(null);
        }
    }

    public ObjectContext getObjectContext() {
        return objectContext;
    }

    public void setObjectContext(ObjectContext objectContext) {
        this.objectContext = objectContext;
    }

    public ObjectId getObjectId() {
        return objectId;
    }

    public void setObjectId(ObjectId objectId) {
        this.objectId = objectId;
    }
    
    /**
     * Returns a map key for a given to-many map relationship and a target object.
     * 
     * @since 3.0
     */
    protected Object getMapKey(String relationshipName, Object value) {

        EntityResolver resolver = objectContext.getEntityResolver();
        ClassDescriptor descriptor = resolver
                .getClassDescriptor(objectId.getEntityName());

        if (descriptor == null) {
            throw new IllegalStateException("DataObject's entity is unmapped, objectId: "
                    + objectId);
        }

        PropertyDescriptor property = descriptor.getProperty(relationshipName);
        if (property instanceof ToManyMapProperty) {
            return ((ToManyMapProperty) property).getMapKey(value);
        }

        throw new IllegalArgumentException("Relationship '"
                + relationshipName
                + "' is not a to-many Map");
    }

    @Override
    public String toString() {
        String state = PersistenceState.persistenceStateName(getPersistenceState());

        StringBuilder buffer = new StringBuilder();
        buffer
                .append("<")
                .append(getClass().getName())
                .append("@")
                .append(System.identityHashCode(this))
                .append(", id=")
                .append(objectId)
                .append(", state=")
                .append(state)
                .append(", context=")
                .append(objectContext)
                .append(">");

        return buffer.toString();
    }
}
