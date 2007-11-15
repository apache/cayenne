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

import java.util.Iterator;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.EmbeddedAttribute;
import org.apache.cayenne.map.EntityInheritanceTree;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;

/**
 * A convenience superclass for {@link ClassDescriptorFactory} implementors.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public abstract class PersistentDescriptorFactory implements ClassDescriptorFactory {

    protected ClassDescriptorMap descriptorMap;

    public PersistentDescriptorFactory(ClassDescriptorMap descriptorMap) {
        this.descriptorMap = descriptorMap;
    }

    public ClassDescriptor getDescriptor(String entityName) {
        ObjEntity entity = descriptorMap.getResolver().getObjEntity(entityName);
        if (entity == null) {
            throw new CayenneRuntimeException("Unmapped entity: " + entityName);
        }

        Class entityClass = entity.getJavaClass();
        return getDescriptor(entity, entityClass);
    }

    protected ClassDescriptor getDescriptor(ObjEntity entity, Class entityClass) {
        String superEntityName = entity.getSuperEntityName();

        ClassDescriptor superDescriptor = (superEntityName != null) ? descriptorMap
                .getDescriptor(superEntityName) : null;

        PersistentDescriptor descriptor = createDescriptor();

        descriptor.setEntity(entity);
        descriptor.setSuperclassDescriptor(superDescriptor);
        descriptor.setObjectClass(entityClass);
        descriptor.setPersistenceStateAccessor(new BeanAccessor(
                entityClass,
                "persistenceState",
                Integer.TYPE));

        // only include this entity attributes and skip superclasses...
        Iterator attributes = descriptor.getEntity().getDeclaredAttributes().iterator();
        while (attributes.hasNext()) {
            Object attribute = attributes.next();

            if (attribute instanceof ObjAttribute) {
                createAttributeProperty(descriptor, (ObjAttribute) attribute);
            }
            else if (attribute instanceof EmbeddedAttribute) {
                EmbeddedAttribute embedded = (EmbeddedAttribute) attribute;
                Iterator embeddedAttributes = embedded.getAttributes().iterator();
                while (embeddedAttributes.hasNext()) {
                    createEmbeddedAttributeProperty(
                            descriptor,
                            embedded,
                            (ObjAttribute) embeddedAttributes.next());
                }
            }
        }

        // only include this entity relationships and skip superclasses...
        Iterator it = descriptor.getEntity().getDeclaredRelationships().iterator();
        while (it.hasNext()) {

            ObjRelationship relationship = (ObjRelationship) it.next();
            if (relationship.isToMany()) {

                String collectionType = relationship.getCollectionType();
                if (collectionType == null
                        || ObjRelationship.DEFAULT_COLLECTION_TYPE.equals(collectionType)) {
                    createToManyListProperty(descriptor, relationship);
                }
                else if (collectionType.equals("java.util.Map")) {
                    createToManyMapProperty(descriptor, relationship);
                }
                else if (collectionType.equals("java.util.Set")) {
                    createToManySetProperty(descriptor, relationship);
                }
                else if (collectionType.equals("java.util.Collection")) {
                    createToManyCollectionProperty(descriptor, relationship);
                }
                else {
                    throw new IllegalArgumentException(
                            "Unsupported to-many collection type: " + collectionType);
                }
            }
            else {
                createToOneProperty(descriptor, relationship);
            }
        }

        EntityInheritanceTree inheritanceTree = descriptorMap
                .getResolver()
                .lookupInheritanceTree(descriptor.getEntity());
        indexSubclassDescriptors(descriptor, inheritanceTree);

        return descriptor;
    }

    protected PersistentDescriptor createDescriptor() {
        return new PersistentDescriptor();
    }

    protected void createAttributeProperty(
            PersistentDescriptor descriptor,
            ObjAttribute attribute) {
        Class propertyType = attribute.getJavaClass();
        Accessor accessor = createAccessor(descriptor, attribute.getName(), propertyType);
        descriptor.addDeclaredProperty(new SimpleAttributeProperty(
                descriptor,
                accessor,
                attribute));
    }

    protected void createEmbeddedAttributeProperty(
            PersistentDescriptor descriptor,
            EmbeddedAttribute embeddedAttribute,
            ObjAttribute attribute) {

        Class embeddableClass = embeddedAttribute.getJavaClass();

        String propertyPath = attribute.getName();
        int lastDot = propertyPath.lastIndexOf('.');
        if (lastDot <= 0 || lastDot == propertyPath.length() - 1) {
            throw new IllegalArgumentException("Invalid embeddable path: " + propertyPath);
        }

        String embeddableName = propertyPath.substring(lastDot + 1);

        Accessor embeddedAccessor = createAccessor(descriptor, embeddedAttribute
                .getName(), embeddableClass);
        Accessor embeddedableAccessor = createEmbeddableAccessor(
                embeddableClass,
                embeddableName,
                attribute.getJavaClass());

        Accessor accessor = new EmbeddedFieldAccessor(
                embeddableClass,
                embeddedAccessor,
                embeddedableAccessor);
        descriptor.addDeclaredProperty(new SimpleAttributeProperty(
                descriptor,
                accessor,
                attribute));
    }

    protected abstract void createToOneProperty(
            PersistentDescriptor descriptor,
            ObjRelationship relationship);

    protected abstract void createToManySetProperty(
            PersistentDescriptor descriptor,
            ObjRelationship relationship);

    protected abstract void createToManyMapProperty(
            PersistentDescriptor descriptor,
            ObjRelationship relationship);

    protected abstract void createToManyListProperty(
            PersistentDescriptor descriptor,
            ObjRelationship relationship);

    protected abstract void createToManyCollectionProperty(
            PersistentDescriptor descriptor,
            ObjRelationship relationship);

    protected void indexSubclassDescriptors(
            PersistentDescriptor descriptor,
            EntityInheritanceTree inheritanceTree) {

        if (inheritanceTree != null) {

            Iterator it = inheritanceTree.getChildren().iterator();
            while (it.hasNext()) {
                EntityInheritanceTree child = (EntityInheritanceTree) it.next();
                descriptor.addSubclassDescriptor(descriptorMap.getDescriptor(child
                        .getEntity()
                        .getName()));

                indexSubclassDescriptors(descriptor, child);
            }
        }
    }

    /**
     * Creates an accessor for the property.
     */
    protected Accessor createAccessor(
            PersistentDescriptor descriptor,
            String propertyName,
            Class propertyType) throws PropertyException {
        return new FieldAccessor(descriptor.getObjectClass(), propertyName, propertyType);
    }

    /**
     * Creates an accessor to read a map key for a given relationship.
     */
    protected Accessor createMapKeyAccessor(
            ObjRelationship relationship,
            ClassDescriptor targetDescriptor) {

        String mapKey = relationship.getMapKey();
        if (mapKey != null) {
            return new PropertyAccessor(targetDescriptor.getProperty(mapKey));
        }

        return IdMapKeyAccessor.SHARED_ACCESSOR;
    }

    /**
     * Creates an accessor for the property of the embeddable class.
     */
    protected Accessor createEmbeddableAccessor(
            Class embeddableClass,
            String propertyName,
            Class propertyType) {
        return new FieldAccessor(embeddableClass, propertyName, propertyType);
    }
}
