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
package org.apache.cayenne.configuration.server;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.dbsync.SchemaUpdateStrategy;
import org.apache.cayenne.access.dbsync.SkipSchemaUpdateStrategy;
import org.apache.cayenne.access.dbsync.ThrowOnPartialOrCreateSchemaStrategy;
import org.apache.cayenne.access.jdbc.SQLTemplateProcessor;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.access.translator.batch.BatchTranslatorFactory;
import org.apache.cayenne.access.translator.batch.DefaultBatchTranslatorFactory;
import org.apache.cayenne.access.translator.select.DefaultSelectTranslatorFactory;
import org.apache.cayenne.access.translator.select.SelectTranslatorFactory;
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
import org.apache.cayenne.dba.db2.DB2Sniffer;
import org.apache.cayenne.dba.derby.DerbySniffer;
import org.apache.cayenne.dba.firebird.FirebirdSniffer;
import org.apache.cayenne.dba.frontbase.FrontBaseSniffer;
import org.apache.cayenne.dba.h2.H2Sniffer;
import org.apache.cayenne.dba.hsqldb.HSQLDBSniffer;
import org.apache.cayenne.dba.ingres.IngresSniffer;
import org.apache.cayenne.dba.mysql.MySQLSniffer;
import org.apache.cayenne.dba.openbase.OpenBaseSniffer;
import org.apache.cayenne.dba.oracle.OracleAdapter;
import org.apache.cayenne.dba.oracle.OracleSniffer;
import org.apache.cayenne.dba.postgres.PostgresSniffer;
import org.apache.cayenne.dba.sqlite.SQLiteSniffer;
import org.apache.cayenne.dba.sqlserver.SQLServerSniffer;
import org.apache.cayenne.dba.sybase.SybaseSniffer;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.ClassLoaderManager;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;
import org.apache.cayenne.di.spi.DefaultClassLoaderManager;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.event.MockEventManager;
import org.apache.cayenne.log.CommonsJdbcEventLogger;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntitySorter;
import org.apache.cayenne.resource.ClassLoaderResourceLocator;
import org.apache.cayenne.resource.Resource;
import org.apache.cayenne.resource.ResourceLocator;
import org.apache.cayenne.resource.mock.MockResource;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

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
				return new ConfigurationTree<DataChannelDescriptor>(testDescriptor, null);
			}
		};

		final EventManager eventManager = new MockEventManager();

		Module testModule = new Module() {

			@Override
			public void configure(Binder binder) {
				final ClassLoaderManager classLoaderManager = new DefaultClassLoaderManager();
				binder.bind(ClassLoaderManager.class).toInstance(classLoaderManager);
				binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);

				binder.bindMap(Constants.PROPERTIES_MAP);

				binder.bind(FirebirdSniffer.class).to(FirebirdSniffer.class);
				binder.bind(OpenBaseSniffer.class).to(OpenBaseSniffer.class);
				binder.bind(FrontBaseSniffer.class).to(FrontBaseSniffer.class);
				binder.bind(IngresSniffer.class).to(IngresSniffer.class);
				binder.bind(SQLiteSniffer.class).to(SQLiteSniffer.class);
				binder.bind(DB2Sniffer.class).to(DB2Sniffer.class);
				binder.bind(H2Sniffer.class).to(H2Sniffer.class);
				binder.bind(HSQLDBSniffer.class).to(HSQLDBSniffer.class);
				binder.bind(SybaseSniffer.class).to(SybaseSniffer.class);
				binder.bind(DerbySniffer.class).to(DerbySniffer.class);
				binder.bind(SQLServerSniffer.class).to(SQLServerSniffer.class);
				binder.bind(OracleSniffer.class).to(OracleSniffer.class);
				binder.bind(PostgresSniffer.class).to(PostgresSniffer.class);
				binder.bind(MySQLSniffer.class).to(MySQLSniffer.class);

				binder.bindList(Constants.SERVER_ADAPTER_DETECTORS_LIST).add(FirebirdSniffer.class)
						.add(OpenBaseSniffer.class).add(FrontBaseSniffer.class).add(IngresSniffer.class)
						.add(SQLiteSniffer.class).add(DB2Sniffer.class).add(H2Sniffer.class).add(HSQLDBSniffer.class)
						.add(SybaseSniffer.class).add(DerbySniffer.class).add(SQLServerSniffer.class)
						.add(OracleSniffer.class).add(PostgresSniffer.class).add(MySQLSniffer.class);
				binder.bindList(Constants.SERVER_DOMAIN_FILTERS_LIST);
				binder.bindList(Constants.SERVER_PROJECT_LOCATIONS_LIST).add(testConfigName);

				// configure extended types
				binder.bindList(Constants.SERVER_DEFAULT_TYPES_LIST);
				binder.bindList(Constants.SERVER_USER_TYPES_LIST);
				binder.bindList(Constants.SERVER_TYPE_FACTORIES_LIST);

				binder.bind(EventManager.class).toInstance(eventManager);
				binder.bind(EntitySorter.class).toInstance(new AshwoodEntitySorter());

				final ResourceLocator locator = new ClassLoaderResourceLocator(classLoaderManager) {

					public Collection<Resource> findResources(String name) {
						// ResourceLocator also used by JdbcAdapter to locate
						// types.xml... if this is the request we are getting,
						// just let
						// it go through..
						if (name.endsWith("types.xml")) {
							return super.findResources(name);
						}

						assertEquals(testConfigName, name);
						return Collections.<Resource> singleton(new MockResource());
					}
				};

				binder.bind(ResourceLocator.class).toInstance(locator);
				binder.bind(Key.get(ResourceLocator.class, Constants.SERVER_RESOURCE_LOCATOR)).toInstance(locator);
				binder.bind(ConfigurationNameMapper.class).to(DefaultConfigurationNameMapper.class);
				binder.bind(DataChannelDescriptorMerger.class).to(DefaultDataChannelDescriptorMerger.class);
				binder.bind(DataChannelDescriptorLoader.class).toInstance(testLoader);
				binder.bind(SchemaUpdateStrategy.class).toInstance(new SkipSchemaUpdateStrategy());
				binder.bind(DbAdapterFactory.class).to(DefaultDbAdapterFactory.class);
				binder.bind(RuntimeProperties.class).to(DefaultRuntimeProperties.class);
				binder.bind(BatchTranslatorFactory.class).to(DefaultBatchTranslatorFactory.class);
				binder.bind(SelectTranslatorFactory.class).to(DefaultSelectTranslatorFactory.class);

				binder.bind(DataSourceFactory.class).toInstance(new MockDataSourceFactory());
				binder.bind(JdbcEventLogger.class).to(CommonsJdbcEventLogger.class);
				binder.bind(QueryCache.class).toInstance(mock(QueryCache.class));
				binder.bind(RowReaderFactory.class).toInstance(mock(RowReaderFactory.class));
				binder.bind(DataNodeFactory.class).to(DefaultDataNodeFactory.class);
				binder.bind(SQLTemplateProcessor.class).toInstance(mock(SQLTemplateProcessor.class));
			}
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
		assertEquals(nodeDescriptor1.getParameters(), node1.getDataSourceLocation());

		assertEquals(nodeDescriptor1.getSchemaUpdateStrategyType(), node1.getSchemaUpdateStrategyName());
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
		assertEquals(nodeDescriptor2.getParameters(), node2.getDataSourceLocation());
		assertEquals(SkipSchemaUpdateStrategy.class.getName(), node2.getSchemaUpdateStrategyName());
		assertNotNull(node2.getSchemaUpdateStrategy());
		assertEquals(SkipSchemaUpdateStrategy.class.getName(), node2.getSchemaUpdateStrategy().getClass().getName());

		assertNotNull(node2.getAdapter());
	}
}
