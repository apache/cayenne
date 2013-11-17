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
package org.apache.cayenne.unit.di.server;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.di.Provider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ServerCaseDataSourceInfoProvider implements Provider<DataSourceInfo> {

    static final String CONNECTION_NAME_KEY = "cayenneTestConnection";
    private static final String DEFAULT_CONNECTION_KEY = "internal_embedded_datasource";

    private static Log logger = LogFactory.getLog(ServerCaseDataSourceInfoProvider.class);

    @Override
    public DataSourceInfo get() throws ConfigurationException {

        String connectionKey = System.getProperty(CONNECTION_NAME_KEY);

        DataSourceInfo connectionInfo = ConnectionProperties.getInstance().getConnectionInfo(connectionKey);

        // attempt default if invalid key is specified
        if (connectionInfo == null) {

            logger.info("Invalid connection key '" + connectionKey + "', trying default: " + DEFAULT_CONNECTION_KEY);

            connectionInfo = ConnectionProperties.getInstance().getConnectionInfo(DEFAULT_CONNECTION_KEY);
        }

        if (connectionInfo == null) {
            throw new RuntimeException("Null connection info for key: " + connectionKey);
        }

        logger.info("loaded connection info: " + connectionInfo);
        return connectionInfo;
    }
}
