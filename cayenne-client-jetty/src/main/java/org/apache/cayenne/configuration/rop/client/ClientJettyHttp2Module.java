/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/

package org.apache.cayenne.configuration.rop.client;

import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.remote.ClientConnection;
import org.apache.cayenne.rop.JettyHttp2ClientConnectionProvider;
import org.apache.cayenne.rop.http.JettyHttpROPConnector;

/**
 * This is HTTP/2 implementation of ROP Client.
 * <p>
 * This module uses {@link JettyHttpROPConnector} initialized by {@link JettyHttp2ClientConnectionProvider}
 * without ALPN by default.
 * <p>
 * In order to use it with ALPN you have to set {@link ClientConstants#ROP_SERVICE_USE_ALPN_PROPERTY} to true
 * and provide the alpn-boot-XXX.jar into the bootstrap classpath.
 */
public class ClientJettyHttp2Module implements Module {

    @Override
    public void configure(Binder binder) {
        binder.bind(ClientConnection.class).toProvider(JettyHttp2ClientConnectionProvider.class);
    }

}
