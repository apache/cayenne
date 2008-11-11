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

package org.apache.cayenne.access;

import org.apache.cayenne.conn.ConnectionEventLoggingDelegate;
import org.apache.cayenne.conn.DataSourceInfo;

/**
 * Adapts {@link org.apache.cayenne.access.QueryLogger} to be used as a
 * {@link org.apache.cayenne.conn.ConnectionEventLoggingDelegate} with Cayenne
 * connection pools.
 * 
 * @since 1.2
 */
public class ConnectionLogger implements ConnectionEventLoggingDelegate {

    public void logConnect(String url, String userName, String password) {
        QueryLogger.logConnect(url, userName, password);
    }

    public void logConnectFailure(Throwable th) {
        QueryLogger.logConnectFailure(th);
    }

    public void logConnectSuccess() {
        QueryLogger.logConnectSuccess();
    }

    public void logPoolCreated(DataSourceInfo info) {
        QueryLogger.logPoolCreated(info);
    }
}
