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

import org.apache.cayenne.unit.CayenneCase;

import com.mockrunner.mock.jdbc.MockConnection;
import com.mockrunner.mock.jdbc.MockDataSource;

public class AutoAdapterTest extends CayenneCase {

    public void testGetAdapter() {
        MockDbAdapter realAdapter = new MockDbAdapter();
        MockDbAdapterFactory factory = new MockDbAdapterFactory(realAdapter);

        MockDataSource dataSource = new MockDataSource();
        dataSource.setupConnection(new MockConnection());
        AutoAdapter adapter = new AutoAdapter(factory, dataSource);

        assertSame(realAdapter, adapter.getAdapter());
    }

    public void testGetDefaultAdapter() throws Exception {

        AutoAdapter adapter = new AutoAdapter(getNode().getDataSource());
        DbAdapter detected = adapter.getAdapter();

        assertNotNull(detected);
        assertEquals(getNode().getAdapter().getClass(), detected.getClass());
    }
}
