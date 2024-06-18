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
package org.apache.cayenne.configuration.runtime;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.DataRowStoreFactory;
import org.apache.cayenne.access.DefaultDataRowStoreFactory;
import org.apache.cayenne.access.dbsync.DefaultSchemaUpdateStrategyFactory;
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategyFactory;
import org.apache.cayenne.access.dbsync.SkipSchemaUpdateStrategy;
import org.apache.cayenne.access.dbsync.ThrowOnPartialOrCreateSchemaStrategy;
import org.apache.cayenne.access.jdbc.SQLTemplateProcessor;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.access.translator.batch.BatchTranslatorFactory;
import org.apache.cayenne.access.translator.batch.DefaultBatchTranslatorFactory;
import org.apache.cayenne.access.translator.select.DefaultSelectTranslatorFactory;
import org.apache.cayenne.access.translator.select.SelectTranslatorFactory;
import org.apache.cayenne.access.types.DefaultValueObjectTypeRegistry;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.annotation.PostLoad;
import org.apache.cayenne.ashwood.AshwoodEntitySorter;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.ConfigurationTree;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataChannelDescriptorLoader;
import org.apache.cayenne.configuration.DataChannelDescriptorMerger;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.DefaultConfigurationNameMapper;
import org.apache.cayenne.configuration.DefaultDataChannelDescriptorMerger;
import org.apache.cayenne.configuration.DefaultRuntimeProperties;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.configuration.mock.MockDataSourceFactory;
import org.apache.cayenne.dba.JdbcPkGenerator;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.dba.db2.DB2Adapter;
import org.apache.cayenne.dba.db2.DB2PkGenerator;
import org.apache.cayenne.dba.db2.DB2Sniffer;
import org.apache.cayenne.dba.derby.DerbyAdapter;
import org.apache.cayenne.dba.derby.DerbyPkGenerator;
import org.apache.cayenne.dba.derby.DerbySniffer;
import org.apache.cayenne.dba.firebird.FirebirdSniffer;
import org.apache.cayenne.dba.frontbase.FrontBaseAdapter;
import org.apache.cayenne.dba.frontbase.FrontBasePkGenerator;
import org.apache.cayenne.dba.frontbase.FrontBaseSniffer;
import org.apache.cayenne.dba.h2.H2Adapter;
import org.apache.cayenne.dba.h2.H2PkGenerator;
import org.apache.cayenne.dba.h2.H2Sniffer;
import org.apache.cayenne.dba.hsqldb.HSQLDBSniffer;
import org.apache.cayenne.dba.ingres.IngresAdapter;
import org.apache.cayenne.dba.ingres.IngresPkGenerator;
import org.apache.cayenne.dba.ingres.IngresSniffer;
import org.apache.cayenne.dba.mariadb.MariaDBSniffer;
import org.apache.cayenne.dba.mysql.MySQLAdapter;
import org.apache.cayenne.dba.mysql.MySQLPkGenerator;
import org.apache.cayenne.dba.mysql.MySQLSniffer;
import org.apache.cayenne.dba.oracle.Oracle8Adapter;
import org.apache.cayenne.dba.oracle.OracleAdapter;
import org.apache.cayenne.dba.oracle.OraclePkGenerator;
import org.apache.cayenne.dba.oracle.OracleSniffer;
import org.apache.cayenne.dba.postgres.PostgresAdapter;
import org.apache.cayenne.dba.postgres.PostgresPkGenerator;
import org.apache.cayenne.dba.postgres.PostgresSniffer;
import org.apache.cayenne.dba.sqlite.SQLiteSniffer;
import org.apache.cayenne.dba.sqlserver.SQLServerAdapter;
import org.apache.cayenne.dba.sqlserver.SQLServerSniffer;
import org.apache.cayenne.dba.sybase.SybaseAdapter;
import org.apache.cayenne.dba.sybase.SybasePkGenerator;
import org.apache.cayenne.dba.sybase.SybaseSniffer;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.ClassLoaderManager;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;
import org.apache.cayenne.di.spi.DefaultClassLoaderManager;
import org.apache.cayenne.event.EventBridge;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.event.MockEventManager;
import org.apache.cayenne.event.NoopEventBridgeProvider;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.log.Slf4jJdbcEventLogger;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntitySorter;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.reflect.generic.DefaultValueComparisonStrategyFactory;
import org.apache.cayenne.reflect.generic.ValueComparisonStrategyFactory;
import org.apache.cayenne.resource.ClassLoaderResourceLocator;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.ResourceLocator;
import org.apache.cayenne.resource.mock.MockResource;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DataDomainProviderTest {

    @Test
    public void testGet() {

        // create dependencies
        final String testConfigName = "testConfig";
        final DataChannelDescriptor testDescriptor = new DataChannelDescriptor();

        DataMap map1 = new DataMap("map1");
        testDescriptor.getDataMaps().add(map1);

        DataMap map2 = new DataMap("map2");
        testDescriptor.getDataMaps().add(map2);

        DataNodeDescriptor nodeDescriptor1 = new DataNodeDescriptor();
        nodeDescriptor1.setName("node1");
        nodeDescriptor1.getDataMapNames().add("map1");
        nodeDescriptor1.setAdapterType(OracleAdapter.class.getName());
        nodeDescriptor1.setDataSourceFactoryType(MockDataSourceFactory.class.getName());
        nodeDescriptor1.setParameters("jdbc/testDataNode1");
        nodeDescriptor1.setSchemaUpdateStrategyType(ThrowOnPartialOrCreateSchemaStrategy.class.getName());
        testDescriptor.getNodeDescriptors().add(nodeDescriptor1);

        DataNodeDescriptor nodeDescriptor2 = new DataNodeDescriptor();
        nodeDescriptor2.setName("node2");
        nodeDescriptor2.getDataMapNames().add("map2");
        nodeDescriptor2.setParameters("testDataNode2.driver.xml");
        testDescriptor.getNodeDescriptors().add(nodeDescriptor2);

        final DataChannelDescriptorLoader testLoader = new DataChannelDescriptorLoader() {

            @Override
            public ConfigurationTree<DataChannelDescriptor> load(Resource configurationResource)
                    throws ConfigurationException {
                return new ConfigurationTree<>(testDescriptor, null);
            }
        };

        final EventManager eventManager = new MockEventManager();
        final TestListener mockListener = new TestListener();

        Module testModule = b -> {

            ClassLoaderManager classLoaderManager = new DefaultClassLoaderManager();
            b.bind(ClassLoaderManager.class).toInstance(classLoaderManager);
            b.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
            b.bind(PkGenerator.class).to(JdbcPkGenerator.class);
            b.bind(PkGeneratorFactoryProvider.class).to(PkGeneratorFactoryProvider.class);

            CoreModule.extend(b)
                    .initAllExtensions()

                    .addAdapterDetector(FirebirdSniffer.class)
                    .addAdapterDetector(FrontBaseSniffer.class).addAdapterDetector(IngresSniffer.class)
                    .addAdapterDetector(SQLiteSniffer.class).addAdapterDetector(DB2Sniffer.class)
                    .addAdapterDetector(H2Sniffer.class).addAdapterDetector(HSQLDBSniffer.class)
                    .addAdapterDetector(SybaseSniffer.class).addAdapterDetector(DerbySniffer.class)
                    .addAdapterDetector(SQLServerSniffer.class).addAdapterDetector(OracleSniffer.class)
                    .addAdapterDetector(PostgresSniffer.class).addAdapterDetector(MySQLSniffer.class)
                    .addAdapterDetector(MariaDBSniffer.class)

                    .addListener(mockListener)
                    .addProjectLocation(testConfigName)

                    .addPkGenerator(DB2Adapter.class, DB2PkGenerator.class)
                    .addPkGenerator(DerbyAdapter.class, DerbyPkGenerator.class)
                    .addPkGenerator(FrontBaseAdapter.class, FrontBasePkGenerator.class)
                    .addPkGenerator(H2Adapter.class, H2PkGenerator.class)
                    .addPkGenerator(IngresAdapter.class, IngresPkGenerator.class)
                    .addPkGenerator(MySQLAdapter.class, MySQLPkGenerator.class)
                    .addPkGenerator(OracleAdapter.class, OraclePkGenerator.class)
                    .addPkGenerator(Oracle8Adapter.class, OraclePkGenerator.class)
                    .addPkGenerator(PostgresAdapter.class, PostgresPkGenerator.class)
                    .addPkGenerator(SQLServerAdapter.class, SybasePkGenerator.class)
                    .addPkGenerator(SybaseAdapter.class, SybasePkGenerator.class);

            b.bind(EventManager.class).toInstance(eventManager);
            b.bind(EntitySorter.class).toInstance(new AshwoodEntitySorter());
            b.bind(SchemaUpdateStrategyFactory.class).to(DefaultSchemaUpdateStrategyFactory.class);

            final ResourceLocator locator = new ClassLoaderResourceLocator(classLoaderManager) {

                public Collection<Resource> findResources(String name) {
                    // ResourceLocator also used by JdbcAdapter to locate types.xml...
                    // if this is the request we are getting, just let it go through..
                    if (name.endsWith("types.xml")) {
                        return super.findResources(name);
                    }

                    assertEquals(testConfigName, name);
                    return Collections.<Resource>singleton(new MockResource());
                }
            };

            b.bind(ResourceLocator.class).toInstance(locator);
            b.bind(Key.get(ResourceLocator.class, Constants.RESOURCE_LOCATOR)).toInstance(locator);
            b.bind(ConfigurationNameMapper.class).to(DefaultConfigurationNameMapper.class);
            b.bind(DataChannelDescriptorMerger.class).to(DefaultDataChannelDescriptorMerger.class);
            b.bind(DataChannelDescriptorLoader.class).toInstance(testLoader);
            b.bind(DbAdapterFactory.class).to(DefaultDbAdapterFactory.class);
            b.bind(RuntimeProperties.class).to(DefaultRuntimeProperties.class);
            b.bind(BatchTranslatorFactory.class).to(DefaultBatchTranslatorFactory.class);
            b.bind(SelectTranslatorFactory.class).to(DefaultSelectTranslatorFactory.class);

            b.bind(DataSourceFactory.class).toInstance(new MockDataSourceFactory());
            b.bind(JdbcEventLogger.class).to(Slf4jJdbcEventLogger.class);
            b.bind(QueryCache.class).toInstance(mock(QueryCache.class));
            b.bind(RowReaderFactory.class).toInstance(mock(RowReaderFactory.class));
            b.bind(DataNodeFactory.class).to(DefaultDataNodeFactory.class);
            b.bind(SQLTemplateProcessor.class).toInstance(mock(SQLTemplateProcessor.class));

            b.bind(EventBridge.class).toProvider(NoopEventBridgeProvider.class);
            b.bind(DataRowStoreFactory.class).to(DefaultDataRowStoreFactory.class);

            b.bind(ValueObjectTypeRegistry.class).to(DefaultValueObjectTypeRegistry.class);
            b.bind(ValueComparisonStrategyFactory.class).to(DefaultValueComparisonStrategyFactory.class);
        };

        Injector injector = DIBootstrap.createInjector(testModule);

        // create and initialize provide instance to test
        DataDomainProvider provider = new DataDomainProvider();
        injector.injectMembers(provider);

        DataChannel channel = provider.get();
        assertNotNull(channel);

        assertTrue(channel instanceof DataDomain);

        DataDomain domain = (DataDomain) channel;
        assertSame(eventManager, domain.getEventManager());
        assertEquals(2, domain.getDataMaps().size());
        assertTrue(domain.getDataMaps().contains(map1));
        assertTrue(domain.getDataMaps().contains(map2));

        assertEquals(2, domain.getDataNodes().size());
        DataNode node1 = domain.getDataNode("node1");
        assertNotNull(node1);
        assertEquals(1, node1.getDataMaps().size());
        assertSame(map1, node1.getDataMaps().iterator().next());
        assertSame(node1, domain.lookupDataNode(map1));
        assertEquals(nodeDescriptor1.getDataSourceFactoryType(), node1.getDataSourceFactory());
        assertNotNull(node1.getDataSource());

        assertNotNull(node1.getSchemaUpdateStrategy());
        assertEquals(nodeDescriptor1.getSchemaUpdateStrategyType(), node1.getSchemaUpdateStrategy().getClass()
                .getName());

        assertNotNull(node1.getAdapter());
        assertEquals(OracleAdapter.class, node1.getAdapter().getClass());

        DataNode node2 = domain.getDataNode("node2");
        assertNotNull(node2);
        assertEquals(1, node2.getDataMaps().size());
        assertSame(map2, node2.getDataMaps().iterator().next());
        assertSame(node2, domain.lookupDataNode(map2));
        assertNull(node2.getDataSourceFactory());
        assertNotNull(node2.getDataSource());
        assertNotNull(node2.getSchemaUpdateStrategy());
        assertEquals(SkipSchemaUpdateStrategy.class.getName(), node2.getSchemaUpdateStrategy().getClass().getName());

        assertNotNull(node2.getAdapter());

        // check that we have mock listener passed correctly
        Persistent mockPersistent = mock(Persistent.class);
        ObjectId mockObjectId = mock(ObjectId.class);
        when(mockObjectId.getEntityName()).thenReturn("mock-entity-name");
        when(mockPersistent.getObjectId()).thenReturn(mockObjectId);
        domain.getEntityResolver().getCallbackRegistry().performCallbacks(LifecycleEvent.POST_LOAD, mockPersistent);

        assertEquals("Should call postLoadCallback() method", 1, TestListener.counter.get());
    }

    static class TestListener {

        static private AtomicInteger counter = new AtomicInteger();

        @PostLoad
        public void postLoadCallback(Object object) {
            counter.incrementAndGet();
        }
    }
}
