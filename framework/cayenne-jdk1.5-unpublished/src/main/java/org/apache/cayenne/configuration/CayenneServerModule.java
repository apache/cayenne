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
package org.apache.cayenne.configuration;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategy;
import org.apache.cayenne.access.dbsync.SkipSchemaUpdateStrategy;
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
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.Scopes;
import org.apache.cayenne.resource.ClassLoaderResourceLocator;
import org.apache.cayenne.resource.ResourceLocator;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.runtime.DataContextProvider;
import org.apache.cayenne.runtime.DataDomainProvider;

/**
 * A DI module containing all Cayenne server runtime configurations. To customize Cayenne
 * runtime configuration, either extend this module, or supply an extra custom module when
 * creating {@link CayenneRuntime}.
 * 
 * @since 3.1
 */
public class CayenneServerModule implements Module {

    protected String runtimeName;

    public CayenneServerModule(String runtimeName) {
        this.runtimeName = runtimeName;
    }

    public void configure(Binder binder) {

        // configure global stack properties
        binder.bindMap(RuntimeProperties.class).put(
                RuntimeProperties.CAYENNE_RUNTIME_NAME,
                runtimeName);

        // configure known DbAdapter detectors in reverse order of popularity. Users can
        // add their own to install custom adapters automatically
        binder
                .bindList(DbAdapterFactory.class)
                .add(new OpenBaseSniffer())
                .add(new FrontBaseSniffer())
                .add(new IngresSniffer())
                .add(new SQLiteSniffer())
                .add(new DB2Sniffer())
                .add(new H2Sniffer())
                .add(new HSQLDBSniffer())
                .add(new SybaseSniffer())
                .add(new DerbySniffer())
                .add(new SQLServerSniffer())
                .add(new OracleSniffer())
                .add(new PostgresSniffer())
                .add(new MySQLSniffer());

        binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class).in(
                Scopes.SINGLETON);
        binder.bind(ConfigurationNameMapper.class).to(
                DefaultConfigurationNameMapper.class).in(Scopes.SINGLETON);

        // a service to provide the main stack DataDomain
        binder.bind(DataDomain.class).toProvider(DataDomainProvider.class).in(
                Scopes.SINGLETON);

        // will return DataDomain for request for a DataChannel
        binder.bind(DataChannel.class).toProvider(DomainDataChannelProvider.class).in(
                Scopes.SINGLETON);

        binder.bind(ObjectContext.class).toProvider(DataContextProvider.class);

        // a service to load project XML descriptors
        binder.bind(DataChannelDescriptorLoader.class).to(
                XMLDataChannelDescriptorLoader.class).in(Scopes.SINGLETON);

        // a service to load DataMap XML descriptors
        binder.bind(DataMapLoader.class).to(XMLDataMapLoader.class).in(Scopes.SINGLETON);

        // a locator of resources, such as XML descriptors
        binder.bind(ResourceLocator.class).to(ClassLoaderResourceLocator.class).in(
                Scopes.SINGLETON);

        // a global properties object
        binder.bind(RuntimeProperties.class).to(DefaultRuntimeProperties.class).in(
                Scopes.SINGLETON);

        // a service to load DataSourceFactories
        binder.bind(DataSourceFactoryLoader.class).to(
                DefaultDataSourceFactoryLoader.class).in(Scopes.SINGLETON);

        // a default SchemaUpdateStrategy (used when no explicit strategy is specified in
        // XML)
        binder.bind(SchemaUpdateStrategy.class).to(SkipSchemaUpdateStrategy.class).in(
                Scopes.SINGLETON);

        // a default DBAdapterFactory used to load custom and automatic DbAdapters
        binder.bind(DbAdapterFactory.class).to(DefaultDbAdapterFactory.class);
    }
}
