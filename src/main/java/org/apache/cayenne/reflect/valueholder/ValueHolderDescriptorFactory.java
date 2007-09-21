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
package org.apache.cayenne.reflect.valueholder;

import java.util.List;

import org.apache.cayenne.ValueHolder;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.reflect.Accessor;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.ClassDescriptorFactory;
import org.apache.cayenne.reflect.ClassDescriptorMap;
import org.apache.cayenne.reflect.ListProperty;
import org.apache.cayenne.reflect.PersistentDescriptor;
import org.apache.cayenne.reflect.PersistentDescriptorFactory;
import org.apache.cayenne.reflect.Property;

/**
 * A {@link ClassDescriptorFactory} for Persistent objects that implement relationship
 * faulting via {@link ValueHolder}.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public class ValueHolderDescriptorFactory extends PersistentDescriptorFactory {

    public ValueHolderDescriptorFactory(ClassDescriptorMap descriptorMap) {
        super(descriptorMap);
    }

    protected void createToManyCollectionProperty(
            PersistentDescriptor descriptor,
            ObjRelationship relationship) {
        throw new UnsupportedOperationException("Implement me");
    }

    protected void createToManyListProperty(
            PersistentDescriptor descriptor,
            ObjRelationship relationship) {
        ClassDescriptor targetDescriptor = descriptorMap.getDescriptor(relationship
                .getTargetEntityName());
        String reverseName = relationship.getReverseRelationshipName();

        Accessor accessor = createAccessor(descriptor, relationship.getName(), List.class);
        descriptor.addDeclaredProperty(new ListProperty(
                descriptor,
                targetDescriptor,
                accessor,
                reverseName));
    }

    protected void createToManyMapProperty(
            PersistentDescriptor descriptor,
            ObjRelationship relationship) {
        throw new UnsupportedOperationException("Implement me");
    }

    protected void createToManySetProperty(
            PersistentDescriptor descriptor,
            ObjRelationship relationship) {
        throw new UnsupportedOperationException("Implement me");
    }

    protected void createToOneProperty(
            PersistentDescriptor descriptor,
            ObjRelationship relationship) {
        ClassDescriptor targetDescriptor = descriptorMap.getDescriptor(relationship
                .getTargetEntityName());
        String reverseName = relationship.getReverseRelationshipName();

        Accessor accessor = createAccessor(
                descriptor,
                relationship.getName(),
                ValueHolder.class);
        Property property = new ValueHolderProperty(
                descriptor,
                targetDescriptor,
                accessor,
                reverseName);

        descriptor.addDeclaredProperty(property);
    }
}
