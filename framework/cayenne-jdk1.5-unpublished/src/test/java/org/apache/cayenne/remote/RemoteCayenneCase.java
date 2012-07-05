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
package org.apache.cayenne.remote;

import org.apache.cayenne.CayenneContext;
import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.cache.MapQueryCache;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.event.DefaultEventManager;
import org.apache.cayenne.remote.service.LocalConnection;
import org.apache.cayenne.unit.UnitLocalConnection;
import org.apache.cayenne.unit.di.client.ClientCase;

public abstract class RemoteCayenneCase extends ClientCase {

    protected CayenneContext clientContext;

    @Inject
    protected DataContext serverContext;

    /**
     * Used serialization policy. Per CAY-979 we're testing on all policies
     */
    private int serializationPolicy;

    @Override
    public void runBare() throws Throwable {
        serializationPolicy = LocalConnection.HESSIAN_SERIALIZATION;
        runBareSimple();
        serializationPolicy = LocalConnection.JAVA_SERIALIZATION;
        runBareSimple();
        serializationPolicy = LocalConnection.NO_SERIALIZATION;
        runBareSimple();
    }

    protected void runBareSimple() throws Throwable {
        super.runBare();
    }

    @Override
    public void setUpAfterInjection() throws Exception {
        clientContext = createROPContext();
    }

    protected CayenneContext createROPContext() {
        ClientServerChannel clientServerChannel = new ClientServerChannel(serverContext);
        UnitLocalConnection connection = new UnitLocalConnection(
                clientServerChannel,
                serializationPolicy);
        ClientChannel channel = new ClientChannel(connection, false,
        // we want events, but we don't want thread leaks, so creating single threaded EM.
        // TODO: replace with container managed ClientCase.
                new DefaultEventManager(0),
                false);
        CayenneContext context = new CayenneContext(channel, true, true);
        context.setQueryCache(new MapQueryCache(10));
        return context;
    }
}
