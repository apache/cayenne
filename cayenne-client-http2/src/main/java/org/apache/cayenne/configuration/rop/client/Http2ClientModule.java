/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.cayenne.rop.HighHttp2ALPNClientConnectionProvider;
import org.apache.cayenne.rop.HighHttp2ClientConnectionProvider;
import org.apache.cayenne.rop.LowHttp2ClientConnectionProvider;
import org.apache.cayenne.rop.http2.HighHttp2ROPConnector;

/**
 * This module uses {@link HighHttp2ROPConnector} through {@link HighHttp2ClientConnectionProvider}
 * without ALPN by default.
 * <p>
 * If you want to use other implementations with ALPN or with low-level API, see
 * {@link HighHttp2ALPNClientConnectionProvider} or {@link LowHttp2ClientConnectionProvider} accordingly.
 */
public class Http2ClientModule implements Module {

    public Http2ClientModule() {
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(ClientConnection.class).toProvider(HighHttp2ClientConnectionProvider.class);
    }
}
