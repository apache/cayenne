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
package org.apache.cayenne.configuration.rop.server;

import java.util.Map;

import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.MapBuilder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.remote.RemoteService;
import org.apache.cayenne.remote.hessian.service.HessianService;

/**
 * A DI module that defines services for the server-side of an ROP application based on
 * Caucho Hessian.
 * 
 * @since 3.1
 */
public class ROPServerModule implements Module {

    protected Map<String, String> eventBridgeProperties;

    public ROPServerModule(Map<String, String> eventBridgeProperties) {
        this.eventBridgeProperties = eventBridgeProperties;
    }

    public void configure(Binder binder) {

        MapBuilder<String> mapBuilder = binder
                .bindMap(Constants.SERVER_ROP_EVENT_BRIDGE_PROPERTIES_MAP);
        mapBuilder.putAll(eventBridgeProperties);

        binder.bind(RemoteService.class).to(HessianService.class);
    }

}
