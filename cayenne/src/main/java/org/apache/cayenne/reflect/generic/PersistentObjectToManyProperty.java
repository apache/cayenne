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

package org.apache.cayenne.reflect.generic;

import org.apache.cayenne.Fault;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyException;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToManyProperty;

/**
 * @since 3.0
 */
class PersistentObjectToManyProperty extends PersistentObjectBaseProperty implements ToManyProperty {

    protected ObjRelationship relationship;
    protected String reverseName;
    protected String reverseDbPath;
    protected ClassDescriptor targetDescriptor;
    protected Fault fault;

    PersistentObjectToManyProperty(ObjRelationship relationship, ClassDescriptor targetDescriptor, Fault fault) {
        this.relationship = relationship;
        this.targetDescriptor = targetDescriptor;
        this.reverseName = relationship.getReverseRelationshipName();
        this.fault = fault;
    }

    public ArcProperty getComplimentaryReverseArc() {
        return reverseName != null ? (ArcProperty) targetDescriptor.getProperty(reverseName) : null;
    }

    public ClassDescriptor getTargetDescriptor() {
        return targetDescriptor;
    }

    @Override
    public String getComplimentaryReverseDbRelationshipPath() {
        if (reverseDbPath == null) {
            reverseDbPath = relationship.getReverseDbRelationshipPath().value();
        }

        return reverseDbPath;
    }

    @Override
    public String getName() {
        return relationship.getName();
    }

    public ObjRelationship getRelationship() {
        return relationship;
    }

    public void addTarget(Object source, Object target, boolean setReverse) throws PropertyException {
        try {
            toPersistent(source).addToManyTarget(getName(), toPersistent(target), setReverse);
        } catch (Throwable th) {
            throw new PropertyException("Error setting to-many Persistent property: " + getName(), this, source, th);
        }
    }

    public void removeTarget(Object source, Object target, boolean setReverse) throws PropertyException {
        try {
            toPersistent(source).removeToManyTarget(getName(), toPersistent(target), setReverse);
        } catch (Throwable th) {
            throw new PropertyException("Error unsetting to-many Persistent property: " + getName(), this, source, th);
        }
    }

    @Override
    public void injectValueHolder(Object object) throws PropertyException {
        if (readPropertyDirectly(object) == null) {
            writePropertyDirectly(object, null, fault.resolveFault((Persistent) object, getName()));
        }
    }

    public boolean isFault(Object source) {
        return readPropertyDirectly(source) instanceof Fault;
    }

    @Override
    public boolean visit(PropertyVisitor visitor) {
        return visitor.visitToMany(this);
    }

    public void invalidate(Object object) {
        Object value = readPropertyDirectly(object);
        if (value instanceof Fault) {
            // nothing to do
        } else if (value instanceof ValueHolder) {
            ((ValueHolder) value).invalidate();
        } else {
            writePropertyDirectly(object, null, fault);
        }
    }

    public void addTargetDirectly(Object source, Object target) throws PropertyException {
        addTarget(source, target, false);
    }

    public void removeTargetDirectly(Object source, Object target) throws PropertyException {
        removeTarget(source, target, false);
    }
}
