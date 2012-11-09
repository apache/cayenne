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
package org.apache.cayenne.tools.configuration;

import org.apache.cayenne.access.jdbc.BatchQueryBuilderFactory;
import org.apache.cayenne.access.jdbc.DefaultBatchQueryBuilderFactory;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.DefaultRuntimeProperties;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.configuration.server.DataSourceFactory;
import org.apache.cayenne.configuration.server.DbAdapterFactory;
import org.apache.cayenne.configuration.server.DefaultDbAdapterFactory;
import org.apache.cayenne.dba.db2.DB2Sniffer;
import org.apache.cayenne.dba.derby.DerbySniffer;
import org.apache.cayenne.dba.frontbase.FrontBaseSniffer;
import org.apache.cayenne.dba.h2.H2Sniffer;
import org.apache.cayenne.dba.hsqldb.HSQLDBSniffer;
import org.apache.cayenne.dba.ingres.IngresSniffer;
import org.apache.cayenne.dba.mysql.MySQLSniffer;
import org.apache.cayenne.dba.openbase.OpenBaseSniffer;
import org.apache.cayenne.dba.oracle.OracleSniffer;
import org.apache.cayenne.dba.postgres.PostgresSniffer;
import org.apache.cayenne.dba.sqlite.SQLiteSniffer;
import org.apache.cayenne.dba.sqlserver.SQLServerSniffer;
import org.apache.cayenne.dba.sybase.SybaseSniffer;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;
import org.apache.cayenne.log.CommonsJdbcEventLogger;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.commons.logging.Log;

/**
 * A DI module to bootstrap DI container for Cayenne Ant tasks and Maven
 * plugins.
 * 
 * @since 3.2
 */
public class ToolsModule implements Module {

    private Log logger;

    public ToolsModule(Log logger) {

        if (logger == null) {
            throw new NullPointerException("Null logger");
        }

        this.logger = logger;
    }

    public void configure(Binder binder) {

        binder.bind(Log.class).toInstance(logger);

        // configure empty global stack properties
        binder.bindMap(Constants.PROPERTIES_MAP);

        binder.bindList(Constants.SERVER_DEFAULT_TYPES_LIST);
        binder.bindList(Constants.SERVER_USER_TYPES_LIST);
        binder.bindList(Constants.SERVER_TYPE_FACTORIES_LIST);

        AdhocObjectFactory objectFactory = new DefaultAdhocObjectFactory();
        binder.bind(AdhocObjectFactory.class).toInstance(objectFactory);

        binder.bind(RuntimeProperties.class).to(DefaultRuntimeProperties.class);
        binder.bind(BatchQueryBuilderFactory.class).to(DefaultBatchQueryBuilderFactory.class);
        binder.bind(JdbcEventLogger.class).to(CommonsJdbcEventLogger.class);

        // TODO: this is cloned from ServerModule... figure out how to reuse
        // this list
        binder.bindList(Constants.SERVER_ADAPTER_DETECTORS_LIST).add(new OpenBaseSniffer(objectFactory))
                .add(new FrontBaseSniffer(objectFactory)).add(new IngresSniffer(objectFactory))
                .add(new SQLiteSniffer(objectFactory)).add(new DB2Sniffer(objectFactory))
                .add(new H2Sniffer(objectFactory)).add(new HSQLDBSniffer(objectFactory))
                .add(new SybaseSniffer(objectFactory)).add(new DerbySniffer(objectFactory))
                .add(new SQLServerSniffer(objectFactory)).add(new OracleSniffer(objectFactory))
                .add(new PostgresSniffer(objectFactory)).add(new MySQLSniffer(objectFactory));

        binder.bind(DbAdapterFactory.class).to(DefaultDbAdapterFactory.class);

        binder.bind(DataSourceFactory.class).to(DriverDataSourceFactory.class);

    }

}
