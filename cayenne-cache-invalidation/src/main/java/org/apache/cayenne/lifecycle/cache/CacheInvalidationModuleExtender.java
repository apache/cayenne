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

package org.apache.cayenne.lifecycle.cache;

import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.Module;

import java.util.Collection;
import java.util.HashSet;

/**
 * @since 4.0
 */
public class CacheInvalidationModuleExtender {

    private Collection<Class<? extends InvalidationHandler>> handlerTypes;

    private Collection<InvalidationHandler> handlerInstances;

    CacheInvalidationModuleExtender() {
        this.handlerTypes = new HashSet<>();
        this.handlerInstances = new HashSet<>();
    }

    /**
     * Adds {@link CacheGroupsHandler} that will setup invalidation based on {@link CacheGroups} and {@link CacheGroup}
     * annotations.
     */
    public CacheInvalidationModuleExtender addCacheGroupsHandler() {
        return addInvalidationHandler(CacheGroupsHandler.class);
    }

    public CacheInvalidationModuleExtender addInvalidationHandler(Class<? extends InvalidationHandler> handlerType) {
        handlerTypes.add(handlerType);
        return this;
    }

    public CacheInvalidationModuleExtender addInvalidationHandler(InvalidationHandler handlerInstance) {
        handlerInstances.add(handlerInstance);
        return this;
    }

    public Module module() {
        return new Module() {
            @Override
            public void configure(Binder binder) {
                ListBuilder<InvalidationHandler> handlers = CacheInvalidationModule.contributeInvalidationHandler(binder);

                handlers.addAll(handlerInstances);

                for (Class<? extends InvalidationHandler> handlerType : handlerTypes) {
                    handlers.add(handlerType);
                }
            }
        };
    }
}
