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
package org.apache.cayenne.dba.sqlserver;

import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class SQLServerAdapterIT extends RuntimeCase {
    
    @Inject
    private AdhocObjectFactory objectFactory;

    @Test
    public void testCreateTableWithFloatAttributeWithScale () {
        SQLServerAdapter adapter = objectFactory.newInstance(
                SQLServerAdapter.class, 
                SQLServerAdapter.class.getName());
        DbEntity e = new DbEntity("Test");
        DbAttribute dblPrec = new DbAttribute("dbl1");
        dblPrec.setType(Types.FLOAT);
        dblPrec.setMaxLength(22);
        dblPrec.setScale(12);
        e.addAttribute(dblPrec);
        
        String sql = adapter.createTable(e);

        // CAY-1363.
        // Postgress don't support notations float(a, b) 
        assertTrue(sql.indexOf("float(22)") > 0);
        assertEquals(-1, sql.indexOf("float(22, 12)"));
        assertEquals("CREATE TABLE Test (dbl1 float(22) NULL)", sql);
    }
}
