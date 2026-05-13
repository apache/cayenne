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
package org.apache.cayenne.unit.di.runtime;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.DataSourceDescriptor;
import org.apache.cayenne.configuration.runtime.CoreModule;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultScope;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.DataChannelInterceptor;
import org.apache.cayenne.unit.util.SQLTemplateCustomizer;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension that wires a Cayenne test environment.
 */
public class CayenneTestsEnv implements BeforeEachCallback, AfterEachCallback {

    private static final DefaultScope TEST_SCOPE;
    private static final Injector INJECTOR;

    static {
        TEST_SCOPE = new DefaultScope();
        INJECTOR = DIBootstrap.createInjector(new RuntimeCaseModule(TEST_SCOPE));
        INJECTOR.getInstance(SchemaBuilder.class).rebuildSchema();
    }

    private final String project;
    private final Class<?>[] extraModules;
    private final boolean autoClean;
    private final boolean weakReferenceStrategy;

    private DataContext context;
    private DBHelper dbHelper;
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
    public void beforeEach(ExtensionContext ctx) throws Exception {
        INJECTOR.getInstance(RuntimeCaseProperties.class).setConfigurationLocation(project);

        Class<?>[] effectiveExtras = extraModules;
        if (weakReferenceStrategy) {
            effectiveExtras = new Class<?>[extraModules.length + 1];
            System.arraycopy(extraModules, 0, effectiveExtras, 0, extraModules.length);
            effectiveExtras[extraModules.length] = WeakReferenceStrategyModule.class;
        }
        INJECTOR.getInstance(RuntimeCaseExtraModules.class).setExtraModules(effectiveExtras);

        this.runtime = INJECTOR.getInstance(CayenneRuntime.class);
        this.context = (DataContext) runtime.newContext();
        this.dbHelper = INJECTOR.getInstance(DBHelper.class);

        if (autoClean) {
            DBCleaner cleaner = dbCleaner();
            try {
                cleaner.clean();
            } catch (Exception ex) {
                cleaner.clean();
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext ctx) {
        TEST_SCOPE.shutdown();
        this.context = null;
        this.dbHelper = null;
        this.runtime = null;
    }

    public DataContext context() {
        return context;
    }

    public DBHelper dbHelper() {
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

    public DbAdapter dbAdapter() {
        return INJECTOR.getInstance(DbAdapter.class);
    }

    public EntityResolver entityResolver() {
        return runtime.getDataDomain().getEntityResolver();
    }

    public DataNode dataNode() {
        DataDomain channel = runtime.getDataDomain();
        return channel.getDataNodes().iterator().next();
    }

    public DataChannelInterceptor dataChannelInterceptor() {
        return INJECTOR.getInstance(DataChannelInterceptor.class);
    }

    public AdhocObjectFactory adhocObjectFactory() {
        return runtime.getInjector().getInstance(AdhocObjectFactory.class);
    }

    public RuntimeCaseDataSourceFactory dataSourceFactory() {
        return INJECTOR.getInstance(RuntimeCaseDataSourceFactory.class);
    }

    public DBCleaner dbCleaner() {
        return INJECTOR.getInstance(DBCleaner.class);
    }

    public DataSourceDescriptor dataSourceDescriptor() {
        return INJECTOR.getInstance(DataSourceDescriptor.class);
    }

    public SQLTemplateCustomizer sqlTemplateCustomizer() {
        return INJECTOR.getInstance(SQLTemplateCustomizer.class);
    }

    public SchemaBuilder schemaBuilder() {
        return INJECTOR.getInstance(SchemaBuilder.class);
    }

    public static class WeakReferenceStrategyModule implements Module {
        @Override
        public void configure(Binder binder) {
            CoreModule.extend(binder).setProperty(Constants.OBJECT_RETAIN_STRATEGY_PROPERTY, "weak");
        }
    }
}
