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
import java.util.Map;

import org.apache.cayenne.property.ClassDescriptor;
import org.apache.cayenne.property.ClassDescriptorFactory;

/**
 * A caching descriptor factory that creates ClassDescriptors based on Cayenne mapping
 * information.
 * <p>
 * <i>Synchronization note: This implementation is NOT synchronized, and requires external
 * synchronization if used in a multi-threaded environment.</i>
 * </p>
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
public class EntityDescriptorFactory implements ClassDescriptorFactory {

    protected EntityResolver resolver;
    protected Map classDescriptors;

    public EntityDescriptorFactory(EntityResolver resolver) {
        this.resolver = resolver;
    }

    public ClassDescriptor getDescriptor(String entityName) {

        if (classDescriptors == null) {
            classDescriptors = new HashMap();
        }
        else {

            ClassDescriptor descriptor = (ClassDescriptor) classDescriptors
                    .get(entityName);
            if (descriptor != null) {
                return descriptor;
            }
        }

        EntityDescriptor entityDescriptor = createDescriptor(entityName);

        if (entityDescriptor == null) {
            return null;
        }

        classDescriptors.put(entityName, entityDescriptor);

        // compile after caching to avoid infinite loops during ArcProperty compilation
        entityDescriptor.compile(resolver);
        return entityDescriptor;
    }

    /**
     * Creates a new descriptor that is not compiled. Compilation is intentionally
     * deferred until after the descriptor is cached, as it triggers creation of related
     * entity descriptors and may result in an endless loop.
     */
    protected EntityDescriptor createDescriptor(String entityName) {
        ObjEntity entity = resolver.getObjEntity(entityName);
        if (entity == null) {
            return null;
        }

        String superEntityName = entity.getSuperEntityName();

        ClassDescriptor superDescriptor = (superEntityName != null) ? resolver
                .getClassDescriptor(superEntityName) : null;

        // return uncompiled
        return new EntityDescriptor(entity, superDescriptor);
    }
}
