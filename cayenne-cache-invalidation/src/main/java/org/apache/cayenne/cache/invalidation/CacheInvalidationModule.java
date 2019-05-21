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

import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.tx.TransactionFilter;

/**
 * This module is autoloaded, all extensions should be done via {@link CacheInvalidationModuleExtender}.
 *
 * @since 4.0
 */
public class CacheInvalidationModule implements Module {

    static ListBuilder<InvalidationHandler> contributeInvalidationHandler(Binder binder) {
        return binder.bindList(InvalidationHandler.class);
    }

    /**
     * Returns a new "extender" to customize the defaults provided by this module.
     *
     * @return a new "extender" to customize the defaults provided by this module.
     */
    public static CacheInvalidationModuleExtender extend() {
        return new CacheInvalidationModuleExtender();
    }

    @Override
    public void configure(Binder binder) {

        binder.bind(CacheGroupsHandler.class).to(CacheGroupsHandler.class);
        contributeInvalidationHandler(binder).add(CacheGroupsHandler.class);

        // want the filter to be INSIDE transaction by default
        ServerModule.contributeDomainSyncFilters(binder)
                .insertBefore(CacheInvalidationFilter.class, TransactionFilter.class);
    }
}
