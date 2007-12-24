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

package org.apache.cayenne.jpa.reflect;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cayenne.jpa.map.AccessType;
import org.apache.cayenne.jpa.map.JpaEntityMap;
import org.apache.cayenne.jpa.map.JpaManagedClass;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.Accessor;
import org.apache.cayenne.reflect.BeanAccessor;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.ClassDescriptorMap;
import org.apache.cayenne.reflect.EmbeddableDescriptor;
import org.apache.cayenne.reflect.FaultFactory;
import org.apache.cayenne.reflect.PersistentDescriptor;
import org.apache.cayenne.reflect.Property;
import org.apache.cayenne.reflect.PropertyException;
import org.apache.cayenne.reflect.pojo.EnhancedPojoDescriptorFactory;

public class JpaClassDescriptorFactory extends EnhancedPojoDescriptorFactory {

    protected JpaEntityMap entityMap;

    public JpaClassDescriptorFactory(JpaEntityMap entityMap,
            ClassDescriptorMap descriptorMap, FaultFactory faultFactory) {
        super(descriptorMap, faultFactory);
        this.entityMap = entityMap;
    }
    
    @Override
    protected Accessor createEmbeddableAccessor(
            EmbeddableDescriptor descriptor,
            String propertyName,
            Class<?> propertyType) {
        
        String className = descriptor.getObjectClass().getName();
        JpaManagedClass managedClass = entityMap.getManagedClass(className);
        if (managedClass == null) {
            throw new IllegalArgumentException("Not a managed class: " + className);
        }
        
        if (managedClass.getAccess() == AccessType.PROPERTY) {
            return new BeanAccessor(
                    descriptor.getObjectClass(),
                    propertyName,
                    propertyType);
        }
        else {
            return super.createEmbeddableAccessor(descriptor, propertyName, propertyType);
        }
    }

    @Override
    protected Accessor createAccessor(
            PersistentDescriptor descriptor,
            String propertyName,
            Class<?> propertyType) throws PropertyException {

        String className = descriptor.getObjectClass().getName();
        JpaManagedClass managedClass = entityMap.getManagedClass(className);
        if (managedClass == null) {
            throw new IllegalArgumentException("Not a managed class: " + className);
        }

        if (managedClass.getAccess() == AccessType.PROPERTY) {
            return new BeanAccessor(
                    descriptor.getObjectClass(),
                    propertyName,
                    propertyType);
        }
        else {
            return super.createAccessor(descriptor, propertyName, propertyType);
        }
    }

    protected void createToManyListProperty(
            PersistentDescriptor descriptor,
            ObjRelationship relationship) {
        ClassDescriptor targetDescriptor = descriptorMap.getDescriptor(relationship
                .getTargetEntityName());
        String reverseName = relationship.getReverseRelationshipName();

        Accessor accessor = new JpaCollectionFieldAccessor(
                descriptor.getObjectClass(),
                relationship.getName(),
                List.class);
        Property property = new JpaListProperty(
                descriptor,
                targetDescriptor,
                accessor,
                reverseName);

        descriptor.addDeclaredProperty(property);
    }

    protected void createToManyMapProperty(
            PersistentDescriptor descriptor,
            ObjRelationship relationship) {
        ClassDescriptor targetDescriptor = descriptorMap.getDescriptor(relationship
                .getTargetEntityName());
        String reverseName = relationship.getReverseRelationshipName();
        Accessor accessor = new JpaCollectionFieldAccessor(
                descriptor.getObjectClass(),
                relationship.getName(),
                Map.class);
        Accessor mapKeyAccessor = createMapKeyAccessor(relationship, targetDescriptor);
        Property property = new JpaMapProperty(
                descriptor,
                targetDescriptor,
                accessor,
                reverseName,
                mapKeyAccessor);

        descriptor.addDeclaredProperty(property);
    }

    protected void createToManySetProperty(
            PersistentDescriptor descriptor,
            ObjRelationship relationship) {
        ClassDescriptor targetDescriptor = descriptorMap.getDescriptor(relationship
                .getTargetEntityName());
        String reverseName = relationship.getReverseRelationshipName();
        Accessor accessor = new JpaCollectionFieldAccessor(
                descriptor.getObjectClass(),
                relationship.getName(),
                Set.class);
        Property property = new JpaSetProperty(
                descriptor,
                targetDescriptor,
                accessor,
                reverseName);

        descriptor.addDeclaredProperty(property);
    }

    protected void createToManyCollectionProperty(
            PersistentDescriptor descriptor,
            ObjRelationship relationship) {
        ClassDescriptor targetDescriptor = descriptorMap.getDescriptor(relationship
                .getTargetEntityName());
        String reverseName = relationship.getReverseRelationshipName();

        Accessor accessor = new JpaCollectionFieldAccessor(
                descriptor.getObjectClass(),
                relationship.getName(),
                Collection.class);
        Property property = new JpaListProperty(
                descriptor,
                targetDescriptor,
                accessor,
                reverseName);

        descriptor.addDeclaredProperty(property);
    }
}
