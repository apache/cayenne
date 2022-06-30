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

package org.apache.cayenne.dbsync;

import org.apache.cayenne.dba.db2.DB2Adapter;
import org.apache.cayenne.dba.derby.DerbyAdapter;
import org.apache.cayenne.dba.firebird.FirebirdAdapter;
import org.apache.cayenne.dba.h2.H2Adapter;
import org.apache.cayenne.dba.hsqldb.HSQLDBAdapter;
import org.apache.cayenne.dba.ingres.IngresAdapter;
import org.apache.cayenne.dba.mysql.MySQLAdapter;
import org.apache.cayenne.dba.oracle.Oracle8Adapter;
import org.apache.cayenne.dba.oracle.OracleAdapter;
import org.apache.cayenne.dba.postgres.PostgresAdapter;
import org.apache.cayenne.dba.sqlserver.SQLServerAdapter;
import org.apache.cayenne.dba.sybase.SybaseAdapter;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactoryProvider;
import org.apache.cayenne.dbsync.merge.factory.DB2MergerTokenFactory;
import org.apache.cayenne.dbsync.merge.factory.DefaultMergerTokenFactory;
import org.apache.cayenne.dbsync.merge.factory.DerbyMergerTokenFactory;
import org.apache.cayenne.dbsync.merge.factory.FirebirdMergerTokenFactory;
import org.apache.cayenne.dbsync.merge.factory.H2MergerTokenFactory;
import org.apache.cayenne.dbsync.merge.factory.HSQLMergerTokenFactory;
import org.apache.cayenne.dbsync.merge.factory.IngresMergerTokenFactory;
import org.apache.cayenne.dbsync.merge.factory.MergerTokenFactory;
import org.apache.cayenne.dbsync.merge.factory.MySQLMergerTokenFactory;
import org.apache.cayenne.dbsync.merge.factory.OracleMergerTokenFactory;
import org.apache.cayenne.dbsync.merge.factory.PostgresMergerTokenFactory;
import org.apache.cayenne.dbsync.merge.factory.SQLServerMergerTokenFactory;
import org.apache.cayenne.dbsync.merge.factory.SybaseMergerTokenFactory;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.MapBuilder;
import org.apache.cayenne.di.Module;

/**
 * @since 4.0
 */
public class DbSyncModule implements Module {

    /**
     * A DI container key for the Map&lt;String, String&gt; storing properties
     * used by built-in Cayenne service.
     */
    public static final String MERGER_FACTORIES_MAP = "cayenne.dbsync.mergerfactories";

    public static MapBuilder<MergerTokenFactory> contributeMergerTokenFactories(Binder binder) {
        return binder.bindMap(MergerTokenFactory.class, MERGER_FACTORIES_MAP);
    }

    @Override
    public void configure(Binder binder) {

        // default and per adapter merger factories...
        binder.bind(MergerTokenFactory.class).to(DefaultMergerTokenFactory.class);
        contributeMergerTokenFactories(binder)
                .put(DB2Adapter.class.getName(), DB2MergerTokenFactory.class)
                .put(DerbyAdapter.class.getName(), DerbyMergerTokenFactory.class)
                .put(FirebirdAdapter.class.getName(), FirebirdMergerTokenFactory.class)
                .put(H2Adapter.class.getName(), H2MergerTokenFactory.class)
                .put(HSQLDBAdapter.class.getName(), HSQLMergerTokenFactory.class)
                .put(IngresAdapter.class.getName(), IngresMergerTokenFactory.class)
                .put(MySQLAdapter.class.getName(), MySQLMergerTokenFactory.class)
                .put(OracleAdapter.class.getName(), OracleMergerTokenFactory.class)
                .put(Oracle8Adapter.class.getName(), OracleMergerTokenFactory.class)
                .put(PostgresAdapter.class.getName(), PostgresMergerTokenFactory.class)
                .put(SQLServerAdapter.class.getName(), SQLServerMergerTokenFactory.class)
                .put(SybaseAdapter.class.getName(), SybaseMergerTokenFactory.class);

        binder.bind(MergerTokenFactoryProvider.class).to(MergerTokenFactoryProvider.class);

    }
}
