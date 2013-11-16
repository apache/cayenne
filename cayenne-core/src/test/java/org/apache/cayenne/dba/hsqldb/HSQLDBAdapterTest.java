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

package org.apache.cayenne.dba.hsqldb;

import java.sql.Types;

import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class HSQLDBAdapterTest extends ServerCase {
    
    @Inject
    private AdhocObjectFactory objectFactory;

    public void testCreateTableIgnoresDoublePrecision() {
        HSQLDBAdapter adapter = objectFactory.newInstance(
                HSQLDBAdapter.class, 
                HSQLDBAdapter.class.getName());
        DbEntity e = new DbEntity("Test");
        DbAttribute dblPrec = new DbAttribute("dbl1");
        dblPrec.setType(Types.DOUBLE);
        dblPrec.setMaxLength(22);
        e.addAttribute(dblPrec);
        
        String sql = adapter.createTable(e);

        // CAY-1095.
        // Make sure the double type is preserved, but not precision constraints, due to a bug in HSQLDB.
        assertTrue(sql.indexOf("DOUBLE") > 0);
        assertEquals(-1, sql.indexOf("DOUBLE(22)"));
    }
    
    public void testCreateTableAddsCachedKeyword() {
        HSQLDBAdapter adapter = objectFactory.newInstance(
                HSQLDBAdapter.class, 
                HSQLDBAdapter.class.getName());
        DbEntity e = new DbEntity("Test");
        DbAttribute dblPrec = new DbAttribute("dbl1");
        dblPrec.setType(Types.INTEGER);
        e.addAttribute(dblPrec);
        
        String sql = adapter.createTable(e);
        
        assertEquals(0,sql.indexOf("CREATE CACHED TABLE"));
    }
}
