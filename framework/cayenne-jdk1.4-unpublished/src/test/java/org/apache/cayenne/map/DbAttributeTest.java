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

package org.apache.cayenne.map;

import java.sql.Types;
import java.util.List;

import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.unit.CayenneCase;

/**
 * @author Andrus Adamchik
 */
public class DbAttributeTest extends CayenneCase {

    public void testConstructor1() throws Exception {
        DbAttribute a = new DbAttribute("abc");
        assertEquals("abc", a.getName());
        assertEquals(TypesMapping.NOT_DEFINED, a.getType());
        assertNull(a.getEntity());
    }

    public void testConstructor2() throws Exception {
        int type = Types.INTEGER;
        DbEntity dbe = new DbEntity("e");
        DbAttribute a = new DbAttribute("abc", type, dbe);
        assertEquals("abc", a.getName());
        assertEquals(type, a.getType());
        assertSame(dbe, a.getEntity());
    }

    public void testPrimaryKeyEmpty() {
        DbEntity dbe = new DbEntity("e");
        assertNotNull(dbe.getPrimaryKey());

        DbAttribute a = new DbAttribute("abc", Types.INTEGER, dbe);
        dbe.addAttribute(a);
        assertNotNull(dbe.getPrimaryKey());
        assertEquals(0, dbe.getPrimaryKey().size());
    }

    public void testPrimaryKeyAdded() {
        DbEntity dbe = new DbEntity("e");
        DbAttribute a = new DbAttribute("abc", Types.INTEGER, dbe);
        a.setPrimaryKey(true);
        dbe.addAttribute(a);
        List pk = dbe.getPrimaryKey();
        assertNotNull(pk);
        assertEquals(1, pk.size());
        assertEquals(pk, dbe.getPrimaryKey());
    }

    public void testPrimaryKeyAttributeChanged() {
        DbEntity dbe = new DbEntity("e");
        DbAttribute a = new DbAttribute("abc", Types.INTEGER, dbe);
        dbe.addAttribute(a);
        List pk = dbe.getPrimaryKey();
        assertNotNull(pk);
        assertEquals(0, pk.size());
        assertEquals(pk, dbe.getPrimaryKey());

        a.setPrimaryKey(true);
        pk = dbe.getPrimaryKey();
        assertNotNull(pk);
        assertEquals(1, pk.size());
        assertEquals(pk, dbe.getPrimaryKey());
    }

    public void testPrimaryKeyRemoved() {
        DbEntity dbe = new DbEntity("e");
        DbAttribute a = new DbAttribute("abc", Types.INTEGER, dbe);
        a.setPrimaryKey(true);
        dbe.addAttribute(a);
        List pk = dbe.getPrimaryKey();
        assertNotNull(pk);
        assertEquals(1, pk.size());
        assertEquals(pk, dbe.getPrimaryKey());

        dbe.removeAttribute(a.getName());
        pk = dbe.getPrimaryKey();
        assertNotNull(pk);
        assertEquals(0, pk.size());
        assertEquals(pk, dbe.getPrimaryKey());
    }

    public void testAttributesCleared() {
        DbEntity dbe = new DbEntity("e");
        DbAttribute a = new DbAttribute("abc", Types.INTEGER, dbe);
        a.setPrimaryKey(true);
        dbe.addAttribute(a);
        List pk = dbe.getPrimaryKey();
        assertNotNull(pk);
        assertEquals(1, pk.size());
        assertEquals(pk, dbe.getPrimaryKey());

        dbe.clearAttributes();
        pk = dbe.getPrimaryKey();
        assertNotNull(pk);
        assertEquals(0, pk.size());
        assertEquals(pk, dbe.getPrimaryKey());
    }

    public void testAutoIncrement() throws Exception {
        DbAttribute attribute = new DbAttribute();
        assertFalse(attribute.isGenerated());

        attribute.setGenerated(true);
        assertTrue(attribute.isGenerated());

        attribute.setGenerated(false);
        assertFalse(attribute.isGenerated());
    }
}
