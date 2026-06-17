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
package org.apache.cayenne.unit;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.DataSourceDescriptor;
import org.apache.cayenne.configuration.runtime.CoreModule;
import org.apache.cayenne.configuration.runtime.DataNodeFactory;
import org.apache.cayenne.datasource.DataSourceBuilder;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.QuotingStrategy;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DbHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.unit.datasource.DataSourceConfigLoader;
import org.apache.cayenne.unit.dba.TestDbAdapter;
import org.apache.cayenne.unit.telemetry.TelemetricDataNodeFactory;
import org.apache.cayenne.unit.telemetry.TestTelemetry;
import org.apache.cayenne.unit.util.SQLTemplateCustomizer;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * JUnit 5 extension that wires a Cayenne test environment.
 */
public class CayenneTestsEnv implements BeforeEachCallback, AfterEachCallback {

    // shared stack parts... use these directly from the tests
    // TODO: we should support multiple physically-isolated schemas for cleaner tests
    public static final DbSchemaManager COMMON_SCHEMA;

    static {
        DataSourceDescriptor dsDescriptor = DataSourceConfigLoader.load();
        DataSource ds = DataSourceBuilder
                .url(dsDescriptor.getDataSourceUrl())
                .driver(dsDescriptor.getJdbcDriver())
                .userName(dsDescriptor.getUserName())
                .password(dsDescriptor.getPassword())
                .pool(dsDescriptor.getMinConnections(), dsDescriptor.getMaxConnections())
                .build();

        // "cayenne-ALL.xml" is a special synthetic project file that includes all test DataMaps
        COMMON_SCHEMA = new DbSchemaManager("cayenne-ALL.xml", dsDescriptor, ds);
        COMMON_SCHEMA.rebuildSchema();
    }

    public static CayenneTestsEnv forProject(String project) {
        // Soft refs by default instead of weak — avoids GC-sensitive test flakiness.
        return new CayenneTestsEnv(project, new Module[0], true, "soft");
    }

    private final String project;
    private final Module[] extraModules;
    private final boolean autoClean;
    private final String retainStrategy;

    // created in beforeEach, discarded in afterEach
    private TestScope scope;

    private CayenneTestsEnv(String project, Module[] extraModules, boolean autoClean, String retainStrategy) {
        this.project = Objects.requireNonNull(project);
        this.extraModules = extraModules;
        this.autoClean = autoClean;
        this.retainStrategy = retainStrategy;
    }
    
    public CayenneTestsEnv withExtraModules(Module... modules) {
        return new CayenneTestsEnv(project, modules, autoClean, retainStrategy);
    }

    public CayenneTestsEnv withoutAutoClean() {
        return new CayenneTestsEnv(project, extraModules, false, retainStrategy);
    }

    public CayenneTestsEnv withWeakReferences() {
        return new CayenneTestsEnv(project, extraModules, autoClean, "weak");
    }

    @Override
    public void beforeEach(ExtensionContext ctx) {
        CayenneRuntime runtime = buildRuntime();
        DataContext context = (DataContext) runtime.newContext();

        DbAdapter firstAdapter = runtime.getDataDomain().getDataNodes().iterator().next().getAdapter();
        TestDbAdapter testDbAdapter = TestDbAdapter.of(firstAdapter);
        tweakProcedures(runtime, testDbAdapter);

        DbEntity firstDbEntity = context.getEntityResolver().getDbEntities().iterator().next();
        QuotingStrategy quotingStrategy = firstAdapter.getQuotingStrategy(firstDbEntity);

        DbHelper dbHelper = new FlavorAwareDbHelper(COMMON_SCHEMA.dataSource(), quotingStrategy);

        DbCleaner dbCleaner = new DbCleaner(
                COMMON_SCHEMA,
                dbHelper,
                context.getEntityResolver().getDataMaps().stream().map(DataMap::getName).collect(Collectors.toSet()));

        this.scope = new TestScope(runtime, context, testDbAdapter, dbHelper, dbCleaner);

        if (autoClean) {
            scope.clean();
        }
    }

    @Override
    public void afterEach(ExtensionContext ctx) {
        if (scope != null) {
            scope.shutdown();
            this.scope = null;
        }
    }

    private CayenneRuntime buildRuntime() {

        List<Module> modules = new ArrayList<>();
        modules.add(b -> CoreModule.extend(b).setProperty(Constants.OBJECT_RETAIN_STRATEGY_PROPERTY, retainStrategy));
        modules.add(b -> b.bind(DataNodeFactory.class).to(TelemetricDataNodeFactory.class));
        Collections.addAll(modules, extraModules);

        return CayenneRuntime.builder()
                .addConfig(project)
                .addModules(modules)
                .dataSource(COMMON_SCHEMA.dataSource())
                .build();
    }

    private static void tweakProcedures(CayenneRuntime runtime, TestDbAdapter adapter) {
        for (DataMap dataMap : runtime.getDataDomain().getDataMaps()) {
            for (Procedure proc : dataMap.getProcedures()) {
                adapter.tweakProcedure(proc);
            }
        }
    }

    public DataContext context() {
        return scope.context();
    }

    public TableHelper table(String tableName, String... columns) {
        return new TableHelper(scope.dbHelper(), tableName, columns);
    }

    public CayenneRuntime runtime() {
        return scope.runtime();
    }

    public TestDbAdapter testDbAdapter() {
        return scope.testDbAdapter();
    }

    public EntityResolver entityResolver() {
        return scope.runtime().getDataDomain().getEntityResolver();
    }

    public DataNode dataNode() {
        DataDomain channel = scope.runtime().getDataDomain();
        return channel.getDataNodes().iterator().next();
    }

    public void runWithQueriesBlocked(Runnable task) {
        TestTelemetry.runWithQueriesBlocked(scope.runtime(), task);
    }

    public int runWithQueryCounter(Runnable task) {
        return TestTelemetry.runWithQueryCounter(scope.runtime(), task);
    }

    public AdhocObjectFactory adhocObjectFactory() {
        return scope.runtime().getInjector().getInstance(AdhocObjectFactory.class);
    }

    public DbCleaner dbCleaner() {
        return scope.dbCleaner();
    }

    public SQLTemplateCustomizer sqlTemplateCustomizer() {
        return SQLTemplateCustomizer.of(dataNode().getAdapter());
    }

    private record TestScope(
            CayenneRuntime runtime,
            DataContext context,
            TestDbAdapter testDbAdapter,
            DbHelper dbHelper,
            DbCleaner dbCleaner) {

        void clean() {
            dbCleaner.clean();
        }

        void shutdown() {
            runtime.shutdown();
        }
    }
}
