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
package org.apache.cayenne.tools.dbimport;

import static org.mockito.Mockito.mock;

import java.sql.Connection;

import junit.framework.TestCase;

import org.apache.cayenne.access.DbLoader;
import org.apache.cayenne.access.DbLoaderDelegate;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.tools.configuration.ToolsModule;
import org.apache.commons.logging.Log;

public class DbImportActionTest extends TestCase {

    public void testCreateLoader() throws Exception {
        Log log = mock(Log.class);
        Injector i = DIBootstrap.createInjector(new ToolsModule(log), new DbImportModule());

        DbImportAction action = i.getInstance(DbImportAction.class);

        DbImportParameters parameters = new DbImportParameters();

        Connection connection = mock(Connection.class);

        DbLoader loader = action.createLoader(parameters, mock(DbAdapter.class), connection,
                mock(DbLoaderDelegate.class));
        assertNotNull(loader);
        assertSame(connection, loader.getConnection());
        assertTrue(loader.includeTableName("dummy"));
    }

    public void testCreateLoader_IncludeExclude() throws Exception {
        Log log = mock(Log.class);
        Injector i = DIBootstrap.createInjector(new ToolsModule(log), new DbImportModule());

        DbImportAction action = i.getInstance(DbImportAction.class);

        DbImportParameters parameters = new DbImportParameters();
        parameters.setIncludeTables("a,b,c*");

        DbLoader loader1 = action.createLoader(parameters, mock(DbAdapter.class), mock(Connection.class),
                mock(DbLoaderDelegate.class));

        assertFalse(loader1.includeTableName("dummy"));
        assertFalse(loader1.includeTableName("ab"));
        assertTrue(loader1.includeTableName("a"));
        assertTrue(loader1.includeTableName("b"));
        assertTrue(loader1.includeTableName("cd"));
        
        parameters.setExcludeTables("cd");

        DbLoader loader2 = action.createLoader(parameters, mock(DbAdapter.class), mock(Connection.class),
                mock(DbLoaderDelegate.class));

        assertFalse(loader2.includeTableName("dummy"));
        assertFalse(loader2.includeTableName("ab"));
        assertTrue(loader2.includeTableName("a"));
        assertTrue(loader2.includeTableName("b"));
        assertFalse(loader2.includeTableName("cd"));
        assertTrue(loader2.includeTableName("cx"));
    }
}
