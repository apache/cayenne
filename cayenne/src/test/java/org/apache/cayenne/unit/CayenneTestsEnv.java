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
import org.apache.cayenne.access.dbsync.SkipSchemaUpdateStrategy;
import org.apache.cayenne.access.jdbc.SQLTemplateProcessor;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.access.translator.batch.BatchTranslatorFactory;
import org.apache.cayenne.access.translator.select.SelectTranslatorFactory;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.DataSourceDescriptor;
import org.apache.cayenne.configuration.runtime.CoreModule;
import org.apache.cayenne.configuration.runtime.DbAdapterFactory;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.Procedure;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DbHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.unit.dba.TestDbAdapter;
import org.apache.cayenne.unit.runtime.DbCleaner;
import org.apache.cayenne.unit.runtime.FlavoredDbHelper;
import org.apache.cayenne.unit.runtime.RuntimeCaseModule;
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

    private static final Injector INJECTOR;

    // shared stack parts... use these directly from the tests
    public static final TestDataSources DATA_SOURCES;
    public static final AllTestsSchemaManager SCHEMAS;

    static {
        INJECTOR = DIBootstrap.createInjector(new RuntimeCaseModule());

        DATA_SOURCES = new TestDataSources(
                INJECTOR.getInstance(DataSourceDescriptor.class),
                INJECTOR.getInstance(AdhocObjectFactory.class));

        SCHEMAS = new AllTestsSchemaManager(
                DATA_SOURCES.sharedDataSource(),
                INJECTOR.getInstance(DbAdapter.class),
                INJECTOR.getInstance(JdbcEventLogger.class),
                INJECTOR.getInstance(DataMapLoader.class));

        SCHEMAS.rebuildSchema();
    }

    private final String project;
    private final Module[] extraModules;
    private final boolean autoClean;
    private final String retainStrategy;

    // single-test scoped vars
    private DataContext context;
    private DbHelper dbHelper;
    private DbCleaner dbCleaner;
    private CayenneRuntime runtime;
    private TestDbAdapter testDbAdapter;

    private CayenneTestsEnv(String project, Module[] extraModules, boolean autoClean, String retainStrategy) {
        this.project = Objects.requireNonNull(project);
        this.extraModules = extraModules;
        this.autoClean = autoClean;
        this.retainStrategy = retainStrategy;
    }

    public static CayenneTestsEnv forProject(String project) {
        // Soft refs by default instead of weak — avoids GC-sensitive test flakiness.
        return new CayenneTestsEnv(project, new Module[0], true, "soft");
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
        this.runtime = buildRuntime();
        synthesizeDataNodes(runtime);

        this.context = (DataContext) runtime.newContext();

        DbAdapter firstAdapter = runtime.getDataDomain().getDataNodes().iterator().next().getAdapter();
        this.testDbAdapter = TestDbAdapter.of(firstAdapter);
        tweakProcedures(runtime, testDbAdapter);

        this.dbHelper = new FlavoredDbHelper(
                DATA_SOURCES.sharedDataSource(),
                firstAdapter.getQuotingStrategy(),
                context.getEntityResolver().getDataMaps().iterator().next());

        this.dbCleaner = new DbCleaner(
                SCHEMAS,
                dbHelper,
                context.getEntityResolver().getDataMaps().stream().map(DataMap::getName).collect(Collectors.toSet()));

        if (autoClean) {
            dbCleaner.clean();
        }
    }

    @Override
    public void afterEach(ExtensionContext ctx) {
        if (runtime != null) {
            runtime.shutdown();
        }
        this.context = null;
        this.dbHelper = null;
        this.dbCleaner = null;
        this.runtime = null;
        this.testDbAdapter = null;
    }

    private CayenneRuntime buildRuntime() {

        List<Module> modules = new ArrayList<>();
        modules.add(b -> CoreModule.extend(b).setProperty(Constants.OBJECT_RETAIN_STRATEGY_PROPERTY, retainStrategy));
        Collections.addAll(modules, extraModules);

        return CayenneRuntime.builder()
                .addConfig(project)
                .addModules(modules)
                .build();
    }

    private static void synthesizeDataNodes(CayenneRuntime runtime) {

        DataDomain domain = runtime.getDataDomain();
        Injector runtimeInjector = runtime.getInjector();

        JdbcEventLogger jdbcEventLogger = runtimeInjector.getInstance(JdbcEventLogger.class);
        RowReaderFactory rowReaderFactory = runtimeInjector.getInstance(RowReaderFactory.class);
        BatchTranslatorFactory batchTranslatorFactory = runtimeInjector.getInstance(BatchTranslatorFactory.class);
        SelectTranslatorFactory selectTranslatorFactory = runtimeInjector.getInstance(SelectTranslatorFactory.class);
        SQLTemplateProcessor sqlTemplateProcessor = runtimeInjector.getInstance(SQLTemplateProcessor.class);

        for (DataMap dataMap : domain.getDataMaps()) {

            DataSource dataSource = DATA_SOURCES.dataSource(dataMap.getName());

            DataNode node = new TestTelemetryDataNode(dataMap.getName());

            node.setJdbcEventLogger(jdbcEventLogger);
            node.setRowReaderFactory(rowReaderFactory);
            node.setBatchTranslatorFactory(batchTranslatorFactory);
            node.setSelectTranslatorFactory(selectTranslatorFactory);
            node.setDataSource(dataSource);

            // this gives us AutoAdapter
            DbAdapter adapter = runtimeInjector.getInstance(DbAdapterFactory.class)
                    .createAdapter(new DataNodeDescriptor(), dataSource);

            node.setAdapter(adapter);
            node.setSchemaUpdateStrategy(new SkipSchemaUpdateStrategy());
            node.setSqlTemplateProcessor(sqlTemplateProcessor);

            node.addDataMap(dataMap);
            domain.addNode(node);
        }

        if (domain.getDataMaps().size() == 1) {
            domain.setDefaultNode(domain.getDataNodes().iterator().next());
        }
    }

    private static void tweakProcedures(CayenneRuntime runtime, TestDbAdapter adapter) {
        for (DataMap dataMap : runtime.getDataDomain().getDataMaps()) {
            for (Procedure proc : dataMap.getProcedures()) {
                adapter.tweakProcedure(proc);
            }
        }
    }

    public DataContext context() {
        return context;
    }

    public DbHelper dbHelper() {
        return dbHelper;
    }

    public TableHelper table(String tableName, String... columns) {
        return new TableHelper(dbHelper, tableName, columns);
    }

    public CayenneRuntime runtime() {
        return runtime;
    }

    public TestDbAdapter testDbAdapter() {
        return testDbAdapter;
    }

    public EntityResolver entityResolver() {
        return runtime.getDataDomain().getEntityResolver();
    }

    public DataNode dataNode() {
        DataDomain channel = runtime.getDataDomain();
        return channel.getDataNodes().iterator().next();
    }

    public void runWithQueriesBlocked(Runnable task) {
        TestTelemetry.runWithQueriesBlocked(runtime, task);
    }

    public int runWithQueryCounter(Runnable task) {
        return TestTelemetry.runWithQueryCounter(runtime, task);
    }

    public AdhocObjectFactory adhocObjectFactory() {
        return runtime.getInjector().getInstance(AdhocObjectFactory.class);
    }

    public DbCleaner dbCleaner() {
        return dbCleaner;
    }

    public DataSourceDescriptor dataSourceDescriptor() {
        return INJECTOR.getInstance(DataSourceDescriptor.class);
    }

    public SQLTemplateCustomizer sqlTemplateCustomizer() {
        return SQLTemplateCustomizer.of(dataNode().getAdapter());
    }
}
