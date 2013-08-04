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

package org.apache.cayenne.reflect.generic;

import org.apache.cayenne.Fault;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.ArcProperty;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.PropertyException;
import org.apache.cayenne.reflect.PropertyVisitor;
import org.apache.cayenne.reflect.ToOneProperty;

/**
 * An ArcProperty for accessing to-one relationships.
 * 
 * @since 3.0
 */
class DataObjectToOneProperty extends DataObjectBaseProperty implements ToOneProperty {

    protected ObjRelationship relationship;
    protected String reverseName;
    protected String reverseDbPath;
    protected ClassDescriptor targetDescriptor;
    protected Fault fault;

    DataObjectToOneProperty(ObjRelationship relationship,
            ClassDescriptor targetDescriptor, Fault fault) {
        this.relationship = relationship;
        this.targetDescriptor = targetDescriptor;
        this.reverseName = relationship.getReverseRelationshipName();
        this.fault = fault;
    }

    public ArcProperty getComplimentaryReverseArc() {
        return reverseName != null ? (ArcProperty) targetDescriptor
                .getProperty(reverseName) : null;
    }
    
    @Override
    public String getComplimentaryReverseDbRelationshipPath() {
        if (reverseDbPath == null) {
            reverseDbPath = relationship.getReverseDbRelationshipPath();
        }

        return reverseDbPath;
    }

    public ClassDescriptor getTargetDescriptor() {
        return targetDescriptor;
    }

    @Override
    public String getName() {
        return relationship.getName();
    }

    public ObjRelationship getRelationship() {
        return relationship;
    }

    @Override
    public void injectValueHolder(Object object) throws PropertyException {
    }

    public void setTarget(Object source, Object target, boolean setReverse) {
        try {
            toDataObject(source).setToOneTarget(
                    getName(),
                    toDataObject(target),
                    setReverse);
        }
        catch (Throwable th) {
            throw new PropertyException("Error setting to-one DataObject property: "
                    + getName(), this, source, th);
        }
    }

    @Override
    public boolean visit(PropertyVisitor visitor) {
        return visitor.visitToOne(this);
    }

    public boolean isFault(Object object) {
        return readPropertyDirectly(object) instanceof Fault;
    }

    public void invalidate(Object object) {
        writePropertyDirectly(object, null, fault);
    }
}
