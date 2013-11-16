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
package org.apache.cayenne.configuration.rop.client;

import org.apache.cayenne.CayenneContext;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.cache.NestedQueryCache;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.ObjectContextFactory;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Injector;

public class CayenneContextFactory implements ObjectContextFactory {

    @Inject
    protected DataChannel dataChannel;

    @Inject
    protected RuntimeProperties properties;
    
    @Inject
    protected QueryCache queryCache;

    @Inject
    protected Injector injector;

    public ObjectContext createContext() {
        return createContext(dataChannel);
    }

    public ObjectContext createContext(DataChannel parent) {
        boolean changeEvents = properties.getBoolean(
                Constants.ROP_CONTEXT_CHANGE_EVENTS_PROPERTY,
                false);

        boolean lifecycleEvents = properties.getBoolean(
                Constants.ROP_CONTEXT_LIFECYCLE_EVENTS_PROPERTY,
                false);

        CayenneContext context = newInstance(parent, changeEvents, lifecycleEvents);
        context.setQueryCache(new NestedQueryCache(queryCache));
        return context;
    }
    
    protected CayenneContext newInstance(DataChannel parent, boolean changeEventsEnabled, boolean lifecycleEventsEnabled) {
        return new CayenneContext(parent, changeEventsEnabled, lifecycleEventsEnabled);
    }
}
