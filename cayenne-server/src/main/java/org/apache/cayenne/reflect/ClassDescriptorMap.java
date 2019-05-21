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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.EntityResolver;

/**
 * An object that holds class descriptors for mapped entities, compiling new
 * descriptors on demand using an internal chain of descriptor factories. Note
 * that the object is not synchronized internally, so it has to be prefilled
 * with descriptors by the caller on initialization via calling 'getDescriptor'
 * for all mapped entities.
 * 
 * @since 3.0
 */
public class ClassDescriptorMap {

    protected EntityResolver resolver;
    protected Map<String, ClassDescriptor> descriptors;
    protected List<ClassDescriptorFactory> factories;

    public ClassDescriptorMap(EntityResolver resolver) {
        this.descriptors = new HashMap<>();
        this.resolver = resolver;
        this.factories = new ArrayList<>();
    }

    public EntityResolver getResolver() {
        return resolver;
    }

    /**
     * Adds a factory to the descriptor factory chain.
     */
    public void addFactory(ClassDescriptorFactory factory) {
        factories.add(factory);
    }

    public void removeFactory(ClassDescriptorFactory factory) {
        factories.remove(factory);
    }

    public void clearFactories() {
        factories.clear();
    }

    public void clearDescriptors() {
        descriptors.clear();
    }

    /**
     * Removes cached descriptor if any for the given entity.
     */
    public void removeDescriptor(String entityName) {
        descriptors.remove(entityName);
    }

    /**
     * Caches descriptor definition.
     */
    public void addDescriptor(String entityName, ClassDescriptor descriptor) {
        if (descriptor == null) {
            removeDescriptor(entityName);
        } else {
            descriptors.put(entityName, descriptor);
        }
    }

    public ClassDescriptor getDescriptor(String entityName) {
        if (entityName == null) {
            throw new NullPointerException("Null 'entityName'");
        }

        ClassDescriptor cached = descriptors.get(entityName);
        if (cached != null) {
            return cached;
        }

        return createProxyDescriptor(entityName);
    }

    /**
     * Creates a descriptor wrapper that will compile the underlying descriptor
     * on demand. Using proxy indirection is needed to compile relationships of
     * descriptors to other descriptors that are not compiled yet.
     */
    protected ClassDescriptor createProxyDescriptor(String entityName) {
        ClassDescriptor descriptor = new LazyClassDescriptorDecorator(this, entityName);
        addDescriptor(entityName, descriptor);
        return descriptor;
    }

    /**
     * Creates a new descriptor.
     */
    protected ClassDescriptor createDescriptor(String entityName) {

        // scan the factory chain until some factory returns a non-null
        // descriptor;
        // scanning is done in reverse order so that the factories added last
        // take higher
        // precedence...
        ListIterator<ClassDescriptorFactory> it = factories.listIterator(factories.size());
        while (it.hasPrevious()) {
            ClassDescriptorFactory factory = it.previous();
            ClassDescriptor descriptor = factory.getDescriptor(entityName);

            if (descriptor != null) {
                return descriptor;
            }
        }

        throw new CayenneRuntimeException("Failed to create descriptor for entity: %s", entityName);
    }
}
