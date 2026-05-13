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
 * Sibling of {@link CayenneTestsExt} that wires its own injector via
 * {@link WeakReferenceStrategyRuntimeCaseModule}. Use it for tests that need
 * a runtime configured with the weak-reference object-tracking strategy.
 *
 * @since 5.0
 */
public class WeakReferenceStrategyTestsExt implements BeforeEachCallback, AfterEachCallback {

    private static final DefaultScope testScope;
    private static final Injector injector;

    static {
        testScope = new DefaultScope();
        injector = DIBootstrap.createInjector(new WeakReferenceStrategyRuntimeCaseModule(testScope));
        injector.getInstance(SchemaBuilder.class).rebuildSchema();
    }

    private final String project;
    private final Class<?>[] extraModules;

    private ObjectContext context;
    private DataContext dataContext;
    private DBHelper dbHelper;
    private CayenneRuntime runtime;

    private WeakReferenceStrategyTestsExt(String project, Class<?>[] extraModules) {
        this.project = project;
        this.extraModules = extraModules;
    }

    public static WeakReferenceStrategyTestsExt forProject(String project) {
        return new WeakReferenceStrategyTestsExt(project, new Class<?>[0]);
    }

    public WeakReferenceStrategyTestsExt withExtraModules(Class<?>... modules) {
        return new WeakReferenceStrategyTestsExt(project, modules);
    }

    @Override
    public void beforeEach(ExtensionContext ctx) throws Exception {
        injector.getInstance(RuntimeCaseProperties.class).setConfigurationLocation(project);
        injector.getInstance(RuntimeCaseExtraModules.class).setExtraModules(extraModules);

        runtime     = injector.getInstance(CayenneRuntime.class);
        context     = injector.getInstance(ObjectContext.class);
        dataContext = injector.getInstance(DataContext.class);
        dbHelper    = injector.getInstance(DBHelper.class);

        DBCleaner dbCleaner = injector.getInstance(DBCleaner.class);
        try {
            dbCleaner.clean();
        } catch (Exception ex) {
            dbCleaner.clean();
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

    public <T> T getInstance(Class<T> type) {
        return injector.getInstance(type);
    }
}
