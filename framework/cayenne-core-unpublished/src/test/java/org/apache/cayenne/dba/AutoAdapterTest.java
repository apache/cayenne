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

package org.apache.cayenne.dba;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.jdbc.SQLTemplateAction;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.log.NoopJdbcEventLogger;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class AutoAdapterTest extends ServerCase {

    @Inject
    private DataNode dataNode;

    public void testGetAdapter_Proxy() throws Exception {
        Provider<DbAdapter> adapterProvider = mock(Provider.class);
        when(adapterProvider.get()).thenReturn(dataNode.getAdapter());

        AutoAdapter adapter = new AutoAdapter(
                adapterProvider,
                NoopJdbcEventLogger.getInstance());
        DbAdapter detected = adapter.getAdapter();
        assertSame(dataNode.getAdapter(), detected);
    }

    public void testCreateSQLTemplateAction() {

        Provider<DbAdapter> adapterProvider = mock(Provider.class);
        when(adapterProvider.get()).thenReturn(dataNode.getAdapter());

        AutoAdapter adapter = new AutoAdapter(
                adapterProvider,
                NoopJdbcEventLogger.getInstance());
        SQLTemplateAction action = (SQLTemplateAction) adapter.getAction(new SQLTemplate(
                Artist.class,
                "select * from artist"), dataNode);

        // it is important for SQLTemplateAction to be used with unwrapped adapter, as the
        // adapter class name is used as a key to the correct SQL template.
        assertNotNull(action.getAdapter());
        assertFalse(action.getAdapter() instanceof AutoAdapter);
        assertSame(dataNode.getAdapter(), action.getAdapter());
    }
}
