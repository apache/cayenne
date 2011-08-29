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

import java.util.Map;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.cache.MapQueryCacheProvider;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.configuration.DefaultRuntimeProperties;
import org.apache.cayenne.configuration.ObjectContextFactory;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.event.DefaultEventManager;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.remote.ClientConnection;

/**
 * A DI module containing all Cayenne ROP client runtime configurations.
 * 
 * @since 3.1
 */
public class ClientModule implements Module {

    public static final String ROP_SERVICE_URL = "cayenne.config.rop.service.url";
    public static final String ROP_SERVICE_USER_NAME = "cayenne.config.rop.service.username";
    public static final String ROP_SERVICE_PASSWORD = "cayenne.config.rop.service.password";
    public static final String ROP_SERVICE_SHARED_SESSION = "cayenne.config.rop.service.shared_session";

    public static final String CHANNEL_EVENTS = "cayenne.config.rop.client.channel.events";
    
    // TODO: this property name is exactly the same as CHANNEL_EVENTS... Seems messed up
    public static final String CHANNEL_REMOTE_EVENTS_OPTIONAL = "cayenne.config.rop.client.channel.events";

    public static final String CONTEXT_CHANGE_EVENTS = "cayenne.config.rop.client.context.change_events";
    public static final String CONTEXT_LIFECYCLE_EVENTS = "cayenne.config.rop.client.context.lifecycle_events";

    protected Map<String, String> properties;

    public ClientModule(Map<String, String> properties) {
        if (properties == null) {
            throw new NullPointerException("Null 'properties' map");
        }

        this.properties = properties;
    }

    public void configure(Binder binder) {

        // expose this module properties to DefaultRuntimeProperties
        binder.<String> bindMap(DefaultRuntimeProperties.PROPERTIES_MAP).putAll(
                properties);

        binder.bind(ObjectContextFactory.class).to(CayenneContextFactory.class);
        binder.bind(ClientConnection.class).toProvider(HessianConnectionProvider.class);
        binder.bind(EventManager.class).to(DefaultEventManager.class);
        binder.bind(RuntimeProperties.class).to(DefaultRuntimeProperties.class);
        binder.bind(DataChannel.class).toProvider(ClientChannelProvider.class);
        binder.bind(QueryCache.class).toProvider(MapQueryCacheProvider.class);
    }

}
