/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.remote.hessian.service.HessianUtil;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class DataRowTst extends CayenneTestCase {

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

        Class entityClass = Number.class;
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