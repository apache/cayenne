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
package org.apache.cayenne.tools.configuration;

import static org.mockito.Mockito.mock;

import javax.sql.DataSource;

import junit.framework.TestCase;

import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.configuration.server.DataSourceFactory;
import org.apache.cayenne.configuration.server.DbAdapterFactory;
import org.apache.cayenne.configuration.server.DefaultDbAdapterFactory;
import org.apache.cayenne.dba.AutoAdapter;
import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.di.spi.DefaultAdhocObjectFactory;
import org.apache.commons.logging.Log;

public class ToolsModuleTest extends TestCase {

    public void testModuleContents() {

        Log log = mock(Log.class);
        Injector i = DIBootstrap.createInjector(new ToolsModule(log));

        assertSame(log, i.getInstance(Log.class));
        assertTrue(i.getInstance(DataSourceFactory.class) instanceof DriverDataSourceFactory);
        assertTrue(i.getInstance(AdhocObjectFactory.class) instanceof DefaultAdhocObjectFactory);
        assertTrue(i.getInstance(DbAdapterFactory.class) instanceof DefaultDbAdapterFactory);
    }

    public void testDbApdater() throws Exception {
        Log log = mock(Log.class);
        Injector i = DIBootstrap.createInjector(new ToolsModule(log));

        DbAdapterFactory factory = i.getInstance(DbAdapterFactory.class);

        DataNodeDescriptor nodeDescriptor = mock(DataNodeDescriptor.class);
        DataSource dataSource = mock(DataSource.class);

        assertTrue(factory.createAdapter(nodeDescriptor, dataSource) instanceof AutoAdapter);
    }
}
