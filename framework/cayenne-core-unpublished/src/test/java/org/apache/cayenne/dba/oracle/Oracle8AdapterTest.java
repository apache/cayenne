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

package org.apache.cayenne.dba.oracle;

import java.net.URL;
import java.sql.Types;

import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class Oracle8AdapterTest extends ServerCase {
    
    @Inject
    private AdhocObjectFactory objectFactory;

    public void testTimestampMapping() throws Exception {
        
        Oracle8Adapter adapter = objectFactory.newInstance(
                Oracle8Adapter.class, 
                Oracle8Adapter.class.getName());

        String[] types = adapter.externalTypesForJdbcType(Types.TIMESTAMP);
        assertNotNull(types);
        assertEquals(1, types.length);
        assertEquals("DATE", types[0]);
    }

    public void testFindAdapterResource() throws Exception {
        
        Oracle8Adapter adapter = objectFactory.newInstance(
                Oracle8Adapter.class, 
                Oracle8Adapter.class.getName());

        URL typesURL = adapter.findResource("/types.xml");
        assertNotNull(typesURL);
        assertTrue("Unexpected url:" + typesURL, typesURL.toExternalForm().endsWith(
                "types-oracle8.xml"));
    }
}
