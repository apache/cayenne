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

import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.Accessor;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.ClassDescriptorMap;
import org.apache.cayenne.reflect.FaultFactory;
import org.apache.cayenne.reflect.PersistentDescriptor;
import org.apache.cayenne.reflect.Property;
import org.apache.cayenne.reflect.pojo.EnhancedPojoDescriptorFactory;

public class JpaClassDescriptorFactory extends EnhancedPojoDescriptorFactory {

    public JpaClassDescriptorFactory(ClassDescriptorMap descriptorMap,
            FaultFactory faultFactory) {
        super(descriptorMap, faultFactory);
    }

    @Override
    protected void createToManyProperty(
            PersistentDescriptor descriptor,
            ObjRelationship relationship) {
        ClassDescriptor targetDescriptor = descriptorMap.getDescriptor(relationship
                .getTargetEntityName());
        String reverseName = relationship.getReverseRelationshipName();

        String collectionType = relationship.getCollectionType();
        Property property;

        if (collectionType == null
                || ObjRelationship.DEFAULT_COLLECTION_TYPE.equals(collectionType)) {

            Accessor accessor = new JpaCollectionFieldAccessor(descriptor
                    .getObjectClass(), relationship.getName(), List.class);

            property = new JpaListProperty(
                    descriptor,
                    targetDescriptor,
                    accessor,
                    reverseName);
        }
        else if (collectionType.equals("java.util.Map")) {
            Accessor accessor = new JpaCollectionFieldAccessor(descriptor
                    .getObjectClass(), relationship.getName(), Map.class);
            property = new JpaMapProperty(
                    descriptor,
                    targetDescriptor,
                    accessor,
                    reverseName);
        }
        else if (collectionType.equals("java.util.Set")) {
            Accessor accessor = new JpaCollectionFieldAccessor(descriptor
                    .getObjectClass(), relationship.getName(), Set.class);
            property = new JpaSetProperty(
                    descriptor,
                    targetDescriptor,
                    accessor,
                    reverseName);
        }
        else if (collectionType.equals("java.util.Collection")) {
            Accessor accessor = new JpaCollectionFieldAccessor(descriptor
                    .getObjectClass(), relationship.getName(), Collection.class);
            property = new JpaListProperty(
                    descriptor,
                    targetDescriptor,
                    accessor,
                    reverseName);
        }
        else {
            throw new IllegalArgumentException("Unsupported to many collection type: "
                    + collectionType);
        }

        descriptor.addDeclaredProperty(property);
    }
}
