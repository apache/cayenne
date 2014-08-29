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

package org.apache.cayenne.dba.postgres;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.apache.cayenne.configuration.server.DbAdapterDetector;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;

/**
 * @since 1.2
 */
public class PostgresSniffer implements DbAdapterDetector {

    protected AdhocObjectFactory objectFactory;

    public PostgresSniffer(@Inject AdhocObjectFactory objectFactory) {
        this.objectFactory = objectFactory;
    }

    @Override
    public DbAdapter createAdapter(DatabaseMetaData md) throws SQLException {
        String dbName = md.getDatabaseProductName();
        return dbName != null && dbName.toUpperCase().contains("POSTGRESQL")
                ? objectFactory.newInstance(DbAdapter.class, PostgresAdapter.class.getName()) : null;
    }
}
