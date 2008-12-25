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

import java.util.HashSet;
import java.util.Set;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.TraversalHelper;
import org.apache.cayenne.map.Attribute;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EmbeddedAttribute;
import org.apache.cayenne.map.EntityInheritanceTree;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Relationship;

/**
 * A convenience superclass for {@link ClassDescriptorFactory} implementors.
 * 
 * @since 3.0
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

        Class<?> entityClass = entity.getJavaClass();
        return getDescriptor(entity, entityClass);
    }

    protected ClassDescriptor getDescriptor(ObjEntity entity, Class<?> entityClass) {
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
        for (Attribute attribute : descriptor.getEntity().getDeclaredAttributes()) {

            if (attribute instanceof EmbeddedAttribute) {
                EmbeddedAttribute embedded = (EmbeddedAttribute) attribute;
                for (ObjAttribute objAttribute : embedded.getAttributes()) {
                    createEmbeddedAttributeProperty(descriptor, embedded, objAttribute);
                }
            }
            else if (attribute instanceof ObjAttribute) {
                createAttributeProperty(descriptor, (ObjAttribute) attribute);
            }
        }

        // only include this entity relationships and skip superclasses...
        for (Relationship relationship : descriptor
                .getEntity()
                .getDeclaredRelationships()) {

            ObjRelationship objRelationship = (ObjRelationship) relationship;

            if (relationship.isToMany()) {

                String collectionType = objRelationship.getCollectionType();
                if (collectionType == null
                        || ObjRelationship.DEFAULT_COLLECTION_TYPE.equals(collectionType)) {
                    createToManyListProperty(descriptor, objRelationship);
                }
                else if (collectionType.equals("java.util.Map")) {
                    createToManyMapProperty(descriptor, objRelationship);
                }
                else if (collectionType.equals("java.util.Set")) {
                    createToManySetProperty(descriptor, objRelationship);
                }
                else if (collectionType.equals("java.util.Collection")) {
                    createToManyCollectionProperty(descriptor, objRelationship);
                }
                else {
                    throw new IllegalArgumentException(
                            "Unsupported to-many collection type: " + collectionType);
                }
            }
            else {
                createToOneProperty(descriptor, objRelationship);
            }
        }

        EntityInheritanceTree inheritanceTree = descriptorMap
                .getResolver()
                .lookupInheritanceTree(descriptor.getEntity());
        descriptor.setEntityInheritanceTree(inheritanceTree);
        indexSubclassDescriptors(descriptor, inheritanceTree);
        indexQualifiers(descriptor, inheritanceTree);

        appendDeclaredRootDbEntity(descriptor, descriptor.getEntity());
        indexRootDbEntities(descriptor, inheritanceTree);

        indexSuperclassProperties(descriptor);

        return descriptor;
    }

    protected PersistentDescriptor createDescriptor() {
        return new PersistentDescriptor();
    }

    protected void createAttributeProperty(
            PersistentDescriptor descriptor,
            ObjAttribute attribute) {
        Class<?> propertyType = attribute.getJavaClass();
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

        Class<?> embeddableClass = embeddedAttribute.getJavaClass();

        String propertyPath = attribute.getName();
        int lastDot = propertyPath.lastIndexOf('.');
        if (lastDot <= 0 || lastDot == propertyPath.length() - 1) {
            throw new IllegalArgumentException("Invalid embeddable path: " + propertyPath);
        }

        String embeddableName = propertyPath.substring(lastDot + 1);

        EmbeddableDescriptor embeddableDescriptor = createEmbeddableDescriptor(embeddedAttribute);

        Accessor embeddedAccessor = createAccessor(descriptor, embeddedAttribute
                .getName(), embeddableClass);
        Accessor embeddedableAccessor = createEmbeddableAccessor(
                embeddableDescriptor,
                embeddableName,
                attribute.getJavaClass());

        Accessor accessor = new EmbeddedFieldAccessor(
                embeddableDescriptor,
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

            for (EntityInheritanceTree child : inheritanceTree.getChildren()) {
                ObjEntity childEntity = child.getEntity();
                descriptor.addSubclassDescriptor(
                        childEntity.getClassName(),
                        descriptorMap.getDescriptor(childEntity.getName()));

                indexSubclassDescriptors(descriptor, child);
            }
        }
    }

    protected void indexRootDbEntities(
            PersistentDescriptor descriptor,
            EntityInheritanceTree inheritanceTree) {

        if (inheritanceTree != null) {

            for (EntityInheritanceTree child : inheritanceTree.getChildren()) {
                ObjEntity childEntity = child.getEntity();
                appendDeclaredRootDbEntity(descriptor, childEntity);
                indexRootDbEntities(descriptor, child);
            }
        }
    }

    private void appendDeclaredRootDbEntity(
            PersistentDescriptor descriptor,
            ObjEntity entity) {

        DbEntity dbEntity = entity.getDbEntity();
        if (dbEntity != null) {
            // descriptor takes care of weeding off duplicates, which are likely in cases
            // of non-horizontal inheritance
            descriptor.addRootDbEntity(dbEntity);
        }
    }

    protected void indexQualifiers(
            final PersistentDescriptor descriptor,
            EntityInheritanceTree inheritanceTree) {

        Expression qualifier;

        if (inheritanceTree != null) {
            qualifier = inheritanceTree.qualifierForEntityAndSubclasses();
        }
        else {
            qualifier = descriptor.getEntity().getDeclaredQualifier();
        }

        if (qualifier != null) {

            final Set<ObjAttribute> attributes = new HashSet<ObjAttribute>();
            final DbEntity dbEntity = descriptor.getEntity().getDbEntity();

            qualifier.traverse(new TraversalHelper() {

                @Override
                public void startNode(Expression node, Expression parentNode) {
                    if (node.getType() == Expression.DB_PATH) {
                        String path = node.getOperand(0).toString();
                        final DbAttribute attribute = (DbAttribute) dbEntity
                                .getAttribute(path);
                        if (attribute != null) {

                            ObjAttribute objectAttribute = descriptor
                                    .getEntity()
                                    .getAttributeForDbAttribute(attribute);

                            if (objectAttribute == null) {
                                objectAttribute = new ObjAttribute(attribute.getName()) {

                                    @Override
                                    public DbAttribute getDbAttribute() {
                                        return attribute;
                                    }
                                };

                                // we semi-officially DO NOT support inheritance
                                // descriptors based on related entities, so here we
                                // assume that DbAttribute is rooted in the root
                                // DbEntity, and no relationship is involved.
                                objectAttribute.setDbAttributePath(attribute.getName());
                                objectAttribute.setType(TypesMapping
                                        .getJavaBySqlType(attribute.getType()));
                            }

                            attributes.add(objectAttribute);
                        }
                    }
                    else if (node.getType() == Expression.OBJ_PATH) {
                        String path = node.getOperand(0).toString();
                        attributes.add((ObjAttribute) descriptor
                                .getEntity()
                                .getAttribute(path));
                    }
                }
            });

            descriptor.setDiscriminatorColumns(attributes);
            descriptor.setEntityQualifier(qualifier);
        }
    }

    /**
     * Adds superclass properties to the descriptor, applying proper overrides.
     */
    protected void indexSuperclassProperties(final PersistentDescriptor descriptor) {
        ClassDescriptor superDescriptor = descriptor.getSuperclassDescriptor();
        if (superDescriptor != null) {

            superDescriptor.visitProperties(new PropertyVisitor() {

                public boolean visitAttribute(AttributeProperty property) {
                    // decorate super property to return an overridden attribute
                    descriptor.addSuperProperty(new AttributePropertyDecorator(
                            descriptor,
                            property));
                    return true;
                }

                public boolean visitToMany(ToManyProperty property) {
                    descriptor.addSuperProperty(property);
                    return true;
                }

                public boolean visitToOne(ToOneProperty property) {
                    descriptor.addSuperProperty(property);
                    return true;
                }
            });
        }
    }

    /**
     * Creates an accessor for the property.
     */
    protected Accessor createAccessor(
            PersistentDescriptor descriptor,
            String propertyName,
            Class<?> propertyType) throws PropertyException {
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
            EmbeddableDescriptor descriptor,
            String propertyName,
            Class<?> propertyType) {
        return new FieldAccessor(descriptor.getObjectClass(), propertyName, propertyType);
    }

    /**
     * Creates a descriptor of the embedded property.
     */
    protected EmbeddableDescriptor createEmbeddableDescriptor(
            EmbeddedAttribute embeddedAttribute) {
        // TODO: andrus, 11/19/2007 = avoid creation of descriptor for every property of
        // embeddable; look up reusable descriptor instead.
        return new FieldEmbeddableDescriptor(
                embeddedAttribute.getEmbeddable(),
                "owner",
                "embeddedProperty");
    }
}
