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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.spi.DefaultScope;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension that wires a Cayenne test environment without requiring
 * a base class or {@code @Inject} fields. Declare it as a static field and
 * use its getter methods from test methods and {@code @BeforeEach} callbacks:
 *
 * <pre>{@code
 * @RegisterExtension
 * static final CayenneTestsExt env = CayenneTestsExt.forProject(CayenneProjects.TESTMAP_PROJECT);
 *
 * @Test
 * void someTest() {
 *     ObjectContext ctx = env.context();
 * }
 * }</pre>
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

    private ObjectContext context;
    private DataContext dataContext;
    private DBHelper dbHelper;
    private CayenneRuntime runtime;

    private CayenneTestsEnv(String project, Class<?>[] extraModules, boolean autoClean) {
        this.project = project;
        this.extraModules = extraModules;
        this.autoClean = autoClean;
    }

    public static CayenneTestsEnv forProject(String project) {
        return new CayenneTestsEnv(project, new Class<?>[0], true);
    }

    public CayenneTestsEnv withExtraModules(Class<?>... modules) {
        return new CayenneTestsEnv(project, modules, autoClean);
    }

    /**
     * Disables the automatic {@code DBCleaner.clean()} call in {@link #beforeEach}.
     * Use this when the test (or a base class) needs to perform schema-specific
     * setup (e.g. break circular FKs) before cleaning. The caller is then
     * responsible for invoking {@code env.getInstance(DBCleaner.class).clean()}
     * from its own {@code @BeforeEach}.
     */
    public CayenneTestsEnv withoutAutoClean() {
        return new CayenneTestsEnv(project, extraModules, false);
    }

    @Override
    public void beforeEach(ExtensionContext ctx) throws Exception {
        INJECTOR.getInstance(RuntimeCaseProperties.class).setConfigurationLocation(project);
        INJECTOR.getInstance(RuntimeCaseExtraModules.class).setExtraModules(extraModules);

        runtime = INJECTOR.getInstance(CayenneRuntime.class);
        context = INJECTOR.getInstance(ObjectContext.class);
        dataContext = INJECTOR.getInstance(DataContext.class);
        dbHelper = INJECTOR.getInstance(DBHelper.class);

        if (autoClean) {
            DBCleaner dbCleaner = INJECTOR.getInstance(DBCleaner.class);
            try {
                dbCleaner.clean();
            } catch (Exception ex) {
                dbCleaner.clean();
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext ctx) {
        TEST_SCOPE.shutdown();
        context = null;
        dataContext = null;
        dbHelper = null;
        runtime = null;
    }

    public ObjectContext context() {
        return context;
    }

    public DataContext dataContext() {
        return dataContext;
    }

    public DBHelper dbHelper() {
        return dbHelper;
    }

    public TableHelper table(String tableName) {
        return new TableHelper(dbHelper, tableName);
    }

    public TableHelper table(String tableName, String... columns) {
        return new TableHelper(dbHelper, tableName, columns);
    }

    public CayenneRuntime runtime() {
        return runtime;
    }

    /**
     * Access to less-common injectable types (DataChannelInterceptor, UnitDbAdapter, etc.).
     */
    public <T> T getInstance(Class<T> type) {
        return INJECTOR.getInstance(type);
    }
}
