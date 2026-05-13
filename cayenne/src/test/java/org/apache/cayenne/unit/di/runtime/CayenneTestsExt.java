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
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.spi.DefaultScope;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.DBHelper;
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
 *
 * @since 5.0
 */
public class CayenneTestsExt implements BeforeEachCallback, AfterEachCallback {

    // Reuse RuntimeCase's shared injector and scope — schema is built exactly once.
    // Referencing RuntimeCase here triggers its static initialization if not yet done.
    private static final DefaultScope testScope = RuntimeCase.testScope;
    private static final Injector injector = RuntimeCase.injector;

    private final String project;
    private final Class<?>[] extraModules;
    private final boolean autoClean;

    private ObjectContext context;
    private DataContext dataContext;
    private DBHelper dbHelper;
    private CayenneRuntime runtime;

    private CayenneTestsExt(String project, Class<?>[] extraModules, boolean autoClean) {
        this.project = project;
        this.extraModules = extraModules;
        this.autoClean = autoClean;
    }

    public static CayenneTestsExt forProject(String project) {
        return new CayenneTestsExt(project, new Class<?>[0], true);
    }

    public CayenneTestsExt withExtraModules(Class<?>... modules) {
        return new CayenneTestsExt(project, modules, autoClean);
    }

    /**
     * Disables the automatic {@code DBCleaner.clean()} call in {@link #beforeEach}.
     * Use this when the test (or a base class) needs to perform schema-specific
     * setup (e.g. break circular FKs) before cleaning. The caller is then
     * responsible for invoking {@code env.getInstance(DBCleaner.class).clean()}
     * from its own {@code @BeforeEach}.
     */
    public CayenneTestsExt withoutAutoClean() {
        return new CayenneTestsExt(project, extraModules, false);
    }

    @Override
    public void beforeEach(ExtensionContext ctx) throws Exception {
        injector.getInstance(RuntimeCaseProperties.class).setConfigurationLocation(project);
        injector.getInstance(RuntimeCaseExtraModules.class).setExtraModules(extraModules);

        runtime     = injector.getInstance(CayenneRuntime.class);
        context     = injector.getInstance(ObjectContext.class);
        dataContext = injector.getInstance(DataContext.class);
        dbHelper    = injector.getInstance(DBHelper.class);

        if (autoClean) {
            DBCleaner dbCleaner = injector.getInstance(DBCleaner.class);
            try {
                dbCleaner.clean();
            } catch (Exception ex) {
                dbCleaner.clean();
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext ctx) {
        testScope.shutdown();
        context     = null;
        dataContext = null;
        dbHelper    = null;
        runtime     = null;
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

    public CayenneRuntime runtime() {
        return runtime;
    }

    /** Access to less-common injectable types (DataChannelInterceptor, UnitDbAdapter, etc.). */
    public <T> T getInstance(Class<T> type) {
        return injector.getInstance(type);
    }
}
