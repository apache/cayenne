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

import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.MockupDataRowUtils;
import org.objectstyle.cayenne.access.ToManyList;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.SQLTemplate;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.testdo.relationship.FlattenedTest1;
import org.objectstyle.cayenne.testdo.relationship.FlattenedTest2;
import org.objectstyle.cayenne.testdo.relationship.FlattenedTest3;
import org.objectstyle.cayenne.unit.RelationshipTestCase;

/**
 * Test case for objects with flattened relationships.
 * 
 * @author Andrei Adamchik
 */
public class FlattenedRelationshipsTst extends RelationshipTestCase {

    protected DataContext context;

    protected void setUp() throws Exception {
        deleteTestData();
        context = createDataContext();
    }

    public void testInsertJoinWithPK() throws Exception {
        FlattenedTest1 obj01 = (FlattenedTest1) context
                .createAndRegisterNewObject(FlattenedTest1.class);
        FlattenedTest3 obj11 = (FlattenedTest3) context
                .createAndRegisterNewObject(FlattenedTest3.class);
        FlattenedTest3 obj12 = (FlattenedTest3) context
                .createAndRegisterNewObject(FlattenedTest3.class);

        obj01.setName("t01");
        obj11.setName("t11");
        obj12.setName("t12");

        obj01.addToFt3OverComplex(obj11);
        obj01.addToFt3OverComplex(obj12);

        context.commitChanges();

        int pk = DataObjectUtils.intPKForObject(obj01);

        context = createDataContext();
        FlattenedTest1 fresh01 = (FlattenedTest1) DataObjectUtils.objectForPK(
                context,
                FlattenedTest1.class,
                pk);

        assertEquals("t01", fresh01.getName());
        ToManyList related = (ToManyList) fresh01.getFt3OverComplex();
        assertTrue(related.needsFetch());

        assertEquals(2, related.size());
    }

    public void testUnsetJoinWithPK() throws Exception {
        createTestData("testUnsetJoinWithPK");

        SQLTemplate joinSelect = new SQLTemplate(
                FlattenedTest1.class,
                "SELECT * FROM COMPLEX_JOIN",
                true);
        joinSelect.setFetchingDataRows(true);
        assertEquals(3, context.performQuery(joinSelect).size());

        FlattenedTest1 ft1 = (FlattenedTest1) DataObjectUtils.objectForPK(
                context,
                FlattenedTest1.class,
                2);

        assertEquals("ft12", ft1.getName());
        ToManyList related = (ToManyList) ft1.getFt3OverComplex();
        assertTrue(related.needsFetch());

        assertEquals(2, related.size());

        FlattenedTest3 ft3 = (FlattenedTest3) DataObjectUtils.objectForPK(
                context,
                FlattenedTest3.class,
                3);
        assertTrue(related.contains(ft3));
        
        ft1.removeFromFt3OverComplex(ft3);
        assertFalse(related.contains(ft3));
        context.commitChanges();
        
        // the thing here is that there are two join records between
        // FT1 and FT3 (emulating invalid data or extras in the join table that 
        // are ignored in the object model).. all (2) joins must be deleted
        assertEquals(1, context.performQuery(joinSelect).size());
    }

    public void testQualifyOnToManyFlattened() throws Exception {
        FlattenedTest1 obj01 = (FlattenedTest1) context
                .createAndRegisterNewObject(FlattenedTest1.class);
        FlattenedTest2 obj02 = (FlattenedTest2) context
                .createAndRegisterNewObject(FlattenedTest2.class);
        FlattenedTest3 obj031 = (FlattenedTest3) context
                .createAndRegisterNewObject(FlattenedTest3.class);
        FlattenedTest3 obj032 = (FlattenedTest3) context
                .createAndRegisterNewObject(FlattenedTest3.class);

        FlattenedTest1 obj11 = (FlattenedTest1) context
                .createAndRegisterNewObject(FlattenedTest1.class);
        FlattenedTest2 obj12 = (FlattenedTest2) context
                .createAndRegisterNewObject(FlattenedTest2.class);
        FlattenedTest3 obj131 = (FlattenedTest3) context
                .createAndRegisterNewObject(FlattenedTest3.class);

        obj01.setName("t01");
        obj02.setName("t02");
        obj031.setName("t031");
        obj032.setName("t032");
        obj02.setToFT1(obj01);
        obj02.addToFt3Array(obj031);
        obj02.addToFt3Array(obj032);

        obj11.setName("t11");
        obj131.setName("t131");
        obj12.setName("t12");
        obj12.addToFt3Array(obj131);
        obj12.setToFT1(obj11);

        context.commitChanges();

        // test 1: qualify on flattened attribute
        Expression qual1 = ExpressionFactory.matchExp("ft3Array.name", "t031");
        SelectQuery query1 = new SelectQuery(FlattenedTest1.class, qual1);
        List objects1 = context.performQuery(query1);

        assertEquals(1, objects1.size());
        assertSame(obj01, objects1.get(0));

        // test 2: qualify on flattened relationship
        Expression qual2 = ExpressionFactory.matchExp("ft3Array", obj131);
        SelectQuery query2 = new SelectQuery(FlattenedTest1.class, qual2);
        List objects2 = context.performQuery(query2);

        assertEquals(1, objects2.size());
        assertSame(obj11, objects2.get(0));
    }

    public void testToOneSeriesFlattenedRel() {
        FlattenedTest1 ft1 = (FlattenedTest1) context
                .createAndRegisterNewObject("FlattenedTest1");
        ft1.setName("FT1Name");
        FlattenedTest2 ft2 = (FlattenedTest2) context
                .createAndRegisterNewObject("FlattenedTest2");
        ft2.setName("FT2Name");
        FlattenedTest3 ft3 = (FlattenedTest3) context
                .createAndRegisterNewObject("FlattenedTest3");
        ft3.setName("FT3Name");

        ft2.setToFT1(ft1);
        ft2.addToFt3Array(ft3);
        context.commitChanges();

        context = createDataContext(); //We need a new context
        SelectQuery q = new SelectQuery(FlattenedTest3.class);
        q.setQualifier(ExpressionFactory.matchExp("name", "FT3Name"));
        List results = context.performQuery(q);

        assertEquals(1, results.size());

        FlattenedTest3 fetchedFT3 = (FlattenedTest3) results.get(0);
        FlattenedTest1 fetchedFT1 = fetchedFT3.getToFT1();
        assertEquals("FT1Name", fetchedFT1.getName());
    }

    public void testTakeObjectSnapshotFlattenedFault() throws Exception {
        createTestData("test");

        // fetch
        List ft3s = context.performQuery(new SelectQuery(FlattenedTest3.class));
        assertEquals(1, ft3s.size());
        FlattenedTest3 ft3 = (FlattenedTest3) ft3s.get(0);

        assertTrue(ft3.readPropertyDirectly("toFT1") instanceof Fault);

        // test that taking a snapshot does not trigger a fault, and generally works well
        Map snapshot = context.currentSnapshot(ft3);

        assertEquals("ft3", snapshot.get("NAME"));
        assertTrue(ft3.readPropertyDirectly("toFT1") instanceof Fault);

    }

    public void testIsToOneTargetModifiedFlattenedFault1() throws Exception {
        createTestData("test");

        // fetch
        List ft3s = context.performQuery(new SelectQuery(FlattenedTest3.class));
        assertEquals(1, ft3s.size());
        FlattenedTest3 ft3 = (FlattenedTest3) ft3s.get(0);

        // mark as dirty for the purpose of the test...
        ft3.setPersistenceState(PersistenceState.MODIFIED);

        assertTrue(ft3.readPropertyDirectly("toFT1") instanceof Fault);

        // test that checking for modifications does not trigger a fault, and generally
        // works well
        ObjEntity entity = context.getEntityResolver().lookupObjEntity(
                FlattenedTest3.class);
        ObjRelationship flattenedRel = (ObjRelationship) entity.getRelationship("toFT1");
        assertFalse(MockupDataRowUtils.isToOneTargetModified(flattenedRel, ft3, context
                .getObjectStore()
                .getCachedSnapshot(ft3.getObjectId())));
        assertTrue(ft3.readPropertyDirectly("toFT1") instanceof Fault);
    }

    public void testRefetchWithFlattenedFaultToOneTarget1() throws Exception {
        createTestData("test");

        // fetch
        List ft3s = context.performQuery(new SelectQuery(FlattenedTest3.class));
        assertEquals(1, ft3s.size());
        FlattenedTest3 ft3 = (FlattenedTest3) ft3s.get(0);

        assertTrue(ft3.readPropertyDirectly("toFT1") instanceof Fault);

        // refetch
        context.performQuery(new SelectQuery(FlattenedTest3.class));
        assertTrue(ft3.readPropertyDirectly("toFT1") instanceof Fault);
    }
}