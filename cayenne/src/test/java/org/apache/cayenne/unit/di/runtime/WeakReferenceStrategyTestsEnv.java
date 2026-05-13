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
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Sibling of {@link CayenneTestsEnv} that wires its own injector via
 * {@link WeakReferenceStrategyRuntimeCaseModule}. Use it for tests that need
 * a runtime configured with the weak-reference object-tracking strategy.
 */
// TODO: merge with CayenneTestsEnv
@Deprecated(forRemoval = true)
public class WeakReferenceStrategyTestsEnv implements BeforeEachCallback, AfterEachCallback {

    private static final DefaultScope TEST_SCOPE;
    private static final Injector INJECTOR;

    static {
        TEST_SCOPE = new DefaultScope();
        INJECTOR = DIBootstrap.createInjector(new WeakReferenceStrategyRuntimeCaseModule(TEST_SCOPE));
        INJECTOR.getInstance(SchemaBuilder.class).rebuildSchema();
    }

    private final String project;
    private final Class<?>[] extraModules;

    private ObjectContext context;
    private DataContext dataContext;
    private DBHelper dbHelper;
    private CayenneRuntime runtime;

    private WeakReferenceStrategyTestsEnv(String project, Class<?>[] extraModules) {
        this.project = project;
        this.extraModules = extraModules;
    }

    public static WeakReferenceStrategyTestsEnv forProject(String project) {
        return new WeakReferenceStrategyTestsEnv(project, new Class<?>[0]);
    }

    public WeakReferenceStrategyTestsEnv withExtraModules(Class<?>... modules) {
        return new WeakReferenceStrategyTestsEnv(project, modules);
    }

    @Override
    public void beforeEach(ExtensionContext ctx) throws Exception {
        INJECTOR.getInstance(RuntimeCaseProperties.class).setConfigurationLocation(project);
        INJECTOR.getInstance(RuntimeCaseExtraModules.class).setExtraModules(extraModules);

        runtime = INJECTOR.getInstance(CayenneRuntime.class);
        context = INJECTOR.getInstance(ObjectContext.class);
        dataContext = INJECTOR.getInstance(DataContext.class);
        dbHelper = INJECTOR.getInstance(DBHelper.class);

        DBCleaner dbCleaner = INJECTOR.getInstance(DBCleaner.class);
        try {
            dbCleaner.clean();
        } catch (Exception ex) {
            dbCleaner.clean();
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

    public CayenneRuntime runtime() {
        return runtime;
    }

    public <T> T getInstance(Class<T> type) {
        return INJECTOR.getInstance(type);
    }
}
