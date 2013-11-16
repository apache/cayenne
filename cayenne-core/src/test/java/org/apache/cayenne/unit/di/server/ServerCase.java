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
package org.apache.cayenne.unit.di.server;

import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.spi.DefaultScope;
import org.apache.cayenne.unit.di.DICase;

public class ServerCase extends DICase {

    // known runtimes... unit tests may reuse these with @UseServerRuntime annotation or
    // can define their own on the fly (TODO: how would that work with the global schema
    // setup?)
    public static final String INHERTITANCE_SINGLE_TABLE1_PROJECT = "cayenne-inheritance-single-table1.xml";
    public static final String INHERTITANCE_VERTICAL_PROJECT = "cayenne-inheritance-vertical.xml";
    public static final String LOCKING_PROJECT = "cayenne-locking.xml";
    public static final String QUOTED_IDENTIFIERS_PROJECT = "cayenne-quoted-identifiers.xml";
    public static final String PEOPLE_PROJECT = "cayenne-people.xml";
    public static final String RELATIONSHIPS_PROJECT = "cayenne-relationships.xml";
    public static final String TESTMAP_PROJECT = "cayenne-testmap.xml";
    public static final String DEFAULT_PROJECT = "cayenne-default.xml";
    public static final String MULTINODE_PROJECT = "cayenne-multinode.xml";
    public static final String ONEWAY_PROJECT = "cayenne-oneway-rels.xml";

    private static final Injector injector;

    static {
        DefaultScope testScope = new DefaultScope();
        injector = DIBootstrap.createInjector(new ServerCaseModule(testScope));
        injector.getInstance(SchemaBuilder.class).rebuildSchema();
    }

    @Override
    protected Injector getUnitTestInjector() {
        return injector;
    }
}
