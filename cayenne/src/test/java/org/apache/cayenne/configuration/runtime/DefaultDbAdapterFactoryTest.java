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

import org.apache.cayenne.access.translator.BatchTranslator;
import org.apache.cayenne.access.types.DefaultValueObjectTypeRegistry;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.DefaultRuntimeProperties;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.dba.AutoAdapter;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.JdbcPkGenerator;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.dba.sybase.SybaseAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.ClassLoaderManager;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;
import org.apache.cayenne.di.spi.DefaultClassLoaderManager;
import org.apache.cayenne.log.SQLLogger;
import org.apache.cayenne.log.Slf4jSQLLogger;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.reflect.generic.DefaultValueComparisonStrategyFactory;
import org.apache.cayenne.reflect.generic.ValueComparisonStrategyFactory;
import org.apache.cayenne.resource.ClassLoaderResourceLocator;
import org.apache.cayenne.resource.ResourceLocator;
import org.apache.cayenne.unit.jdbc.TestConnection;
import org.apache.cayenne.unit.jdbc.TestDataSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultDbAdapterFactoryTest {

    @Test
    public void createdAdapter_Auto() throws Exception {

        final DbAdapter adapter = mock(JdbcAdapter.class);
        when(adapter.createTable(any(DbEntity.class))).thenReturn("XXXXX");
        when(adapter.unwrap()).thenReturn(adapter);

        List<DbAdapterDetector> detectors = new ArrayList<>();
        detectors.add(md -> adapter);

        TestConnection connection = new TestConnection();

        TestDataSource dataSource = new TestDataSource();
        dataSource.setupConnection(connection);

        Module testModule = binder -> {
            CoreModule.extend(binder).initAllExtensions();

            binder.bind(PkGenerator.class).to(JdbcPkGenerator.class);
            binder.bind(PkGeneratorFactoryProvider.class).to(PkGeneratorFactoryProvider.class);
            binder.bind(SQLLogger.class).to(Slf4jSQLLogger.class);
            binder.bind(ClassLoaderManager.class).to(DefaultClassLoaderManager.class);
            binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
            binder.bind(RuntimeProperties.class).to(DefaultRuntimeProperties.class);
            binder.bind(Key.get(BatchTranslator.class, BatchTranslator.INSERT)).toInstance(mock(BatchTranslator.class));
            binder.bind(Key.get(BatchTranslator.class, BatchTranslator.UPDATE)).toInstance(mock(BatchTranslator.class));
            binder.bind(Key.get(BatchTranslator.class, BatchTranslator.DELETE)).toInstance(mock(BatchTranslator.class));
        };

        Injector injector = DIBootstrap.createInjector(testModule);

        DefaultDbAdapterFactory factory = new DefaultDbAdapterFactory(detectors);
        injector.injectMembers(factory);

        DbAdapter createdAdapter = factory.createAdapter(new DataNodeDescriptor(), dataSource);
        assertTrue(createdAdapter instanceof AutoAdapter);
        assertEquals("XXXXX", createdAdapter.createTable(new DbEntity("Test")));
    }

    @Test
    public void createdAdapter_Generic() {

        List<DbAdapterDetector> detectors = new ArrayList<>();

        Module testModule = b -> {

            CoreModule.extend(b).initAllExtensions();

            b.bind(PkGenerator.class).to(JdbcPkGenerator.class);
            b.bind(PkGeneratorFactoryProvider.class).to(PkGeneratorFactoryProvider.class);
            b.bind(SQLLogger.class).to(Slf4jSQLLogger.class);
            b.bind(ClassLoaderManager.class).to(DefaultClassLoaderManager.class);
            b.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
            b.bind(ResourceLocator.class).to(ClassLoaderResourceLocator.class);
            b.bind(Key.get(ResourceLocator.class, Constants.RESOURCE_LOCATOR)).to(ClassLoaderResourceLocator.class);
            b.bind(RuntimeProperties.class).to(DefaultRuntimeProperties.class);
            b.bind(Key.get(BatchTranslator.class, BatchTranslator.INSERT)).toInstance(mock(BatchTranslator.class));
            b.bind(Key.get(BatchTranslator.class, BatchTranslator.UPDATE)).toInstance(mock(BatchTranslator.class));
            b.bind(Key.get(BatchTranslator.class, BatchTranslator.DELETE)).toInstance(mock(BatchTranslator.class));

            b.bind(ValueObjectTypeRegistry.class).to(DefaultValueObjectTypeRegistry.class);
        };

        Injector injector = DIBootstrap.createInjector(testModule);

        DefaultDbAdapterFactory factory = new DefaultDbAdapterFactory(detectors);
        injector.injectMembers(factory);

        DbAdapter createdAdapter = factory.createAdapter(new DataNodeDescriptor(), new TestDataSource());
        assertNotNull(createdAdapter);
        assertTrue(createdAdapter instanceof AutoAdapter, "Unexpected class: " + createdAdapter.getClass().getName());
        assertEquals("CREATE TABLE Test ()", createdAdapter.createTable(new DbEntity("Test")));
    }

    @Test
    public void createdAdapter_Custom() throws Exception {

        DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor();
        nodeDescriptor.setAdapterType(SybaseAdapter.class.getName());

        List<DbAdapterDetector> detectors = new ArrayList<>();

        Module testModule = b -> {
            CoreModule.extend(b).initAllExtensions();

            b.bind(PkGenerator.class).to(JdbcPkGenerator.class);
            b.bind(PkGeneratorFactoryProvider.class).to(PkGeneratorFactoryProvider.class);
            b.bind(SQLLogger.class).to(Slf4jSQLLogger.class);
            b.bind(ClassLoaderManager.class).to(DefaultClassLoaderManager.class);
            b.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
            b.bind(ResourceLocator.class).to(ClassLoaderResourceLocator.class);
            b.bind(Key.get(ResourceLocator.class, Constants.RESOURCE_LOCATOR)).to(ClassLoaderResourceLocator.class);
            b.bind(RuntimeProperties.class).to(DefaultRuntimeProperties.class);
            b.bind(Key.get(BatchTranslator.class, BatchTranslator.INSERT)).toInstance(mock(BatchTranslator.class));
            b.bind(Key.get(BatchTranslator.class, BatchTranslator.UPDATE)).toInstance(mock(BatchTranslator.class));
            b.bind(Key.get(BatchTranslator.class, BatchTranslator.DELETE)).toInstance(mock(BatchTranslator.class));

            b.bind(ValueObjectTypeRegistry.class).to(DefaultValueObjectTypeRegistry.class);
            b.bind(ValueComparisonStrategyFactory.class).to(DefaultValueComparisonStrategyFactory.class);
        };

        Injector injector = DIBootstrap.createInjector(testModule);

        DefaultDbAdapterFactory factory = new DefaultDbAdapterFactory(detectors);
        injector.injectMembers(factory);

        DbAdapter createdAdapter = factory.createAdapter(nodeDescriptor, new TestDataSource());
        assertNotNull(createdAdapter);
        assertTrue(createdAdapter instanceof SybaseAdapter, "Unexpected class: " + createdAdapter.getClass().getName());
    }

    @Test
    public void createdAdapter_AutoExplicit() throws Exception {

        final DbAdapter adapter = mock(JdbcAdapter.class);
        when(adapter.createTable(any(DbEntity.class))).thenReturn("XXXXX");
        when(adapter.unwrap()).thenReturn(adapter);

        List<DbAdapterDetector> detectors = new ArrayList<>();
        detectors.add(md -> adapter);

        TestConnection connection = new TestConnection();

        TestDataSource dataSource = new TestDataSource();
        dataSource.setupConnection(connection);

        Module testModule = binder -> {
            CoreModule.extend(binder).initAllExtensions();

            binder.bind(PkGenerator.class).to(JdbcPkGenerator.class);
            binder.bind(PkGeneratorFactoryProvider.class).to(PkGeneratorFactoryProvider.class);
            binder.bind(ClassLoaderManager.class).to(DefaultClassLoaderManager.class);
            binder.bind(SQLLogger.class).to(Slf4jSQLLogger.class);
            binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
            binder.bind(RuntimeProperties.class).to(DefaultRuntimeProperties.class);
            binder.bind(Key.get(BatchTranslator.class, BatchTranslator.INSERT)).toInstance(mock(BatchTranslator.class));
            binder.bind(Key.get(BatchTranslator.class, BatchTranslator.UPDATE)).toInstance(mock(BatchTranslator.class));
            binder.bind(Key.get(BatchTranslator.class, BatchTranslator.DELETE)).toInstance(mock(BatchTranslator.class));
        };

        Injector injector = DIBootstrap.createInjector(testModule);

        DefaultDbAdapterFactory factory = new DefaultDbAdapterFactory(detectors);
        injector.injectMembers(factory);

        DataNodeDescriptor nodeDescriptor = new DataNodeDescriptor();
        nodeDescriptor.setAdapterType(AutoAdapter.class.getName());

        DbAdapter createdAdapter = factory.createAdapter(nodeDescriptor, dataSource);
        assertTrue(createdAdapter instanceof AutoAdapter);
        assertEquals("XXXXX", createdAdapter.createTable(new DbEntity("Test")));
    }
}
