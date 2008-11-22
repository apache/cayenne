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

package org.apache.cayenne;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.remote.hessian.service.HessianUtil;
import org.apache.cayenne.unit.CayenneCase;

/**
 */
public class DataRowTest extends CayenneCase {

    public void testHessianSerializability() throws Exception {
        DataRow s1 = new DataRow(10);
        s1.put("a", "b");

        DataRow s2 = (DataRow) HessianUtil.cloneViaServerClientSerialization(
                s1,
                new EntityResolver());

        assertNotSame(s1, s2);
        assertEquals(s1, s2);
        assertEquals(s1.getVersion(), s2.getVersion());
        assertEquals(s1.getReplacesVersion(), s2.getReplacesVersion());

        // at the moment there are no serializers that can go from client to server.
        // DataRow s3 = (DataRow) HessianUtil.cloneViaClientServerSerialization(
        // s1,
        // new EntityResolver());
        //
        // assertNotSame(s1, s3);
        // assertEquals(s1, s3);
    }

    public void testVersion() throws Exception {
        DataRow s1 = new DataRow(10);
        DataRow s2 = new DataRow(10);
        DataRow s3 = new DataRow(10);
        assertFalse(s1.getVersion() == s2.getVersion());
        assertFalse(s2.getVersion() == s3.getVersion());
        assertFalse(s3.getVersion() == s1.getVersion());
    }

    /**
     * @deprecated since 3.0 - unused.
     */
    public void testCreateObjectId() throws Exception {
        // must provide a map container for the entities
        DataMap entityContainer = new DataMap();

        ObjEntity objEntity = new ObjEntity("456");
        entityContainer.addObjEntity(objEntity);

        DbEntity dbe = new DbEntity("123");
        objEntity.setDbEntityName("123");
        entityContainer.addDbEntity(dbe);

        DbAttribute at = new DbAttribute("xyz");
        at.setPrimaryKey(true);
        dbe.addAttribute(at);

        Class<?> entityClass = Number.class;
        objEntity.setClassName(entityClass.getName());

        // test same id created by different methods
        DataRow map = new DataRow(10);
        map.put(at.getName(), "123");

        DataRow map2 = new DataRow(10);
        map2.put(at.getName(), "123");

        ObjectId ref = new ObjectId(objEntity.getName(), map);
        ObjectId oid = map2.createObjectId(objEntity);

        assertEquals(ref, oid);
    }

    /**
     * @deprecated since 3.0 - unused.
     */
    public void testCreateObjectIdNulls() throws Exception {
        // must provide a map container for the entities
        DataMap entityContainer = new DataMap();

        DbEntity dbe = new DbEntity("123");
        entityContainer.addDbEntity(dbe);

        DbAttribute at = new DbAttribute("xyz");
        at.setPrimaryKey(true);
        dbe.addAttribute(at);

        // assert that data row is smart enough to throw on null ids...
        DataRow map = new DataRow(10);
        try {
            map.createObjectId("T", dbe);
            fail("Must have failed... Null pk");
        }
        catch (CayenneRuntimeException ex) {
            // expected...
        }
    }
}
