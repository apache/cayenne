/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectstyle.art.Artist;
import org.objectstyle.art.CharPkTest;
import org.objectstyle.art.CompoundPkTest;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class DataObjectUtilsTst extends CayenneTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testNoObjectForPK() throws Exception {
        createTestData("testObjectForPKInt");
        DataContext context = createDataContext();

        // use bogus non-existent PK
        DataObject object = DataObjectUtils.objectForPK(context, Artist.class, 44001);
        assertNull(object);
    }

    public void testObjectForPKObjectId() throws Exception {
        createTestData("testObjectForPKInt");
        DataContext context = createDataContext();

        DataObject object = DataObjectUtils.objectForPK(context, new ObjectId(
                Artist.class,
                Artist.ARTIST_ID_PK_COLUMN,
                33002));

        assertNotNull(object);
        assertTrue(object instanceof Artist);
        assertEquals("artist2", ((Artist) object).getArtistName());
    }

    public void testObjectForPKClassInt() throws Exception {
        createTestData("testObjectForPKInt");
        DataContext context = createDataContext();

        DataObject object = DataObjectUtils.objectForPK(context, Artist.class, 33002);

        assertNotNull(object);
        assertTrue(object instanceof Artist);
        assertEquals("artist2", ((Artist) object).getArtistName());
    }

    public void testObjectForPKEntityInt() throws Exception {
        createTestData("testObjectForPKInt");
        DataContext context = createDataContext();

        DataObject object = DataObjectUtils.objectForPK(context, "Artist", 33002);

        assertNotNull(object);
        assertTrue(object instanceof Artist);
        assertEquals("artist2", ((Artist) object).getArtistName());
    }

    public void testObjectForPKClassMap() throws Exception {
        createTestData("testObjectForPKInt");
        DataContext context = createDataContext();

        Map pk = Collections.singletonMap(Artist.ARTIST_ID_PK_COLUMN, new Integer(33002));
        DataObject object = DataObjectUtils.objectForPK(context, Artist.class, pk);

        assertNotNull(object);
        assertTrue(object instanceof Artist);
        assertEquals("artist2", ((Artist) object).getArtistName());
    }

    public void testObjectForPKEntityMapCompound() throws Exception {
        createTestData("testObjectForPKCompound");
        DataContext context = createDataContext();

        Map pk = new HashMap();
        pk.put(CompoundPkTest.KEY1_PK_COLUMN, "PK1");
        pk.put(CompoundPkTest.KEY2_PK_COLUMN, "PK2");
        DataObject object = DataObjectUtils
                .objectForPK(context, CompoundPkTest.class, pk);

        assertNotNull(object);
        assertTrue(object instanceof CompoundPkTest);
        assertEquals("BBB", ((CompoundPkTest) object).getName());
    }

    public void testCompoundPKForObject() throws Exception {
        createTestData("testCompoundPKForObject");

        DataContext context = createDataContext();
        List objects = context.performQuery(new SelectQuery(CompoundPkTest.class));
        assertEquals(1, objects.size());
        DataObject object = (DataObject) objects.get(0);

        Map pk = DataObjectUtils.compoundPKForObject(object);
        assertNotNull(pk);
        assertEquals(2, pk.size());
        assertEquals("PK1", pk.get(CompoundPkTest.KEY1_PK_COLUMN));
        assertEquals("PK2", pk.get(CompoundPkTest.KEY2_PK_COLUMN));
    }

    public void testIntPKForObjectFailureForCompound() throws Exception {
        createTestData("testCompoundPKForObject");

        DataContext context = createDataContext();
        List objects = context.performQuery(new SelectQuery(CompoundPkTest.class));
        assertEquals(1, objects.size());
        DataObject object = (DataObject) objects.get(0);

        try {
            DataObjectUtils.intPKForObject(object);
            fail("intPKForObject must fail for compound key");
        }
        catch (CayenneRuntimeException ex) {

        }
    }

    public void testIntPKForObjectFailureForNonNumeric() throws Exception {
        createTestData("testIntPKForObjectNonNumeric");

        DataContext context = createDataContext();
        List objects = context.performQuery(new SelectQuery(CharPkTest.class));
        assertEquals(1, objects.size());
        DataObject object = (DataObject) objects.get(0);

        try {
            DataObjectUtils.intPKForObject(object);
            fail("intPKForObject must fail for non-numeric key");
        }
        catch (CayenneRuntimeException ex) {

        }
    }

    public void testPKForObjectFailureForCompound() throws Exception {
        createTestData("testCompoundPKForObject");

        DataContext context = createDataContext();
        List objects = context.performQuery(new SelectQuery(CompoundPkTest.class));
        assertEquals(1, objects.size());
        DataObject object = (DataObject) objects.get(0);

        try {
            DataObjectUtils.pkForObject(object);
            fail("pkForObject must fail for compound key");
        }
        catch (CayenneRuntimeException ex) {

        }
    }

    public void testIntPKForObject() throws Exception {
        createTestData("testIntPKForObject");

        DataContext context = createDataContext();
        List objects = context.performQuery(new SelectQuery(Artist.class));
        assertEquals(1, objects.size());
        DataObject object = (DataObject) objects.get(0);

        assertEquals(33001, DataObjectUtils.intPKForObject(object));
    }

    public void testPKForObject() throws Exception {
        createTestData("testIntPKForObject");

        DataContext context = createDataContext();
        List objects = context.performQuery(new SelectQuery(Artist.class));
        assertEquals(1, objects.size());
        DataObject object = (DataObject) objects.get(0);

        assertEquals(new Integer(33001), DataObjectUtils.pkForObject(object));
    }

    public void testIntPKForObjectNonNumeric() throws Exception {
        createTestData("testIntPKForObjectNonNumeric");

        DataContext context = createDataContext();
        List objects = context.performQuery(new SelectQuery(CharPkTest.class));
        assertEquals(1, objects.size());
        DataObject object = (DataObject) objects.get(0);

        assertEquals("CPK", DataObjectUtils.pkForObject(object));
    }
}