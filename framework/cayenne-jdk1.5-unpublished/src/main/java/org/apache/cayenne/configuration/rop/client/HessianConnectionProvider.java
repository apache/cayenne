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
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.remote.ClientConnection;
import org.apache.cayenne.remote.hessian.HessianConnection;

public class HessianConnectionProvider implements Provider<ClientConnection> {

    @Inject
    protected RuntimeProperties runtimeProperties;

    public ClientConnection get() throws ConfigurationException {

        String url = runtimeProperties.get(ClientModule.ROP_SERVICE_URL);
        if (url == null) {
            throw new ConfigurationException(
                    "No property defined for '%s', can't initialize HessianConnection",
                    ClientModule.ROP_SERVICE_URL);
        }

        String userName = runtimeProperties
                .get(ClientModule.ROP_SERVICE_USER_NAME);
        String password = runtimeProperties.get(ClientModule.ROP_SERVICE_PASSWORD);
        String sharedSession = runtimeProperties
                .get(ClientModule.ROP_SERVICE_SHARED_SESSION);
        String readTimeoutStr = runtimeProperties
                .get(ClientModule.ROP_SERVICE_TIMEOUT);
        
        HessianConnection result = new HessianConnection(url, userName, password, sharedSession);
        
        if (readTimeoutStr != null && readTimeoutStr.length() != 0) {
            result.setReadTimeout(Long.parseLong(readTimeoutStr));
        }

        return result;
    }
}
