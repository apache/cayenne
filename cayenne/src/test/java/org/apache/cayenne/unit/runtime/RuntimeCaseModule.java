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
package org.apache.cayenne.unit.runtime;

import org.apache.cayenne.access.DefaultObjectMapRetainStrategy;
import org.apache.cayenne.access.ObjectMapRetainStrategy;
import org.apache.cayenne.access.translator.batch.BatchTranslatorFactory;
import org.apache.cayenne.access.types.*;
import org.apache.cayenne.configuration.ConfigurationNameMapper;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.DefaultConfigurationNameMapper;
import org.apache.cayenne.configuration.DefaultObjectStoreFactory;
import org.apache.cayenne.configuration.DefaultRuntimeProperties;
import org.apache.cayenne.configuration.ObjectStoreFactory;
import org.apache.cayenne.configuration.RuntimeProperties;
import org.apache.cayenne.configuration.runtime.CoreModuleExtender;
import org.apache.cayenne.configuration.runtime.PkGeneratorFactoryProvider;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.configuration.xml.DefaultHandlerFactory;
import org.apache.cayenne.configuration.xml.HandlerFactory;
import org.apache.cayenne.configuration.xml.NoopDataChannelMetaData;
import org.apache.cayenne.configuration.xml.XMLDataMapLoader;
import org.apache.cayenne.configuration.xml.XMLReaderProvider;
import org.apache.cayenne.dba.JdbcPkGenerator;
import org.apache.cayenne.dba.PkGenerator;
import org.apache.cayenne.dba.db2.DB2Adapter;
import org.apache.cayenne.dba.db2.DB2PkGenerator;
import org.apache.cayenne.dba.derby.DerbyAdapter;
import org.apache.cayenne.dba.derby.DerbyPkGenerator;
import org.apache.cayenne.dba.frontbase.FrontBaseAdapter;
import org.apache.cayenne.dba.frontbase.FrontBasePkGenerator;
import org.apache.cayenne.dba.h2.H2Adapter;
import org.apache.cayenne.dba.h2.H2PkGenerator;
import org.apache.cayenne.dba.ingres.IngresAdapter;
import org.apache.cayenne.dba.ingres.IngresPkGenerator;
import org.apache.cayenne.dba.mysql.MySQLAdapter;
import org.apache.cayenne.dba.mysql.MySQLPkGenerator;
import org.apache.cayenne.dba.oracle.Oracle8Adapter;
import org.apache.cayenne.dba.oracle.OracleAdapter;
import org.apache.cayenne.dba.oracle.OraclePkGenerator;
import org.apache.cayenne.dba.postgres.PostgresAdapter;
import org.apache.cayenne.dba.postgres.PostgresPkGenerator;
import org.apache.cayenne.dba.sqlserver.SQLServerAdapter;
import org.apache.cayenne.dba.sybase.SybaseAdapter;
import org.apache.cayenne.dba.sybase.SybasePkGenerator;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.ClassLoaderManager;
import org.apache.cayenne.di.Key;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;
import org.apache.cayenne.di.spi.DefaultClassLoaderManager;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.log.Slf4jJdbcEventLogger;
import org.apache.cayenne.reflect.generic.DefaultValueComparisonStrategyFactory;
import org.apache.cayenne.reflect.generic.ValueComparisonStrategyFactory;
import org.apache.cayenne.resource.ClassLoaderResourceLocator;
import org.apache.cayenne.resource.ResourceLocator;
import org.xml.sax.XMLReader;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class RuntimeCaseModule implements Module {

    public void configure(Binder binder) {

        binder.bind(PkGeneratorFactoryProvider.class).to(PkGeneratorFactoryProvider.class);
        binder.bind(PkGenerator.class).to(JdbcPkGenerator.class);

        new RuntimeCaseModuleExtender(binder)
                .initAllExtensions()

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
                .addPkGenerator(SybaseAdapter.class, SybasePkGenerator.class)

                // configure extended types
                .addDefaultExtendedType(new VoidType())
                .addDefaultExtendedType(new BigDecimalType())
                .addDefaultExtendedType(new BooleanType())
                .addDefaultExtendedType(new ByteArrayType(false, true))
                .addDefaultExtendedType(new ByteType(false))
                .addDefaultExtendedType(new CharType(false, true))
                .addDefaultExtendedType(new DateType())
                .addDefaultExtendedType(new DoubleType())
                .addDefaultExtendedType(new FloatType())
                .addDefaultExtendedType(new IntegerType())
                .addDefaultExtendedType(new LongType())
                .addDefaultExtendedType(new ShortType(false))
                .addDefaultExtendedType(new TimeType())
                .addDefaultExtendedType(new TimestampType())
                .addDefaultExtendedType(new UtilDateType())
                .addDefaultExtendedType(new CalendarType<>(GregorianCalendar.class))
                .addDefaultExtendedType(new CalendarType<>(Calendar.class))
                .addDefaultExtendedType(new DurationType())

                .addExtendedTypeFactory(new InternalUnsupportedTypeFactory())

                .addValueObjectType(BigIntegerValueType.class)
                .addValueObjectType(BigDecimalValueType.class)
                .addValueObjectType(UUIDValueType.class)
                .addValueObjectType(LocalDateValueType.class)
                .addValueObjectType(LocalTimeValueType.class)
                .addValueObjectType(LocalDateTimeValueType.class)
                .addValueObjectType(PeriodValueType.class)
                .addValueObjectType(CharacterValueType.class);

        binder.bind(ValueObjectTypeRegistry.class).to(DefaultValueObjectTypeRegistry.class);
        binder.bind(ValueComparisonStrategyFactory.class).to(DefaultValueComparisonStrategyFactory.class);

        binder.bind(JdbcEventLogger.class).to(Slf4jJdbcEventLogger.class);
        binder.bind(RuntimeProperties.class).to(DefaultRuntimeProperties.class);
        binder.bind(ObjectMapRetainStrategy.class).to(DefaultObjectMapRetainStrategy.class);

        // this factory is a hack that allows to inject to DbAdapters loaded outside of
        // server runtime... BatchQueryBuilderFactory is hardcoded and whatever is placed
        // in the CoreModule is ignored
        binder.bind(BatchTranslatorFactory.class).toProvider(RuntimeCaseBatchQueryBuilderFactoryProvider.class);
        binder.bind(ClassLoaderManager.class).to(DefaultClassLoaderManager.class);
        binder.bind(AdhocObjectFactory.class).to(DefaultAdhocObjectFactory.class);
        binder.bind(ResourceLocator.class).to(ClassLoaderResourceLocator.class);
        binder.bind(Key.get(ResourceLocator.class, Constants.RESOURCE_LOCATOR)).to(ClassLoaderResourceLocator.class);
        binder.bind(ObjectStoreFactory.class).to(DefaultObjectStoreFactory.class);
        binder.bind(DataMapLoader.class).to(XMLDataMapLoader.class);
        binder.bind(ConfigurationNameMapper.class).to(DefaultConfigurationNameMapper.class);
        binder.bind(HandlerFactory.class).to(DefaultHandlerFactory.class);
        binder.bind(DataChannelMetaData.class).to(NoopDataChannelMetaData.class);

        binder.bind(XMLReader.class).toProviderInstance(new XMLReaderProvider(false)).withoutScope();
    }

    // this class exists so that ToolsModule can call "initAllExtensions()" that is protected in CoreModuleExtender.
    static class RuntimeCaseModuleExtender extends CoreModuleExtender {
        public RuntimeCaseModuleExtender(Binder binder) {
            super(binder);
        }

        @Override
        protected CoreModuleExtender initAllExtensions() {
            return super.initAllExtensions();
        }
    }
}
