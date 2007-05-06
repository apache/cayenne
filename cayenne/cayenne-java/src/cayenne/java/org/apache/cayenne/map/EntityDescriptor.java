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

package org.apache.cayenne.map;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.cayenne.DataObject;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.property.BaseClassDescriptor;
import org.apache.cayenne.property.BeanAccessor;
import org.apache.cayenne.property.ClassDescriptor;
import org.apache.cayenne.property.DataObjectAccessor;
import org.apache.cayenne.property.FieldAccessor;
import org.apache.cayenne.property.ListProperty;
import org.apache.cayenne.property.PersistentObjectProperty;
import org.apache.cayenne.property.Property;
import org.apache.cayenne.property.PropertyAccessException;
import org.apache.cayenne.property.PropertyAccessor;
import org.apache.cayenne.property.SimpleProperty;
import org.apache.cayenne.property.ToManyListProperty;
import org.apache.cayenne.property.ValueHolderProperty;

/**
 * A ClassDescriptor describing a persistent bean based on ObjEntity.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class EntityDescriptor extends BaseClassDescriptor {

    protected ObjEntity entity;

    // compiled properties
    protected boolean dataObject;
    boolean persistent;

    /**
     * Creates and compiles a class descriptor for a given entity. A second optional
     * 'superclassDescriptor' parameter should be used if an entity has a super-entity.
     */
    public EntityDescriptor(ObjEntity entity, ClassDescriptor superclassDescriptor) {
        super(superclassDescriptor);
        this.entity = entity;
    }

    public void shallowMerge(Object from, Object to) throws PropertyAccessException {
        super.shallowMerge(from, to);

        if (dataObject && from instanceof DataObject && to instanceof DataObject) {
            ((DataObject) to)
                    .setSnapshotVersion(((DataObject) from).getSnapshotVersion());
        }
    }

    /**
     * Returns ObjEntity described by this object.
     */
    public ObjEntity getEntity() {
        return entity;
    }

    /**
     * Registers a property. This method is useful to customize default ClassDescriptor
     * generated from ObjEntity by adding new properties or overriding the standard ones.
     */
    public void setDeclaredProperty(Property property) {
        declaredProperties.put(property.getName(), property);
    }

    /**
     * Removes declared property. This method can be used to customize default
     * ClassDescriptor generated from ObjEntity.
     */
    public void removeDeclaredProperty(String propertyName) {
        declaredProperties.remove(propertyName);
    }

    /**
     * Prepares the descriptor. A descriptor must be explicitly compiled before it can
     * operate.
     */
    public void compile(EntityResolver resolver) {
        if (entity == null) {
            throw new IllegalStateException(
                    "Entity is not initialized, can't index descriptor.");
        }

        // compile common stuff
        this.objectClass = entity.getJavaClass();
        this.persistent = Persistent.class.isAssignableFrom(objectClass);
        this.dataObject = persistent && DataObject.class.isAssignableFrom(objectClass);

        compileSpecialProperties();

        // init property descriptors...
        Map allDescriptors = new HashMap();

        compileRelationships(resolver, allDescriptors);

        // before we compile attributes, extract all relationship descriptors to a
        // separate value holder map
        if(!allDescriptors.isEmpty()) {
            this.valueHolderProperties = new HashMap(allDescriptors);
        }

        compileAttributes(allDescriptors);

        this.declaredProperties = allDescriptors;

        // init subclass lookup tree
        EntityInheritanceTree inheritanceTree = resolver.lookupInheritanceTree(entity);
        if (inheritanceTree == null) {
            this.subclassDescriptors = null;
        }
        else {
            Map subclassMapping = new HashMap();
            compileSubclassMapping(resolver, subclassMapping, inheritanceTree);
            this.subclassDescriptors = subclassMapping;
        }
    }

    protected void compileSubclassMapping(
            EntityResolver resolver,
            Map subclassDescriptors,
            EntityInheritanceTree treeNode) {
        ObjEntity entity = treeNode.getEntity();
        ClassDescriptor descriptor = resolver.getClassDescriptor(entity.getName());
        subclassDescriptors.put(entity.getClassName(), descriptor);

        Iterator it = treeNode.getChildren().iterator();
        while (it.hasNext()) {
            EntityInheritanceTree child = (EntityInheritanceTree) it.next();
            compileSubclassMapping(resolver, subclassDescriptors, child);
        }
    }

    /**
     * Implements an attributes compilation step. Called internally from "compile".
     */
    protected void compileSpecialProperties() {
        this.persistenceStateProperty = new BeanAccessor(
                objectClass,
                "persistenceState",
                Integer.TYPE);
    }

    /**
     * Implements an attributes compilation step. Called internally from "compile".
     */
    protected void compileAttributes(Map allDescriptors) {

        // only include this entity attributes and skip superclasses...
        Iterator it = entity.getDeclaredAttributes().iterator();
        while (it.hasNext()) {
            ObjAttribute attribute = (ObjAttribute) it.next();

            Class propertyType = attribute.getJavaClass();
            PropertyAccessor accessor = makeAccessor(attribute.getName(), propertyType);
            allDescriptors.put(attribute.getName(), persistent
                    ? new SimplePersistentProperty(this, accessor)
                    : new SimpleProperty(this, accessor));
        }
    }

    /**
     * Implements a relationships compilation step. Maps each to-one relationship to a
     * ValueHolderProperty and to-many - to CollectionProperty. Called internally from
     * "compile" method.
     */
    protected void compileRelationships(EntityResolver resolver, Map allDescriptors) {

        // only include this entity relationships and skip superclasses...
        Iterator it = entity.getDeclaredRelationships().iterator();
        while (it.hasNext()) {

            ObjRelationship relationship = (ObjRelationship) it.next();
            ClassDescriptor targetDescriptor = resolver.getClassDescriptor(relationship
                    .getTargetEntityName());
            String reverseName = relationship.getReverseRelationshipName();

            Property property;
            if (relationship.isToMany()) {

                PropertyAccessor accessor = makeAccessor(
                        relationship.getName(),
                        List.class);

                if (dataObject) {
                    property = new ToManyListProperty(
                            this,
                            targetDescriptor,
                            accessor,
                            reverseName);
                }
                else {
                    property = new ListProperty(
                            this,
                            targetDescriptor,
                            accessor,
                            reverseName);
                }
            }
            else {

                if (dataObject) {
                    ObjEntity targetEntity = (ObjEntity) relationship.getTargetEntity();
                    PropertyAccessor accessor = makeAccessor(
                            relationship.getName(),
                            targetEntity.getJavaClass());
                    property = new PersistentObjectProperty(
                            this,
                            targetDescriptor,
                            accessor,
                            reverseName);
                }
                else {
                    PropertyAccessor accessor = makeAccessor(
                            relationship.getName(),
                            ValueHolder.class);
                    property = new ValueHolderProperty(
                            this,
                            targetDescriptor,
                            accessor,
                            reverseName);
                }
            }

            allDescriptors.put(relationship.getName(), property);
        }
    }

    /*
     * Creates an accessor for the property.
     */
    PropertyAccessor makeAccessor(String propertyName, Class propertyType)
            throws PropertyAccessException {

        if (dataObject) {
            return new DataObjectAccessor(propertyName);
        }

        try {
            return new FieldAccessor(objectClass, propertyName, propertyType);
        }
        catch (Throwable th) {

            throw new PropertyAccessException("Can't create accessor for property '"
                    + propertyName
                    + "' of class '"
                    + objectClass.getName()
                    + "'", null, null);
        }
    }

    /**
     * Overrides toString method of Object to provide a meaningful description.
     */
    public String toString() {
        String entityName = (entity != null) ? entity.getName() : null;
        String className = (objectClass != null) ? objectClass.getName() : null;
        return new ToStringBuilder(this).append("entity", entityName).append(
                "objectClass",
                className).toString();
    }

    // TODO: andrus, 4/26/2006 - this property was added when 1.2 was in public API freeze
    // - move it to the property package past 1.2
    final class SimplePersistentProperty extends SimpleProperty {

        SimplePersistentProperty(ClassDescriptor owner, PropertyAccessor accessor) {
            super(owner, accessor);
        }

        public Object readProperty(Object object) throws PropertyAccessException {
            resolveFault(object);
            return super.readProperty(object);
        }

        public void writeProperty(Object object, Object oldValue, Object newValue)
                throws PropertyAccessException {
            resolveFault(object);
            super.writeProperty(object, oldValue, newValue);
        }

        void resolveFault(Object object) throws PropertyAccessException {
            try {
                Persistent persistent = (Persistent) object;
                ObjectContext context = persistent.getObjectContext();
                if (context != null) {
                    context.prepareForAccess(persistent, getName());
                }
            }
            catch (ClassCastException e) {
                throw new PropertyAccessException("Object is not a Persistent: '"
                        + object.getClass().getName()
                        + "'", this, object, e);
            }
        }
    }
}
