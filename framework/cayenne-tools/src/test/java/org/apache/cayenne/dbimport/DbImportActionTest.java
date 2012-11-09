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
package org.apache.cayenne.dbimport;

import static org.mockito.Mockito.mock;

import javax.sql.DataSource;

import junit.framework.TestCase;

import org.apache.cayenne.configuration.server.DbAdapterFactory;
import org.apache.cayenne.dba.AutoAdapter;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.mysql.MySQLAdapter;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.tools.configuration.ToolsModule;
import org.apache.commons.logging.Log;

public class DbImportActionTest extends TestCase {

    public void testGetAdapter() throws Exception {

        Injector injector = DIBootstrap.createInjector(new ToolsModule(mock(Log.class)));

        DbImportAction action = new DbImportAction(injector.getInstance(Log.class),
                injector.getInstance(DbAdapterFactory.class));

        DataSource ds = mock(DataSource.class);

        DbAdapter adapter = action.getAdapter(null, ds);
        assertNotNull(adapter);
        assertTrue(adapter instanceof AutoAdapter);

        DbAdapter adapter2 = action.getAdapter(MySQLAdapter.class.getName(), ds);
        assertNotNull(adapter2);
        assertTrue(adapter2 instanceof MySQLAdapter);
    }
}
