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

package org.apache.cayenne.dba.sybase;

import org.apache.cayenne.configuration.server.DbAdapterDetector;
import org.apache.cayenne.configuration.server.PkGeneratorFactoryProvider;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Detects Sybase database from JDBC metadata.
 *
 * @since 1.2
 */
public class SybaseSniffer implements DbAdapterDetector {

    protected AdhocObjectFactory objectFactory;

    protected PkGeneratorFactoryProvider pkGeneratorProvider;

    public SybaseSniffer(@Inject AdhocObjectFactory objectFactory,
                         @Inject PkGeneratorFactoryProvider pkGeneratorProvider) {
        this.objectFactory = objectFactory;
        this.pkGeneratorProvider = Objects.requireNonNull(pkGeneratorProvider, "Null pkGeneratorProvider");
    }

    @Override
    public DbAdapter createAdapter(DatabaseMetaData md) throws SQLException {
        // JTDS driver returns "sql server" for Sybase, so need to handle it differently
        String driver = md.getDriverName();
        JdbcAdapter adapter;
        if (driver != null && driver.toLowerCase().startsWith("jtds")) {
            String url = md.getURL();
            adapter = url != null && url.toLowerCase().startsWith("jdbc:jtds:sybase:")
                    ? objectFactory.newInstance(DbAdapter.class, SybaseAdapter.class.getName()) : null;
        } else {
            String dbName = md.getDatabaseProductName();
            adapter = dbName != null && dbName.toUpperCase().contains("ADAPTIVE SERVER")
                    ? objectFactory.newInstance(DbAdapter.class, SybaseAdapter.class.getName()) : null;
        }
        
        if (adapter != null) {
            PkGenerator pkGenerator = pkGeneratorProvider.get(adapter);

            if (pkGenerator != null) {
                adapter.setPkGenerator(pkGenerator);
            }
            return adapter;
        } else {
            return null;
        }
    }
}
