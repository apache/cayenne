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
package org.apache.cayenne.unit.di.client;

import org.apache.cayenne.CayenneContext;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.configuration.rop.client.ClientRuntime;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultScope;
import org.apache.cayenne.remote.ClientConnection;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.di.UnitTestLifecycleManager;

public class ClientCaseModule implements Module {

    protected DefaultScope testScope;

    public ClientCaseModule(DefaultScope testScope) {
        this.testScope = testScope;
    }

    public void configure(Binder binder) {

        // singletons

        binder.bind(UnitTestLifecycleManager.class).toInstance(
                new ClientCaseLifecycleManager(testScope));
        binder.bind(Key.get(DataChannelInterceptor.class, ClientCase.ROP_CLIENT_KEY)).to(
                ClientServerDataChannelInterceptor.class);

        // test-scoped objects

        binder.bind(ClientCaseProperties.class).to(ClientCaseProperties.class).in(
                testScope);

        binder.bind(ClientRuntime.class).toProvider(ClientRuntimeProvider.class).in(
                testScope);

        binder.bind(Key.get(ObjectContext.class, ClientCase.ROP_CLIENT_KEY)).toProvider(
                ClientCaseObjectContextProvider.class).in(testScope);
        binder.bind(CayenneContext.class).toProvider(
                ClientCaseCayenneContextProvider.class).in(testScope);

        binder.bind(ClientServerChannel.class).toProvider(
                ClientServerChannelProvider.class).in(testScope);
        binder.bind(ClientConnection.class).toProvider(
                ClientCaseClientConnectionProvider.class).in(testScope);
    }
}
