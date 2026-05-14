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
import org.apache.cayenne.configuration.DataMapLoader;
import org.apache.cayenne.configuration.DataSourceDescriptor;
import org.apache.cayenne.configuration.runtime.CoreModule;
import org.apache.cayenne.configuration.runtime.DataNodeFactory;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DbHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.unit.dba.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.AllTestsSchemaManager;
import org.apache.cayenne.unit.di.runtime.DbCleaner;
import org.apache.cayenne.unit.di.runtime.FlavoredDbHelper;
import org.apache.cayenne.unit.di.runtime.RuntimeCaseDataNodeFactory;
import org.apache.cayenne.unit.di.runtime.RuntimeCaseDataSourceFactory;
import org.apache.cayenne.unit.di.runtime.RuntimeCaseModule;
import org.apache.cayenne.unit.util.SQLTemplateCustomizer;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JUnit 5 extension that wires a Cayenne test environment.
 */
public class CayenneTestsEnv implements BeforeEachCallback, AfterEachCallback {

    private static final Injector INJECTOR;
    static final RuntimeCaseDataSourceFactory DATA_SOURCE_FACTORY;
    public static final AllTestsSchemaManager SCHEMA_MANAGER;

    static {
        INJECTOR = DIBootstrap.createInjector(new RuntimeCaseModule());
        DATA_SOURCE_FACTORY = new RuntimeCaseDataSourceFactory(
                INJECTOR.getInstance(DataSourceDescriptor.class),
                INJECTOR.getInstance(AdhocObjectFactory.class));
        SCHEMA_MANAGER = new AllTestsSchemaManager(
                DATA_SOURCE_FACTORY,
                INJECTOR.getInstance(UnitDbAdapter.class),
                INJECTOR.getInstance(DbAdapter.class),
                INJECTOR.getInstance(JdbcEventLogger.class),
                INJECTOR.getInstance(DataMapLoader.class));
        SCHEMA_MANAGER.rebuildSchema();
    }

    private final String project;
    private final Class<?>[] extraModules;
    private final boolean autoClean;
    private final boolean weakReferenceStrategy;

    // single-test scoped vars
    private DataContext context;
    private DbHelper dbHelper;
    private DbCleaner dbCleaner;
    private CayenneRuntime runtime;

    private CayenneTestsEnv(String project, Class<?>[] extraModules, boolean autoClean, boolean weakReferenceStrategy) {
        this.project = project;
        this.extraModules = extraModules;
        this.autoClean = autoClean;
        this.weakReferenceStrategy = weakReferenceStrategy;
    }

    public static CayenneTestsEnv forProject(String project) {
        return new CayenneTestsEnv(project, new Class<?>[0], true, false);
    }

    public CayenneTestsEnv withExtraModules(Class<?>... modules) {
        return new CayenneTestsEnv(project, modules, autoClean, weakReferenceStrategy);
    }

    /**
     * Disables the automatic {@code DBCleaner.clean()} call in {@link #beforeEach}.
     * Use this when the test (or a base class) needs to perform schema-specific
     * setup (e.g. break circular FKs) before cleaning. The caller is then
     * responsible for invoking {@code env.dbCleaner().clean()} from its own
     * {@code @BeforeEach}.
     */
    public CayenneTestsEnv withoutAutoClean() {
        return new CayenneTestsEnv(project, extraModules, false, weakReferenceStrategy);
    }

    /**
     * Configures the runtime with the weak-reference object-tracking strategy
     * instead of the default soft-reference strategy used by other tests. Use
     * for GC-sensitive tests that need objects to be collectable as soon as
     * they become unreferenced.
     */
    public CayenneTestsEnv withWeakReferenceStrategy() {
        return new CayenneTestsEnv(project, extraModules, autoClean, true);
    }

    @Override
    public void beforeEach(ExtensionContext ctx) {
        if (project == null) {
            throw new NullPointerException("Null 'project', annotate your test case with @UseCayenneRuntime");
        }

        this.runtime = buildRuntime();
        this.context = (DataContext) runtime.newContext();

        DataNode node = runtime.getDataDomain().getDataNodes().iterator().next();
        DataMap firstMap = context.getEntityResolver().getDataMaps().iterator().next();
        this.dbHelper = new FlavoredDbHelper(
                DATA_SOURCE_FACTORY.getSharedDataSource(),
                node.getAdapter().getQuotingStrategy(),
                firstMap);

        this.dbCleaner = new DbCleaner(
                SCHEMA_MANAGER,
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
    }

    private CayenneRuntime buildRuntime() {
        UnitDbAdapter unitDbAdapter = INJECTOR.getInstance(UnitDbAdapter.class);


        List<Module> modules = new ArrayList<>();

        modules.add(new TestRuntimeOverridesModule(unitDbAdapter));

        for (Class<?> moduleType : extraModules) {
            modules.add(instantiateModule(moduleType));
        }

        if (weakReferenceStrategy) {
            modules.add(new WeakReferenceStrategyModule());
        }

        return CayenneRuntime.builder()
                .addConfig(project)
                .addModules(modules)
                .build();
    }

    private static Module instantiateModule(Class<?> moduleType) {
        try {
            return (Module) moduleType.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate extra module: " + moduleType.getName(), e);
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

    public UnitDbAdapter unitDbAdapter() {
        return INJECTOR.getInstance(UnitDbAdapter.class);
    }

    public EntityResolver entityResolver() {
        return runtime.getDataDomain().getEntityResolver();
    }

    public DataNode dataNode() {
        DataDomain channel = runtime.getDataDomain();
        return channel.getDataNodes().iterator().next();
    }

    public void runWithQueriesBlocked(Runnable task) {
        RuntimeTelemetry.runWithQueriesBlocked(runtime, task);
    }

    public int runWithQueryCounter(Runnable task) {
        return RuntimeTelemetry.runWithQueryCounter(runtime, task);
    }

    public AdhocObjectFactory adhocObjectFactory() {
        return runtime.getInjector().getInstance(AdhocObjectFactory.class);
    }

    public RuntimeCaseDataSourceFactory dataSourceFactory() {
        return DATA_SOURCE_FACTORY;
    }

    public DbCleaner dbCleaner() {
        return dbCleaner;
    }

    public DataSourceDescriptor dataSourceDescriptor() {
        return INJECTOR.getInstance(DataSourceDescriptor.class);
    }

    public SQLTemplateCustomizer sqlTemplateCustomizer() {
        return INJECTOR.getInstance(SQLTemplateCustomizer.class);
    }

    public static class WeakReferenceStrategyModule implements Module {
        @Override
        public void configure(Binder binder) {
            CoreModule.extend(binder).setProperty(Constants.OBJECT_RETAIN_STRATEGY_PROPERTY, "weak");
        }
    }

    private record TestRuntimeOverridesModule(UnitDbAdapter unitDbAdapter) implements Module {

        @Override
        public void configure(Binder binder) {

            // TODO: factor it out of INJECTOR
            // a fresh DbAdapter per call — RuntimeCaseDbAdapterProvider is unscoped in the test injector
            binder.bind(DbAdapter.class).toProviderInstance(() -> INJECTOR.getInstance(DbAdapter.class));

            binder.bind(DataDomain.class).toProviderInstance(new RuntimeCaseDataDomainProvider(unitDbAdapter));
            binder.bind(DataNodeFactory.class).to(RuntimeCaseDataNodeFactory.class);
            binder.bind(UnitDbAdapter.class).toInstance(unitDbAdapter);
            binder.bind(RuntimeCaseDataSourceFactory.class).toInstance(DATA_SOURCE_FACTORY);

            CoreModule.extend(binder)
                    // Soft refs instead of weak — avoids GC-sensitive test flakiness.
                    .setProperty(Constants.OBJECT_RETAIN_STRATEGY_PROPERTY, "soft");
        }
    }
}
