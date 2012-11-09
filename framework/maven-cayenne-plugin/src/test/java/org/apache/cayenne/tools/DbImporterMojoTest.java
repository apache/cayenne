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
package org.apache.cayenne.tools;

import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;

import javax.sql.DataSource;

import junit.framework.TestCase;

import org.apache.cayenne.dba.AutoAdapter;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.mysql.MySQLAdapter;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.tools.configuration.ToolsModule;

public class DbImporterMojoTest extends TestCase {

    public void testGetAdapter() throws Exception {

        DbImporterMojo mojo = new DbImporterMojo();

        DataSource ds = mock(DataSource.class);
        Injector injector = DIBootstrap.createInjector(new ToolsModule());
        DbAdapter adapter = mojo.getAdapter(injector, ds);
        assertNotNull(adapter);
        assertTrue(adapter instanceof AutoAdapter);

        Field adapterField = mojo.getClass().getDeclaredField("adapter");
        adapterField.setAccessible(true);
        adapterField.set(mojo, MySQLAdapter.class.getName());
        
        DbAdapter adapter2 = mojo.getAdapter(injector, ds);
        assertNotNull(adapter2);
        assertTrue(adapter2 instanceof MySQLAdapter);
    }
}
