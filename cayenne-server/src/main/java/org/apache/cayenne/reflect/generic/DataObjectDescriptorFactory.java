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

import org.apache.cayenne.DataObject;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.Accessor;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.ClassDescriptorFactory;
import org.apache.cayenne.reflect.ClassDescriptorMap;
import org.apache.cayenne.reflect.FaultFactory;
import org.apache.cayenne.reflect.PersistentDescriptor;
import org.apache.cayenne.reflect.PersistentDescriptorFactory;
import org.apache.cayenne.reflect.PropertyException;

/**
 * A {@link ClassDescriptorFactory} that creates descriptors for classes implementing
 * {@link DataObject}.
 * 
 * @since 3.0
 */
public class DataObjectDescriptorFactory extends PersistentDescriptorFactory {

    protected FaultFactory faultFactory;

    public DataObjectDescriptorFactory(ClassDescriptorMap descriptorMap,
            FaultFactory faultFactory) {
        super(descriptorMap);
        this.faultFactory = faultFactory;
    }

    @Override
    protected ClassDescriptor getDescriptor(ObjEntity entity, Class<?> entityClass) {
        if (!DataObject.class.isAssignableFrom(entityClass)) {
            return null;
        }

        return super.getDescriptor(entity, entityClass);
    }

    @Override
    protected PersistentDescriptor createDescriptor() {
        return new DataObjectDescriptor();
    }

    @Override
    protected void createAttributeProperty(
            PersistentDescriptor descriptor,
            ObjAttribute attribute) {
        descriptor.addDeclaredProperty(new DataObjectAttributeProperty(attribute));
    }

    @Override
    protected void createToManyListProperty(
            PersistentDescriptor descriptor,
            ObjRelationship relationship) {

        ClassDescriptor targetDescriptor = descriptorMap.getDescriptor(relationship
                .getTargetEntityName());
        descriptor.addDeclaredProperty(new DataObjectToManyProperty(
                relationship,
                targetDescriptor,
                faultFactory.getListFault()));
    }

    @Override
    protected void createToManyMapProperty(
            PersistentDescriptor descriptor,
            ObjRelationship relationship) {
        ClassDescriptor targetDescriptor = descriptorMap.getDescriptor(relationship
                .getTargetEntityName());

        Accessor mapKeyAccessor = createMapKeyAccessor(relationship, targetDescriptor);
        descriptor.addDeclaredProperty(new DataObjectToManyMapProperty(
                relationship,
                targetDescriptor,
                faultFactory.getMapFault(mapKeyAccessor),
                mapKeyAccessor));
    }

    @Override
    protected void createToManySetProperty(
            PersistentDescriptor descriptor,
            ObjRelationship relationship) {
        ClassDescriptor targetDescriptor = descriptorMap.getDescriptor(relationship
                .getTargetEntityName());
        descriptor.addDeclaredProperty(new DataObjectToManyProperty(
                relationship,
                targetDescriptor,
                faultFactory.getSetFault()));
    }

    @Override
    protected void createToManyCollectionProperty(
            PersistentDescriptor descriptor,
            ObjRelationship relationship) {
        ClassDescriptor targetDescriptor = descriptorMap.getDescriptor(relationship
                .getTargetEntityName());
        descriptor.addDeclaredProperty(new DataObjectToManyProperty(
                relationship,
                targetDescriptor,
                faultFactory.getCollectionFault()));
    }

    @Override
    protected void createToOneProperty(
            PersistentDescriptor descriptor,
            ObjRelationship relationship) {

        ClassDescriptor targetDescriptor = descriptorMap.getDescriptor(relationship
                .getTargetEntityName());
        descriptor.addDeclaredProperty(new DataObjectToOneProperty(
                relationship,
                targetDescriptor,
                faultFactory.getToOneFault()));
    }

    @Override
    protected Accessor createAccessor(
            PersistentDescriptor descriptor,
            String propertyName,
            Class<?> propertyType) throws PropertyException {
        return new DataObjectAccessor(propertyName);
    }
}
