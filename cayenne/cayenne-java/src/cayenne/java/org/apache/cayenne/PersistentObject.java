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
 * @author Andrus Adamchik
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
            this.objectContext = null;
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

    public String toString() {
        String state = PersistenceState.persistenceStateName(getPersistenceState());

        StringBuffer buffer = new StringBuffer();
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
