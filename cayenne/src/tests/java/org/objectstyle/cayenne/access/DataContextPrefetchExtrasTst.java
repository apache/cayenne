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
package org.objectstyle.cayenne.access;

import java.util.List;

import org.objectstyle.art.CharFkTest;
import org.objectstyle.art.CharPkTest;
import org.objectstyle.art.CompoundFkTest;
import org.objectstyle.art.CompoundPkTest;
import org.objectstyle.cayenne.CayenneDataObject;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.Ordering;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * Test prefetching of various obscure cases.
 * 
 * @author Andrei Adamchik
 */
public class DataContextPrefetchExtrasTst extends CayenneTestCase {
    protected DataContext context;

    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
        context = createDataContext();
    }

    public void testPrefetchToManyOnCharKey() throws Exception {
        createTestData("testPrefetchToManyOnCharKey");

        SelectQuery q = new SelectQuery(CharPkTest.class);
        q.addPrefetch("charFKs");
        q.addOrdering(CharPkTest.OTHER_COL_PROPERTY, Ordering.ASC);

        List pks = context.performQuery(q);
        assertEquals(2, pks.size());

        CharPkTest pk1 = (CharPkTest) pks.get(0);
        assertEquals("n1", pk1.getOtherCol());
        ToManyList toMany = (ToManyList) pk1.readPropertyDirectly("charFKs");
        assertNotNull(toMany);
        assertFalse(toMany.needsFetch());
        assertEquals(3, toMany.size());

        CharFkTest fk1 = (CharFkTest) toMany.get(0);
        assertEquals(PersistenceState.COMMITTED, fk1.getPersistenceState());
        assertSame(pk1, fk1.getToCharPK());
    }

    /**
     * Tests to-one prefetching over relationships with compound keys.
     */
    public void testPrefetch10() throws Exception {
        createTestData("testCompound");

        Expression e = ExpressionFactory.matchExp("name", "CFK2");
        SelectQuery q = new SelectQuery(CompoundFkTest.class, e);
        q.addPrefetch("toCompoundPk");

        List objects = context.performQuery(q);
        assertEquals(1, objects.size());
        CayenneDataObject fk1 = (CayenneDataObject) objects.get(0);

        // resolving the fault must not result in extra queries, since
        // artist must have been prefetched
        DataContextDelegate delegate = new DefaultDataContextDelegate() {
            public GenericSelectQuery willPerformSelect(
                DataContext context,
                GenericSelectQuery query) {
                throw new CayenneRuntimeException(
                    "No query expected.. attempt to run: " + query);
            }
        };

        fk1.getDataContext().setDelegate(delegate);

        Object toOnePrefetch = fk1.readNestedProperty("toCompoundPk");
        assertNotNull(toOnePrefetch);
        assertTrue(
            "Expected DataObject, got: " + toOnePrefetch.getClass().getName(),
            toOnePrefetch instanceof DataObject);

        DataObject pk1 = (DataObject) toOnePrefetch;
        assertEquals(PersistenceState.COMMITTED, pk1.getPersistenceState());
        assertEquals("CPK2", pk1.readPropertyDirectly("name"));
    }

    /**
     * Tests to-many prefetching over relationships with compound keys.
     */
    public void testPrefetch11() throws Exception {
        createTestData("testCompound");

        Expression e = ExpressionFactory.matchExp("name", "CPK2");
        SelectQuery q = new SelectQuery(CompoundPkTest.class, e);
        q.addPrefetch("compoundFkArray");

        List pks = context.performQuery(q);
        assertEquals(1, pks.size());
        CayenneDataObject pk1 = (CayenneDataObject) pks.get(0);

        ToManyList toMany = (ToManyList) pk1.readPropertyDirectly("compoundFkArray");
        assertNotNull(toMany);
        assertFalse(toMany.needsFetch());
        assertEquals(2, toMany.size());

        CayenneDataObject fk1 = (CayenneDataObject) toMany.get(0);
        assertEquals(PersistenceState.COMMITTED, fk1.getPersistenceState());

        CayenneDataObject fk2 = (CayenneDataObject) toMany.get(1);
        assertEquals(PersistenceState.COMMITTED, fk2.getPersistenceState());
    }
}
