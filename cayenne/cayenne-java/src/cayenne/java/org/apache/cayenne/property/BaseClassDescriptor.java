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

package org.apache.cayenne.property;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections.IteratorUtils;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.PersistenceState;

/**
 * A superclass of Cayenne ClassDescriptors. Defines all main bean descriptor parameters
 * and operations. Subclasses would provide methods to initialize the descriptor.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public abstract class BaseClassDescriptor implements ClassDescriptor {

    static final Integer TRANSIENT_STATE = new Integer(PersistenceState.TRANSIENT);
    static final Integer HOLLOW_STATE = new Integer(PersistenceState.HOLLOW);
    static final Integer COMMITTED_STATE = new Integer(PersistenceState.COMMITTED);

    protected ClassDescriptor superclassDescriptor;

    // compiled properties ...
    protected Class objectClass;
    protected Map declaredProperties;
    protected Map valueHolderProperties;
    protected Map subclassDescriptors;
    protected PropertyAccessor persistenceStateProperty;

    /**
     * Creates an uncompiled BaseClassDescriptor. Subclasses may add a call to "compile"
     * in the constructor after finishing their initialization.
     */
    public BaseClassDescriptor(ClassDescriptor superclassDescriptor) {
        this.superclassDescriptor = superclassDescriptor;
    }

    public boolean isFault(Object object) {
        if (superclassDescriptor != null) {
            return superclassDescriptor.isFault(object);
        }

        if (object == null) {
            return false;
        }

        return HOLLOW_STATE.equals(persistenceStateProperty.readPropertyDirectly(object));
    }

    /**
     * Returns true if a descriptor is initialized and ready for operation.
     */
    public boolean isValid() {
        return objectClass != null && declaredProperties != null;
    }

    public Class getObjectClass() {
        return objectClass;
    }

    public ClassDescriptor getSubclassDescriptor(Class objectClass) {
        if (objectClass == null) {
            throw new IllegalArgumentException("Null objectClass");
        }

        if (subclassDescriptors == null) {
            return this;
        }

        ClassDescriptor subclassDescriptor = (ClassDescriptor) subclassDescriptors
                .get(objectClass.getName());

        // ascend via the class hierarchy (only doing it if there are multiple choices)
        if (subclassDescriptor == null) {
            Class currentClass = objectClass;
            while (subclassDescriptor == null
                    && (currentClass = currentClass.getSuperclass()) != null) {
                subclassDescriptor = (ClassDescriptor) subclassDescriptors
                        .get(currentClass.getName());
            }
        }

        return subclassDescriptor != null ? subclassDescriptor : this;
    }

    public Iterator getProperties() {
        Iterator declaredIt = IteratorUtils.unmodifiableIterator(declaredProperties
                .values()
                .iterator());

        if (getSuperclassDescriptor() == null) {
            return declaredIt;
        }
        else {
            return IteratorUtils.chainedIterator(
                    superclassDescriptor.getProperties(),
                    declaredIt);
        }
    }

    /**
     * Recursively looks up property descriptor in this class descriptor and all
     * superclass descriptors.
     */
    public Property getProperty(String propertyName) {
        Property property = getDeclaredProperty(propertyName);

        if (property == null && superclassDescriptor != null) {
            property = superclassDescriptor.getProperty(propertyName);
        }

        return property;
    }

    public Property getDeclaredProperty(String propertyName) {
        return (Property) declaredProperties.get(propertyName);
    }

    /**
     * Returns a descriptor of the mapped superclass or null if the descriptor's entity
     * sits at the top of inheritance hierarchy.
     */
    public ClassDescriptor getSuperclassDescriptor() {
        return superclassDescriptor;
    }

    /**
     * Creates a new instance of a class described by this object.
     */
    public Object createObject() {
        if (objectClass == null) {
            throw new NullPointerException(
                    "Null objectClass. Descriptor wasn't initialized properly.");
        }

        try {
            return objectClass.newInstance();
        }
        catch (Throwable e) {
            throw new CayenneRuntimeException("Error creating object of class '"
                    + objectClass.getName()
                    + "'", e);
        }
    }

    /**
     * Invokes 'prepareForAccess' of a super descriptor and then invokes
     * 'prepareForAccess' of each declared property.
     */
    public void injectValueHolders(Object object) throws PropertyAccessException {

        // do super first
        if (getSuperclassDescriptor() != null) {
            getSuperclassDescriptor().injectValueHolders(object);
        }

        if (valueHolderProperties != null) {
            Iterator it = valueHolderProperties.values().iterator();
            while (it.hasNext()) {
                Property property = (Property) it.next();
                property.injectValueHolder(object);
            }
        }
    }

    /**
     * Copies object properties from one object to another. Invokes 'shallowCopy' of a
     * super descriptor and then invokes 'shallowCopy' of each declared property.
     */
    public void shallowMerge(Object from, Object to) throws PropertyAccessException {

        // do super first
        if (getSuperclassDescriptor() != null) {
            getSuperclassDescriptor().shallowMerge(from, to);
        }

        Iterator it = declaredProperties.values().iterator();
        while (it.hasNext()) {
            Property property = (Property) it.next();
            property.shallowMerge(from, to);
        }
    }

    public boolean visitProperties(PropertyVisitor visitor) {
        if (superclassDescriptor != null
                && !superclassDescriptor.visitProperties(visitor)) {
            return false;
        }

        Iterator it = declaredProperties.values().iterator();
        while (it.hasNext()) {
            Property next = (Property) it.next();
            if (!next.visit(visitor)) {
                return false;
            }
        }

        return true;
    }
}
