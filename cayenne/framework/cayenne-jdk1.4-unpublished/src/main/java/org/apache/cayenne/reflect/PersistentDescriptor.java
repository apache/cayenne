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
package org.apache.cayenne.reflect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.commons.collections.IteratorUtils;

/**
 * A default ClassDescriptor implementation for persistent objects.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public class PersistentDescriptor implements ClassDescriptor {

    static final Integer TRANSIENT_STATE = new Integer(PersistenceState.TRANSIENT);
    static final Integer HOLLOW_STATE = new Integer(PersistenceState.HOLLOW);
    static final Integer COMMITTED_STATE = new Integer(PersistenceState.COMMITTED);

    protected ClassDescriptor superclassDescriptor;

    // compiled properties ...
    protected Class objectClass;
    protected Map declaredProperties;
    protected Map subclassDescriptors;
    protected Accessor persistenceStateAccessor;

    protected ObjEntity entity;

    protected Collection declaredIdProperties;

    /**
     * Creates a PersistentDescriptor.
     */
    public PersistentDescriptor() {
        this.declaredProperties = new HashMap();
        this.subclassDescriptors = new HashMap();
    }

    /**
     * Registers a property. This method is useful to customize default ClassDescriptor
     * generated from ObjEntity by adding new properties or overriding the standard ones.
     */
    public void addDeclaredProperty(Property property) {
        declaredProperties.put(property.getName(), property);

        if (property instanceof AttributeProperty) {
            ObjAttribute attribute = ((AttributeProperty) property).getAttribute();
            if (attribute.getDbAttributeName() != null
                    && attribute.getDbAttribute().isPrimaryKey()) {

                if (declaredIdProperties == null) {
                    declaredIdProperties = new ArrayList(2);
                }

                declaredIdProperties.add(property);
            }
        }
    }

    /**
     * Removes declared property. This method can be used to customize default
     * ClassDescriptor generated from ObjEntity.
     */
    public void removeDeclaredProperty(String propertyName) {
        Object removed = declaredProperties.remove(propertyName);

        if (declaredIdProperties != null && removed != null) {
            declaredIdProperties.remove(removed);
        }
    }

    public void addSubclassDescriptor(ClassDescriptor subclassDescriptor) {
        subclassDescriptors.put(
                subclassDescriptor.getEntity().getClassName(),
                subclassDescriptor);
    }

    public ObjEntity getEntity() {
        return entity;
    }

    public boolean isFault(Object object) {
        if (superclassDescriptor != null) {
            return superclassDescriptor.isFault(object);
        }

        if (object == null) {
            return false;
        }

        return HOLLOW_STATE.equals(persistenceStateAccessor.getValue(object));
    }

    public Class getObjectClass() {
        return objectClass;
    }

    void setObjectClass(Class objectClass) {
        this.objectClass = objectClass;
    }

    public ClassDescriptor getSubclassDescriptor(Class objectClass) {
        if (objectClass == null) {
            throw new IllegalArgumentException("Null objectClass");
        }

        if (subclassDescriptors.isEmpty()) {
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

    /**
     * @deprecated since 3.0. Use {@link #visitProperties(PropertyVisitor)} method
     *             instead.
     */
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

    public Iterator getIdProperties() {

        Iterator it = null;

        if (getSuperclassDescriptor() != null) {
            it = getSuperclassDescriptor().getIdProperties();
        }

        if (declaredIdProperties != null) {
            it = (it != null) ? IteratorUtils.chainedIterator(it, declaredIdProperties
                    .iterator()) : declaredIdProperties.iterator();
        }

        return it != null ? it : IteratorUtils.EMPTY_ITERATOR;
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
    public void injectValueHolders(Object object) throws PropertyException {

        // do super first
        if (getSuperclassDescriptor() != null) {
            getSuperclassDescriptor().injectValueHolders(object);
        }

        Iterator it = declaredProperties.values().iterator();
        while (it.hasNext()) {
            Property property = (Property) it.next();
            property.injectValueHolder(object);
        }
    }

    /**
     * Copies object properties from one object to another. Invokes 'shallowCopy' of a
     * super descriptor and then invokes 'shallowCopy' of each declared property.
     */
    public void shallowMerge(final Object from, final Object to) throws PropertyException {

        visitProperties(new PropertyVisitor() {

            public boolean visitAttribute(AttributeProperty property) {
                property.writePropertyDirectly(
                        to,
                        property.readPropertyDirectly(to),
                        property.readPropertyDirectly(from));
                return true;
            }

            public boolean visitToOne(ToOneProperty property) {
                property.invalidate(to);
                return true;
            }

            public boolean visitToMany(ToManyProperty property) {
                return true;
            }
        });
    }

    /**
     * @since 3.0
     */
    public boolean visitDeclaredProperties(PropertyVisitor visitor) {
        Iterator it = declaredProperties.values().iterator();
        while (it.hasNext()) {
            Property next = (Property) it.next();
            if (!next.visit(visitor)) {
                return false;
            }
        }

        return true;
    }

    /**
     * @since 3.0
     */
    public boolean visitAllProperties(PropertyVisitor visitor) {
        if (!visitProperties(visitor)) {
            return false;
        }

        if (!subclassDescriptors.isEmpty()) {
            Iterator it = subclassDescriptors.values().iterator();
            while (it.hasNext()) {
                ClassDescriptor next = (ClassDescriptor) it.next();
                if (!next.visitDeclaredProperties(visitor)) {
                    return false;
                }
            }
        }

        return true;
    }

    public boolean visitProperties(PropertyVisitor visitor) {
        if (superclassDescriptor != null
                && !superclassDescriptor.visitProperties(visitor)) {
            return false;
        }

        return visitDeclaredProperties(visitor);
    }

    public void setPersistenceStateAccessor(Accessor persistenceStateAccessor) {
        this.persistenceStateAccessor = persistenceStateAccessor;
    }

    public void setEntity(ObjEntity entity) {
        this.entity = entity;
    }

    public void setSuperclassDescriptor(ClassDescriptor superclassDescriptor) {
        this.superclassDescriptor = superclassDescriptor;
    }
}
