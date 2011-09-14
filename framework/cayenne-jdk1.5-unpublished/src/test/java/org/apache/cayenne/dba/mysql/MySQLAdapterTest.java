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

package org.apache.cayenne.dba.mysql;

import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class MySQLAdapterTest extends ServerCase {
    
    @Inject
    private AdhocObjectFactory objectFactory;

    public void testCreateTableAppendPKClause() {
        MySQLAdapter adapter = objectFactory.newInstance(
                MySQLAdapter.class, 
                MySQLAdapter.class.getName());

        DbEntity e = new DbEntity("Test");
        DbAttribute pk1 = new DbAttribute("PK1");
        pk1.setPrimaryKey(true);
        e.addAttribute(pk1);

        DbAttribute pk2 = new DbAttribute("PK2");
        pk2.setPrimaryKey(true);
        e.addAttribute(pk2);

        StringBuffer b1 = new StringBuffer();
        adapter.createTableAppendPKClause(b1, e);

        assertTrue(b1.indexOf("PK1") > 0);
        assertTrue(b1.indexOf("PK2") > 0);
        assertTrue(b1.indexOf("PK1") < b1.indexOf("PK2"));

        pk2.setGenerated(true);
        
        StringBuffer b2 = new StringBuffer();
        adapter.createTableAppendPKClause(b2, e);

        assertTrue(b2.indexOf("PK1") > 0);
        assertTrue(b2.indexOf("PK2") > 0);
        assertTrue(b2.indexOf("PK1") > b2.indexOf("PK2"));
    }
}
