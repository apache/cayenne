/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.jcache.unit;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.di.spi.DefaultScope;
import org.apache.cayenne.unit.di.DICase;
import org.apache.cayenne.unit.di.server.SchemaBuilder;
import org.apache.cayenne.unit.di.server.ServerCaseModule;
import org.junit.BeforeClass;

/**
 * @since 4.0
 */
public class JCacheCase extends DICase {

    private static Injector injector;

    @BeforeClass
    static public void setupInjector() {
        final DefaultScope testScope = new DefaultScope();
        injector = DIBootstrap.createInjector(
                new ServerCaseModule(testScope),
                new Module() {
                    @Override
                    public void configure(Binder binder) {
                        binder.bind(ServerRuntime.class).toProvider(CacheServerRuntimeProvider.class).in(testScope);
                    }
                }
        );
        injector.getInstance(SchemaBuilder.class).rebuildSchema();
    }

    @Override
    protected Injector getUnitTestInjector() {
        return injector;
    }
}
