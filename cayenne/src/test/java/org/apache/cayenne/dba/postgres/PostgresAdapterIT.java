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

import org.apache.cayenne.dba.NativeColumnType;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.Types;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PostgresAdapterIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void createTableWithFloatAttributeWithScale() {
        PostgresAdapter adapter = env.adhocObjectFactory().newInstance(
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
    public void createTableWithGeneratedColumnRendersSerial() {
        PostgresAdapter adapter = env.adhocObjectFactory().newInstance(
                PostgresAdapter.class,
                PostgresAdapter.class.getName());
        DbEntity e = new DbEntity("Test");
        DbAttribute id = new DbAttribute("id");
        id.setType(Types.INTEGER);
        id.setGenerated(true);
        id.setMandatory(true);
        e.addAttribute(id);

        String sql = adapter.createTable(e);

        // the auto-increment variant is selected for generated columns
        assertTrue(sql.contains("id serial"), sql);
    }

    @Test
    public void externalColumnTypesAndLegacyArray() {
        PostgresAdapter adapter = env.adhocObjectFactory().newInstance(
                PostgresAdapter.class,
                PostgresAdapter.class.getName());

        NativeColumnType[] variants = adapter.nativeColumnTypes(Types.INTEGER);
        assertEquals(2, variants.length);
        assertEquals("integer", variants[0].nativeType());
        assertFalse(variants[0].autoIncrement());
        assertEquals("serial", variants[1].nativeType());
        assertTrue(variants[1].autoIncrement());

        // the deprecated array view still reproduces the historical contents
        assertArrayEquals(new String[]{"integer", "serial"}, adapter.externalTypesForJdbcType(Types.INTEGER));
    }

}
