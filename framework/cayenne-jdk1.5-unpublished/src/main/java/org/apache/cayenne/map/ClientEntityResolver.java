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

import org.apache.cayenne.reflect.ClassDescriptorFactory;
import org.apache.cayenne.reflect.ClassDescriptorMap;
import org.apache.cayenne.reflect.valueholder.ValueHolderDescriptorFactory;

/**
 * An EntityResolver subclass that uses a different default {@link ClassDescriptorFactory}
 * that handles ValueHolder to-one relationships.
 * 
 * @since 3.0
 */
class ClientEntityResolver extends EntityResolver {

    ClientEntityResolver() {
    }

    @Override
    public EntityResolver getClientEntityResolver() {
        return this;
    }

    @Override
    public ClassDescriptorMap getClassDescriptorMap() {

        if (classDescriptorMap == null) {
            ClassDescriptorMap classDescriptorMap = new ClassDescriptorMap(this);

            classDescriptorMap.addFactory(new ValueHolderDescriptorFactory(
                    classDescriptorMap));

            // since ClassDescriptorMap is not synchronized, we need to prefill it with
            // entity proxies here.
            for (DataMap map : maps) {
                for (String entityName : map.getObjEntityMap().keySet()) {
                    classDescriptorMap.getDescriptor(entityName);
                }
            }

            this.classDescriptorMap = classDescriptorMap;
        }

        return classDescriptorMap;

    }
}
