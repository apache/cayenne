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

import junit.framework.TestCase;

import org.objectstyle.art.oneway.Artist;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.unit.util.TestBean;

public class CayenneDataObjectTst extends TestCase {

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
