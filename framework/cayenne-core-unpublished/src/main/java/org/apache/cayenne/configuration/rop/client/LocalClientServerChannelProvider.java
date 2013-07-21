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

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.configuration.ObjectContextFactory;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.remote.service.LocalConnection;

/**
 * Creates a {@link ClientServerChannel} for the {@link LocalConnection}.
 * 
 * @since 3.1
 */
public class LocalClientServerChannelProvider implements Provider<DataChannel> {

    protected Injector serverInjector;

    public LocalClientServerChannelProvider(Injector serverInjector) {
        this.serverInjector = serverInjector;
    }

    public DataChannel get() throws ConfigurationException {
        ObjectContextFactory factory = serverInjector
                .getInstance(ObjectContextFactory.class);

        // TODO: ugly cast
        DataContext serverContext = (DataContext) factory.createContext();
        return new ClientServerChannel(serverContext);
    }
}
