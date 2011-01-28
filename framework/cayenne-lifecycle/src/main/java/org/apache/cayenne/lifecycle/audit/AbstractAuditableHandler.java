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
package org.apache.cayenne.lifecycle.audit;

import org.apache.cayenne.DataObject;
import org.apache.cayenne.annotation.PostPersist;
import org.apache.cayenne.annotation.PostRemove;
import org.apache.cayenne.annotation.PostUpdate;

/**
 * A superclass of application specific handlers of the {@link Auditable} mixin that
 * provides basic needed callbacks.
 */
public abstract class AbstractAuditableHandler {

    /**
     * A worker method that creates audit records, as appropriate in a given application.
     * Subclasses may insert audit records, log a message, etc.
     */
    protected abstract void audit(
            Object auditRoot,
            Object auditSource,
            AuditableOperation operation);

    @PostPersist(entityAnnotations = Auditable.class)
    void insertAudit(Object object) {
        audit(object, object, AuditableOperation.INSERT);
    }

    @PostRemove(entityAnnotations = Auditable.class)
    void deleteAudit(Object object) {
        audit(object, object, AuditableOperation.DELETE);
    }

    @PostUpdate(entityAnnotations = Auditable.class)
    void updateAudit(Object object) {
        audit(object, object, AuditableOperation.UPDATE);
    }

    // only catching child updates... child insert/delete presumably causes an event on
    // the owner object

    @PostUpdate(entityAnnotations = AuditableChild.class)
    void updateAuditChild(Object object) {

        Object parent = getParent(object);

        if (parent != null) {
            audit(parent, object, AuditableOperation.UPDATE);
        }
        else {
            // at least og this fact... shouldn't normally happen, but I can imagine
            // certain combinations of object graphs, disconnected relationships, delete
            // rules, etc. may cause this
        }
    }

    protected Object getParent(Object object) {

        if (object == null) {
            throw new NullPointerException("Null object");
        }

        if (!(object instanceof DataObject)) {
            throw new IllegalArgumentException("Object is not a DataObject: "
                    + object.getClass().getName());
        }

        DataObject dataObject = (DataObject) object;

        AuditableChild annotation = dataObject.getClass().getAnnotation(
                AuditableChild.class);
        if (annotation == null) {
            throw new IllegalArgumentException("No 'AuditableChild' annotation found");
        }

        // support for nested paths
        return dataObject.readNestedProperty(annotation.value());
    }
}
