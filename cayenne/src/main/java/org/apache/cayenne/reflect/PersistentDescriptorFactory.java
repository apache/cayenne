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
package org.apache.cayenne.reflect;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.TraversalHandler;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.DeleteRule;
import org.apache.cayenne.map.EmbeddedAttribute;
import org.apache.cayenne.map.EntityInheritanceTree;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.util.CayenneMapEntry;

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
            throw new CayenneRuntimeException("Unmapped entity: %s", entityName);
        }

        Class<?> entityClass = descriptorMap.getResolver().getObjectFactory().getJavaClass(entity.getJavaClassName());
        return getDescriptor(entity, entityClass);
    }

    protected ClassDescriptor getDescriptor(ObjEntity entity, Class<?> entityClass) {
        String superEntityName = entity.getSuperEntityName();

        ClassDescriptor superDescriptor = (superEntityName != null)
                ? descriptorMap.getDescriptor(superEntityName)
                : null;

        PersistentDescriptor descriptor = createDescriptor();

        descriptor.setEntity(entity);
        descriptor.setSuperclassDescriptor(superDescriptor);
        descriptor.setObjectClass(entityClass);
        descriptor.setPersistenceStateAccessor(new BeanAccessor(entityClass, "persistenceState", Integer.TYPE));

        // only include this entity attributes and skip superclasses...
        for (ObjAttribute attribute : descriptor.getEntity().getDeclaredAttributes()) {

            if (attribute instanceof EmbeddedAttribute) {
                EmbeddedAttribute embedded = (EmbeddedAttribute) attribute;
                for (ObjAttribute objAttribute : embedded.getAttributes()) {
                    createEmbeddedAttributeProperty(descriptor, embedded, objAttribute);
                }
            } else {
                createAttributeProperty(descriptor, attribute);
            }
        }

        // only include this entity relationships and skip superclasses...
        for (ObjRelationship relationship : descriptor.getEntity().getDeclaredRelationships()) {

            if (relationship.isToMany()) {

                String collectionType = relationship.getCollectionType();
                if (collectionType == null || ObjRelationship.DEFAULT_COLLECTION_TYPE.equals(collectionType)) {
                    createToManyListProperty(descriptor, relationship);
                } else if ("java.util.Map".equals(collectionType)) {
                    createToManyMapProperty(descriptor, relationship);
                } else if ("java.util.Set".equals(collectionType)) {
                    createToManySetProperty(descriptor, relationship);
                } else if ("java.util.Collection".equals(collectionType)) {
                    createToManyCollectionProperty(descriptor, relationship);
                } else {
                    throw new IllegalArgumentException("Unsupported to-many collection type: " + collectionType);
                }
            } else {
                createToOneProperty(descriptor, relationship);
            }
        }

        EntityInheritanceTree inheritanceTree = descriptorMap.getResolver().getInheritanceTree(descriptor.getEntity().getName());
        descriptor.setEntityInheritanceTree(inheritanceTree);
        indexSubclassDescriptors(descriptor, inheritanceTree);
        indexQualifiers(descriptor, inheritanceTree);

        appendDeclaredRootDbEntity(descriptor, descriptor.getEntity());
        indexRootDbEntities(descriptor, inheritanceTree);

        indexSuperclassProperties(descriptor);
        indexAdditionalDbEntities(descriptor);

        descriptor.sortProperties();

        return descriptor;
    }

    protected PersistentDescriptor createDescriptor() {
        return new PersistentDescriptor();
    }

    protected void createAttributeProperty(PersistentDescriptor descriptor, ObjAttribute attribute) {
        Class<?> propertyType = attribute.getJavaClass();
        Accessor accessor = createAccessor(descriptor, attribute.getName(), propertyType);
        descriptor.addDeclaredProperty(new SimpleAttributeProperty(descriptor, accessor, attribute));
    }

    protected void createEmbeddedAttributeProperty(PersistentDescriptor descriptor,
            EmbeddedAttribute embeddedAttribute, ObjAttribute attribute) {

        Class<?> embeddableClass = embeddedAttribute.getJavaClass();

        String propertyPath = attribute.getName();
        int lastDot = propertyPath.lastIndexOf('.');
        if (lastDot <= 0 || lastDot == propertyPath.length() - 1) {
            throw new IllegalArgumentException("Invalid embeddable path: " + propertyPath);
        }

        String embeddableName = propertyPath.substring(lastDot + 1);

        EmbeddableDescriptor embeddableDescriptor = createEmbeddableDescriptor(embeddedAttribute);

        Accessor embeddedAccessor = createAccessor(descriptor, embeddedAttribute.getName(), embeddableClass);
        Accessor embeddedableAccessor = createEmbeddableAccessor(embeddableDescriptor, embeddableName,
                attribute.getJavaClass());

        Accessor accessor = new EmbeddedFieldAccessor(embeddableDescriptor, embeddedAccessor, embeddedableAccessor);
        descriptor.addDeclaredProperty(new SimpleAttributeProperty(descriptor, accessor, attribute));
    }

    protected abstract void createToOneProperty(PersistentDescriptor descriptor, ObjRelationship relationship);

    protected abstract void createToManySetProperty(PersistentDescriptor descriptor, ObjRelationship relationship);

    protected abstract void createToManyMapProperty(PersistentDescriptor descriptor, ObjRelationship relationship);

    protected abstract void createToManyListProperty(PersistentDescriptor descriptor, ObjRelationship relationship);

    protected abstract void createToManyCollectionProperty(PersistentDescriptor descriptor, ObjRelationship relationship);

    protected void indexSubclassDescriptors(PersistentDescriptor descriptor, EntityInheritanceTree inheritanceTree) {

        if (inheritanceTree != null) {

            for (EntityInheritanceTree child : inheritanceTree.getChildren()) {
                ObjEntity childEntity = child.getEntity();
                descriptor.addSubclassDescriptor(childEntity.getName(),
                        descriptorMap.getDescriptor(childEntity.getName()));

                indexSubclassDescriptors(descriptor, child);
            }
        }
    }

    protected void indexRootDbEntities(PersistentDescriptor descriptor, EntityInheritanceTree inheritanceTree) {

        if (inheritanceTree != null) {

            for (EntityInheritanceTree child : inheritanceTree.getChildren()) {
                ObjEntity childEntity = child.getEntity();
                appendDeclaredRootDbEntity(descriptor, childEntity);
                indexRootDbEntities(descriptor, child);
            }
        }
    }

    private void appendDeclaredRootDbEntity(PersistentDescriptor descriptor, ObjEntity entity) {

        DbEntity dbEntity = entity.getDbEntity();
        if (dbEntity != null) {
            // descriptor takes care of weeding off duplicates, which are likely
            // in cases of non-horizontal inheritance
            descriptor.addRootDbEntity(dbEntity);
        }
    }

    protected void indexQualifiers(final PersistentDescriptor descriptor, EntityInheritanceTree inheritanceTree) {

        Expression qualifier;

        if (inheritanceTree != null) {
            qualifier = inheritanceTree.qualifierForEntityAndSubclasses();
        } else {
            qualifier = descriptor.getEntity().getDeclaredQualifier();
        }

        if (qualifier != null) {

            // using map instead of a Set to collect attributes, as
            // ObjEntity.getAttribute may return a decorator for attribute on
            // each call, resulting in dupes
            final Map<String, ObjAttribute> attributes = new HashMap<>();
            final DbEntity dbEntity = descriptor.getEntity().getDbEntity();

            qualifier.traverse(new TraversalHandler() {

                @Override
                public void startNode(Expression node, Expression parentNode) {
                    if (node.getType() == Expression.DB_PATH) {
                        String path = node.getOperand(0).toString();
                        final DbAttribute attribute = dbEntity.getAttribute(path);
                        if (attribute != null) {

                            ObjAttribute objectAttribute = descriptor.getEntity().getAttributeForDbAttribute(attribute);

                            if (objectAttribute == null) {
                                objectAttribute = new ObjAttribute(attribute.getName()) {

                                    @Override
                                    public DbAttribute getDbAttribute() {
                                        return attribute;
                                    }
                                };

                                // we semi-officially DO NOT support inheritance
                                // descriptors based on related entities, so
                                // here we
                                // assume that DbAttribute is rooted in the root
                                // DbEntity, and no relationship is involved.
                                objectAttribute.setDbAttributePath(attribute.getName());
                                objectAttribute.setType(TypesMapping.getJavaBySqlType(attribute));
                            }

                            attributes.put(objectAttribute.getName(), objectAttribute);
                        }
                    } else if (node.getType() == Expression.OBJ_PATH) {
                        String path = node.getOperand(0).toString();
                        ObjAttribute attribute = descriptor.getEntity().getAttribute(path);
                        attributes.put(path, attribute);
                    }
                }
            });

            descriptor.setDiscriminatorColumns(attributes.values());
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
                    descriptor.addSuperProperty(new AttributePropertyDecorator(descriptor, property));
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

    protected void indexAdditionalDbEntities(final PersistentDescriptor descriptor) {
        descriptor.visitProperties(new PropertyVisitor() {
            @Override
            public boolean visitAttribute(AttributeProperty property) {
                if(!property.getAttribute().isFlattened()) {
                    return true;
                }

                Iterator<CayenneMapEntry> it = property.getAttribute().getDbPathIterator();
                CayennePath path = CayennePath.EMPTY_PATH;
                while(it.hasNext()) {
                    CayenneMapEntry next = it.next();
                    if(next instanceof DbRelationship) {
                        DbRelationship rel = (DbRelationship)next;
                        // When deleting an ObjEntity that has flattened attributes we also delete the target
                        // DbEntity row for the flattened attribute (see RootRowOpProcessor.visitDelete),
                        // EXCEPT if the target dbEntity has a toMany relationship with the source dbEntity
                        // (as there may then be other records in the source entity with references to it)
                        // OR delete rules prevent it (i.e. if there are any ObjRelationships matching the
                        // flattened attributes DbRelationship that don't have a CASCADE delete rule).
                        boolean blockCascadeDelete = rel.getReverseRelationship().isToMany()
                                                  || descriptor.getEntity().getRelationships().stream()
                                                     .filter(r -> r.getDbRelationships().equals(List.of(rel)))
                                                     .anyMatch(r -> r.getDeleteRule() != DeleteRule.CASCADE);

                        path = path.dot(rel.getName());
                        descriptor.addAdditionalDbEntity(path, rel.getTargetEntity(), blockCascadeDelete);
                    }
                }
                return true;
            }

            @Override
            public boolean visitToOne(ToOneProperty property) {
                if(!property.getRelationship().isFlattened()) {
                    return true;
                }

                List<DbRelationship> dbRelationships = property.getRelationship().getDbRelationships();
                CayennePath path = CayennePath.EMPTY_PATH;
                int count = dbRelationships.size();
                for(int i=0; i<count-1; i++) {
                    DbRelationship rel = dbRelationships.get(i);
                    path = path.dot(rel.getName());
                    descriptor.addAdditionalDbEntity(path, rel.getTargetEntity(), false);
                }
                return true;
            }

            @Override
            public boolean visitToMany(ToManyProperty property) {
                return true;
            }
        });
    }

    /**
     * Creates an accessor for the property.
     */
    protected Accessor createAccessor(PersistentDescriptor descriptor, String propertyName, Class<?> propertyType)
            throws PropertyException {
        return new FieldAccessor(descriptor.getObjectClass(), propertyName, propertyType);
    }

    /**
     * Creates an accessor to read a map key for a given relationship.
     */
    protected Accessor createMapKeyAccessor(ObjRelationship relationship, ClassDescriptor targetDescriptor) {

        String mapKey = relationship.getMapKey();
        if (mapKey != null) {
            return new PropertyAccessor(targetDescriptor.getProperty(mapKey));
        }

        return IdMapKeyAccessor.SHARED_ACCESSOR;
    }

    /**
     * Creates an accessor for the property of the embeddable class.
     */
    protected Accessor createEmbeddableAccessor(EmbeddableDescriptor descriptor, String propertyName,
                                                Class<?> propertyType) {
        return new FieldAccessor(descriptor.getObjectClass(), propertyName, propertyType);
    }

    /**
     * Creates a descriptor of the embedded property.
     */
    protected EmbeddableDescriptor createEmbeddableDescriptor(EmbeddedAttribute embeddedAttribute) {
        // TODO: andrus, 11/19/2007 = avoid creation of descriptor for every property of embeddable;
        //       look up reusable descriptor instead.
        return new FieldEmbeddableDescriptor(descriptorMap.getResolver().getObjectFactory(),
                embeddedAttribute.getEmbeddable(), "owner", "embeddedProperty");
    }
}
