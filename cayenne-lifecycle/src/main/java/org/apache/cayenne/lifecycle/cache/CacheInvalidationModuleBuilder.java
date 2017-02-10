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

import java.util.Collection;
import java.util.HashSet;

import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.tx.TransactionFilter;

/**
 * @since 4.0
 */
public class CacheInvalidationModuleBuilder {

    public static final String INVALIDATION_HANDLERS_LIST = "cayenne.querycache.invalidation_handlers";

    private Collection<Class<? extends InvalidationHandler>> handlerTypes;

    private Collection<InvalidationHandler> handlerInstances;

    public static CacheInvalidationModuleBuilder builder() {
        return new CacheInvalidationModuleBuilder();
    }

    private static ListBuilder<InvalidationHandler> contributeInvalidationHandler(Binder binder) {
        return binder.bindList(INVALIDATION_HANDLERS_LIST);
    }

    CacheInvalidationModuleBuilder() {
        this.handlerTypes = new HashSet<>();
        this.handlerInstances = new HashSet<>();
    }

    public CacheInvalidationModuleBuilder invalidationHandler(Class<? extends InvalidationHandler> handlerType) {
        handlerTypes.add(handlerType);
        return this;
    }

    public CacheInvalidationModuleBuilder invalidationHandler(InvalidationHandler handlerInstance) {
        handlerInstances.add(handlerInstance);
        return this;
    }

    public Module build() {
        return new Module() {
            @Override
            public void configure(Binder binder) {
                ListBuilder<InvalidationHandler> handlers = contributeInvalidationHandler(binder);

                handlers.add(CacheGroupsHandler.class);
                handlers.addAll(handlerInstances);

                for(Class<? extends InvalidationHandler> handlerType : handlerTypes) {
                    handlers.add(handlerType);
                }

                // want the filter to be INSIDE transaction
                binder.bindList(Constants.SERVER_DOMAIN_FILTERS_LIST)
                        .add(CacheInvalidationFilter.class).before(TransactionFilter.class);
            }
        };
    }
}
