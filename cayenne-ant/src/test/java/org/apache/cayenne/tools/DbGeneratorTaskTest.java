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

package org.apache.cayenne.tools;

import org.apache.cayenne.dba.AutoAdapter;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.sqlserver.SQLServerAdapter;
import org.apache.cayenne.dbsync.reverse.configuration.ToolsModule;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.slf4j.Logger;
import org.junit.Test;

import javax.sql.DataSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class DbGeneratorTaskTest {

    @Test
    public void testSetUserName() throws Exception {
        DbGeneratorTask task = new DbGeneratorTask();
        task.setUserName("abc");
        assertEquals("abc", task.userName);
    }

    @Test
    public void testSetPassword() throws Exception {
        DbGeneratorTask task = new DbGeneratorTask();
        task.setPassword("xyz");
        assertEquals("xyz", task.password);
    }

    @Test
    public void testSetAdapter() throws Exception {
        DataSource ds = mock(DataSource.class);
        Injector injector = DIBootstrap.createInjector(new ToolsModule(mock(Logger.class)));

        DbGeneratorTask task = new DbGeneratorTask();

        DbAdapter autoAdapter = task.getAdapter(injector, ds);
        assertTrue(autoAdapter instanceof AutoAdapter);

        task.setAdapter(SQLServerAdapter.class.getName());

        DbAdapter sqlServerAdapter = task.getAdapter(injector, ds);
        assertTrue(sqlServerAdapter instanceof SQLServerAdapter);
    }

    @Test
    public void testSetUrl() throws Exception {
        DbGeneratorTask task = new DbGeneratorTask();
        task.setUrl("jdbc:///");
        assertEquals("jdbc:///", task.url);
    }
}
