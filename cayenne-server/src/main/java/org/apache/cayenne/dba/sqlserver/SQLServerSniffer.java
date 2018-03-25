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

package org.apache.cayenne.dba.sqlserver;

import org.apache.cayenne.configuration.server.DbAdapterDetector;
import org.apache.cayenne.configuration.server.PkGeneratorFactoryProvider;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Detects SQLServer database from JDBC metadata.
 *
 * @since 1.2
 */
public class SQLServerSniffer implements DbAdapterDetector {

    protected AdhocObjectFactory objectFactory;

    protected PkGeneratorFactoryProvider pkGeneratorProvider;

    public SQLServerSniffer(@Inject AdhocObjectFactory objectFactory,
                            @Inject PkGeneratorFactoryProvider pkGeneratorProvider) {
        this.objectFactory = objectFactory;
        this.pkGeneratorProvider = Objects.requireNonNull(pkGeneratorProvider, () -> "Null pkGeneratorProvider");
    }

    @Override
    public DbAdapter createAdapter(DatabaseMetaData md) throws SQLException {

        SQLServerAdapter adapter = objectFactory.newInstance(
                SQLServerAdapter.class,
                SQLServerAdapter.class.getName());

        // detect whether generated keys are supported

        boolean generatedKeys = false;
        PkGenerator pkGenerator = null;

        try {
            generatedKeys = md.supportsGetGeneratedKeys();
            if (generatedKeys) {
                pkGenerator = pkGeneratorProvider.get(adapter);

            }
        } catch (Throwable th) {
            // catch exceptions resulting from incomplete JDBC3 implementation
            // ** we have to catch Throwable, as unimplemented methods would result in
            // "AbstractMethodError".
        }
        adapter.setSupportsGeneratedKeys(generatedKeys);
        if (pkGenerator != null) {
            adapter.setPkGenerator(pkGenerator);
        }

        return adapter;
    }
}
