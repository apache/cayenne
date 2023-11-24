/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.dba.sqlserver;

import org.apache.cayenne.configuration.runtime.DbAdapterDetector;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Detects SQLServer database from JDBC metadata.
 *
 * @since 1.2
 */
public class SQLServerSniffer implements DbAdapterDetector {

    protected AdhocObjectFactory objectFactory;

    public SQLServerSniffer(@Inject AdhocObjectFactory objectFactory) {
        this.objectFactory = objectFactory;
    }

    @Override
    public DbAdapter createAdapter(DatabaseMetaData md) throws SQLException {
        String dbName = md.getDatabaseProductName();
        if (dbName == null || !dbName.toUpperCase().contains("MICROSOFT SQL SERVER")) {
            return null;
        }

        SQLServerAdapter adapter = objectFactory.newInstance(
                SQLServerAdapter.class,
                SQLServerAdapter.class.getName());
        adapter.setVersion(md.getDatabaseMajorVersion());

        // detect whether generated keys are supported
        boolean generatedKeys = false;

        try {
            generatedKeys = md.supportsGetGeneratedKeys();
        } catch (Throwable th) {
            // catch exceptions resulting from incomplete JDBC3 implementation
            // ** we have to catch Throwable, as unimplemented methods would result in
            // "AbstractMethodError".
        }
        adapter.setSupportsGeneratedKeys(generatedKeys);
        return adapter;
    }
}
