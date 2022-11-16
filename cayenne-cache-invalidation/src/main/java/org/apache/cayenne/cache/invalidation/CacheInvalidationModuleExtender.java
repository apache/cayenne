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

package org.apache.cayenne.cache.invalidation;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.ListBuilder;

import java.util.Collection;
import java.util.function.Function;

/**
 * @since 4.0
 */
public class CacheInvalidationModuleExtender {

    private final Binder binder;
    private ListBuilder<InvalidationHandler> invalidationHandlers;

    protected CacheInvalidationModuleExtender(Binder binder) {
        this.binder = binder;
    }

    protected CacheInvalidationModuleExtender initAllExtensions() {
        contributeInvalidationHandlers();
        return this;
    }

    /**
     * Disable the default {@link CacheGroupsHandler} that is tied to {@link CacheGroups} and {@link CacheGroup}
     * annotations.
     */
    public CacheInvalidationModuleExtender noCacheGroupsHandler() {

        // replace CacheGroupsHandler with a dummy no op handler
        binder.bind(CacheGroupsHandler.class).toInstance(new CacheGroupsHandler() {
            @Override
            public Function<Persistent, Collection<CacheGroupDescriptor>> canHandle(Class<? extends Persistent> type) {
                return null;
            }
        });

        return this;
    }

    public CacheInvalidationModuleExtender addHandler(Class<? extends InvalidationHandler> handlerType) {
        contributeInvalidationHandlers().add(handlerType);
        return this;
    }

    public CacheInvalidationModuleExtender addHandler(InvalidationHandler handlerInstance) {
        contributeInvalidationHandlers().add(handlerInstance);
        return this;
    }

    private ListBuilder<InvalidationHandler> contributeInvalidationHandlers() {
        if (invalidationHandlers == null) {
            invalidationHandlers = binder.bindList(InvalidationHandler.class);
        }
        return invalidationHandlers;
    }

}
