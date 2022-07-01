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
package org.apache.cayenne.unit.di.server;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.configuration.server.PkGeneratorFactoryProvider;
import org.apache.cayenne.conn.DataSourceInfo;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;

public class ServerCaseDbAdapterProvider implements Provider<JdbcAdapter> {

    private DataSourceInfo dataSourceInfo;
    private AdhocObjectFactory objectFactory;

    private PkGeneratorFactoryProvider pkGeneratorProvider;

    public ServerCaseDbAdapterProvider(
            @Inject DataSourceInfo dataSourceInfo,
            @Inject AdhocObjectFactory objectFactory,
            @Inject PkGeneratorFactoryProvider pkGeneratorProvider) {
        this.dataSourceInfo = dataSourceInfo;
        this.objectFactory = objectFactory;
        this.pkGeneratorProvider = pkGeneratorProvider;
    }

    public JdbcAdapter get() throws ConfigurationException {
        JdbcAdapter jdbcAdapter = objectFactory.newInstance(DbAdapter.class, dataSourceInfo.getAdapterClassName());
        PkGenerator pkGenerator = pkGeneratorProvider.get(jdbcAdapter);
        jdbcAdapter.setPkGenerator(pkGenerator);
        pkGenerator.setAdapter(jdbcAdapter);
        return jdbcAdapter;
    }
}
