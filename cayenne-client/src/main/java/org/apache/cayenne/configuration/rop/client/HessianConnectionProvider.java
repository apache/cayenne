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
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.remote.ClientConnection;
import org.apache.cayenne.remote.hessian.HessianConnection;

public class HessianConnectionProvider implements Provider<ClientConnection> {

    @Inject
    protected RuntimeProperties runtimeProperties;

    public ClientConnection get() throws ConfigurationException {

        String url = runtimeProperties.get(Constants.ROP_SERVICE_URL_PROPERTY);
        if (url == null) {
            throw new ConfigurationException(
                    "No property defined for '%s', can't initialize HessianConnection",
                    Constants.ROP_SERVICE_URL_PROPERTY);
        }

        String userName = runtimeProperties.get(Constants.ROP_SERVICE_USERNAME_PROPERTY);
        String password = runtimeProperties.get(Constants.ROP_SERVICE_PASSWORD_PROPERTY);
        String sharedSession = runtimeProperties
                .get(Constants.ROP_SERVICE_SHARED_SESSION_PROPERTY);
        long readTimeout = runtimeProperties.getLong(
                Constants.ROP_SERVICE_TIMEOUT_PROPERTY,
                -1l);

        HessianConnection result = new HessianConnection(
                url,
                userName,
                password,
                sharedSession);

        if (readTimeout > 0) {
            result.setReadTimeout(readTimeout);
        }

        return result;
    }
}
