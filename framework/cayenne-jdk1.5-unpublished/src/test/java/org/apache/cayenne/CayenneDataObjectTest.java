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

import junit.framework.TestCase;

import org.apache.art.Artist;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.unit.util.TestBean;

public class CayenneDataObjectTest extends TestCase {

    public void testSetObjectId() throws Exception {
        CayenneDataObject obj = new CayenneDataObject();
        ObjectId oid = new ObjectId("T");

        assertNull(obj.getObjectId());

        obj.setObjectId(oid);
        assertSame(oid, obj.getObjectId());
    }

    public void testSetPersistenceState() throws Exception {
        CayenneDataObject obj = new CayenneDataObject();
        assertEquals(PersistenceState.TRANSIENT, obj.getPersistenceState());

        obj.setPersistenceState(PersistenceState.COMMITTED);
        assertEquals(PersistenceState.COMMITTED, obj.getPersistenceState());
    }

    /**
     * @deprecated since 3.0.
     */
    public void testSetDataContext() throws Exception {
        CayenneDataObject obj = new CayenneDataObject();
        assertNull(obj.getDataContext());

        DataContext c = new DataContext();
        obj.setDataContext(c);
        assertSame(c, obj.getDataContext());
    }

    public void testReadNestedProperty1() throws Exception {
        Artist a = new Artist();
        assertNull(a.readNestedProperty("artistName"));
        a.setArtistName("aaa");
        assertEquals("aaa", a.readNestedProperty("artistName"));
    }

    public void testReadNestedPropertyNotPersistentString() throws Exception {
        Artist a = new Artist();
        assertNull(a.readNestedProperty("someOtherProperty"));
        a.setSomeOtherProperty("aaa");
        assertEquals("aaa", a.readNestedProperty("someOtherProperty"));
    }

    public void testReadNestedPropertyNonPersistentNotString() throws Exception {
        Artist a = new Artist();
        Object object = new Object();
        assertNull(a.readNestedProperty("someOtherObjectProperty"));
        a.setSomeOtherObjectProperty(object);
        assertSame(object, a.readNestedProperty("someOtherObjectProperty"));
    }

    public void testReadNestedPropertyNonDataObjectPath() {
        CayenneDataObject o1 = new CayenneDataObject();
        TestBean o2 = new TestBean();
        o2.setInteger(new Integer(55));
        o1.writePropertyDirectly("o2", o2);

        assertSame(o2, o1.readNestedProperty("o2"));
        assertEquals(new Integer(55), o1.readNestedProperty("o2.integer"));
        assertEquals(TestBean.class, o1.readNestedProperty("o2.class"));
        assertEquals(TestBean.class.getName(), o1.readNestedProperty("o2.class.name"));
    }
}
