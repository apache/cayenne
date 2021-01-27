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
package org.apache.cayenne.dba.postgres;

import org.apache.cayenne.di.AdhocObjectFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class PostgresAdapterIT extends ServerCase {
    
    @Inject
    private AdhocObjectFactory objectFactory;

    @Test
    public void testCreateTableWithFloatAttributeWithScale () {
        PostgresAdapter adapter = objectFactory.newInstance(
                PostgresAdapter.class, 
                PostgresAdapter.class.getName());
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

    @Test
    public void testCreateTableWithTimeAndTimestampAttributeWithScale() {
        PostgresAdapter adapter = objectFactory.newInstance(
                PostgresAdapter.class,
                PostgresAdapter.class.getName());
        DbEntity e = new DbEntity("Test");
        DbAttribute dblPrec = new DbAttribute("dbl1");
        dblPrec.setType(Types.TIMESTAMP);
        dblPrec.setMaxLength(-1);
        dblPrec.setScale(3);
        e.addAttribute(dblPrec);

        DbAttribute dblPrec2 = new DbAttribute("dbl2");
        dblPrec2.setType(Types.TIME);
        dblPrec2.setMaxLength(-1);
        dblPrec2.setScale(6);
        e.addAttribute(dblPrec2);

        String sql = adapter.createTable(e);

        // CAY-2694.
        assertTrue(sql.indexOf("time(6)") > 0);
        assertTrue(sql.indexOf("timestamp(3) with time zone") > 0);
        assertEquals("CREATE TABLE Test (dbl1 timestamp(3) with time zone NULL, dbl2 time(6) NULL)", sql);
    }

    @Test
    public void testCreateTableWithTimeAndTimestampAttributeWithoutScale() {
        PostgresAdapter adapter = objectFactory.newInstance(
                PostgresAdapter.class,
                PostgresAdapter.class.getName());
        DbEntity e = new DbEntity("Test");
        DbAttribute dblPrec = new DbAttribute("dbl1");
        dblPrec.setType(Types.TIMESTAMP);
        dblPrec.setMaxLength(-1);
        e.addAttribute(dblPrec);

        DbAttribute dblPrec2 = new DbAttribute("dbl2");
        dblPrec2.setType(Types.TIME);
        dblPrec2.setMaxLength(-1);
        e.addAttribute(dblPrec2);

        String sql = adapter.createTable(e);

        // CAY-2694.
        assertTrue(sql.indexOf("time") > 0);
        assertTrue(sql.indexOf("timestamp with time zone") > 0);
        assertEquals("CREATE TABLE Test (dbl1 timestamp with time zone NULL, dbl2 time NULL)", sql);

    }
}
