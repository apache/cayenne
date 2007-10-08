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
package org.objectstyle.cayenne.access;

import java.util.Arrays;
import java.util.Collections;

import org.objectstyle.art.GeneratedColumnCompKey;
import org.objectstyle.art.GeneratedColumnCompMaster;
import org.objectstyle.art.GeneratedColumnDep;
import org.objectstyle.art.GeneratedColumnTest;
import org.objectstyle.art.GeneratedColumnTest2;
import org.objectstyle.art.MeaningfulGeneratedColumnTest;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataObjectUtils;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.unit.CayenneTestCase;

/**
 * @author Andrei Adamchik
 */
public class IdentityColumnsTst extends CayenneTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testNewObject() throws Exception {
        DataContext context = createDataContext();
        GeneratedColumnTest idObject = (GeneratedColumnTest) context
                .createAndRegisterNewObject(GeneratedColumnTest.class);

        String name = "n_" + System.currentTimeMillis();
        idObject.setName(name);

        idObject.getDataContext().commitChanges();

        // this will throw an exception if id wasn't generated one way or another
        int id = DataObjectUtils.intPKForObject(idObject);
        assertTrue(id >= 0);

        // make sure that id is the same as id in the DB
        context.invalidateObjects(Collections.singleton(idObject));
        GeneratedColumnTest object = (GeneratedColumnTest) DataObjectUtils.objectForPK(
                context,
                GeneratedColumnTest.class,
                id);
        assertNotNull(object);
        assertEquals(name, object.getName());
    }

    /**
     * Tests CAY-422 bug.
     */
    public void testUnrelatedUpdate() throws Exception {
        DataContext context = createDataContext();
        GeneratedColumnTest m = (GeneratedColumnTest) context
                .createAndRegisterNewObject(GeneratedColumnTest.class);

        m.setName("m");

        GeneratedColumnDep d = (GeneratedColumnDep) context
                .createAndRegisterNewObject(GeneratedColumnDep.class);
        d.setName("d");
        d.setToMaster(m);
        context.commitChanges();

        context.invalidateObjects(Arrays.asList(new Object[] {
                m, d
        }));

        context.prepareForAccess(d, null);

        // this line caused CAY-422 error
        d.getToMaster();

        d.setName("new name");
        context.commitChanges();
    }

    /**
     * Tests that insert in two tables with identity pk does not generate a conflict. See
     * CAY-341 for the original bug.
     */
    public void testMultipleNewObjectsSeparateTables() throws Exception {
        DataContext context = createDataContext();

        GeneratedColumnTest idObject1 = (GeneratedColumnTest) context
                .createAndRegisterNewObject(GeneratedColumnTest.class);
        idObject1.setName("o1");

        GeneratedColumnTest2 idObject2 = (GeneratedColumnTest2) context
                .createAndRegisterNewObject(GeneratedColumnTest2.class);
        idObject2.setName("o2");

        context.commitChanges();
    }

    public void testMultipleNewObjects() throws Exception {
        DataContext context = createDataContext();

        String[] names = new String[] {
                "n1_" + System.currentTimeMillis(), "n2_" + System.currentTimeMillis(),
                "n3_" + System.currentTimeMillis()
        };

        GeneratedColumnTest[] idObjects = new GeneratedColumnTest[] {
                (GeneratedColumnTest) context
                        .createAndRegisterNewObject(GeneratedColumnTest.class),
                (GeneratedColumnTest) context
                        .createAndRegisterNewObject(GeneratedColumnTest.class),
                (GeneratedColumnTest) context
                        .createAndRegisterNewObject(GeneratedColumnTest.class)
        };

        for (int i = 0; i < idObjects.length; i++) {
            idObjects[i].setName(names[i]);
        }

        context.commitChanges();

        int[] ids = new int[idObjects.length];
        for (int i = 0; i < idObjects.length; i++) {
            ids[i] = DataObjectUtils.intPKForObject(idObjects[i]);
            assertTrue(ids[i] > 0);
        }

        context.invalidateObjects(Arrays.asList(idObjects));

        for (int i = 0; i < ids.length; i++) {
            GeneratedColumnTest object = (GeneratedColumnTest) DataObjectUtils
                    .objectForPK(context, GeneratedColumnTest.class, ids[i]);
            assertNotNull(object);
            assertEquals(names[i], object.getName());
        }
    }

    public void testCompoundPKWithGeneratedColumn() throws Exception {
        if (getAccessStackAdapter().getAdapter().supportsGeneratedKeys()) {
            // only works for generated keys, as the entity tested has one Cayenne
            // auto-pk and one generated key

            String masterName = "m_" + System.currentTimeMillis();
            String depName1 = "dep1_" + System.currentTimeMillis();
            String depName2 = "dep2_" + System.currentTimeMillis();

            DataContext context = createDataContext();
            GeneratedColumnCompMaster master = (GeneratedColumnCompMaster) context
                    .createAndRegisterNewObject(GeneratedColumnCompMaster.class);
            master.setName(masterName);

            GeneratedColumnCompKey dep1 = (GeneratedColumnCompKey) context
                    .createAndRegisterNewObject(GeneratedColumnCompKey.class);
            dep1.setName(depName1);
            dep1.setToMaster(master);

            GeneratedColumnCompKey dep2 = (GeneratedColumnCompKey) context
                    .createAndRegisterNewObject(GeneratedColumnCompKey.class);
            dep2.setName(depName2);
            dep2.setToMaster(master);

            context.commitChanges();

            int masterId = DataObjectUtils.intPKForObject(master);

            ObjectId id2 = dep2.getObjectId();

            // check propagated id
            Number propagatedID2 = (Number) id2.getIdSnapshot().get(
                    GeneratedColumnCompKey.PROPAGATED_PK_PK_COLUMN);
            assertNotNull(propagatedID2);
            assertEquals(masterId, propagatedID2.intValue());

            // check Cayenne-generated ID
            Number cayenneGeneratedID2 = (Number) id2.getIdSnapshot().get(
                    GeneratedColumnCompKey.AUTO_PK_PK_COLUMN);
            assertNotNull(cayenneGeneratedID2);

            // check DB-generated ID
            Number dbGeneratedID2 = (Number) id2.getIdSnapshot().get(
                    GeneratedColumnCompKey.GENERATED_COLUMN_PK_COLUMN);
            assertNotNull(dbGeneratedID2);

            context.invalidateObjects(Arrays.asList(new Object[] {
                    master, dep1, dep2
            }));

            DataObject fetchedDep2 = DataObjectUtils.objectForPK(context, id2);
            assertNotNull(fetchedDep2);
        }
    }

    public void testUpdateDependentWithNewMaster() throws Exception {
        DataContext context = createDataContext();
        GeneratedColumnTest master1 = (GeneratedColumnTest) context
                .createAndRegisterNewObject(GeneratedColumnTest.class);
        master1.setName("aaa");

        GeneratedColumnDep dependent = (GeneratedColumnDep) context
                .createAndRegisterNewObject(GeneratedColumnDep.class);
        dependent.setName("aaa");
        dependent.setToMaster(master1);

        context.commitChanges();

        // change master
        GeneratedColumnTest master2 = (GeneratedColumnTest) context
                .createAndRegisterNewObject(GeneratedColumnTest.class);
        master2.setName("bbb");

        // TESTING THIS
        dependent.setToMaster(master2);
        context.commitChanges();

        int id1 = DataObjectUtils.intPKForObject(master2);
        assertTrue(id1 >= 0);

        int id2 = DataObjectUtils.intPKForObject(dependent);
        assertTrue(id2 >= 0);
        assertEquals(id1, id2);

        context.invalidateObjects(Arrays.asList(new Object[] {
                master2, dependent
        }));

        assertNotNull(DataObjectUtils
                .objectForPK(context, GeneratedColumnTest.class, id1));
        assertNotNull(DataObjectUtils.objectForPK(context, GeneratedColumnDep.class, id2));
    }

    public void testGeneratedDefaultValue() throws Exception {

        // fail("TODO: test insert with DEFAULT generated column...need custom SQL to
        // build such table");
    }

    public void testPropagateToDependent() throws Exception {
        DataContext context = createDataContext();
        GeneratedColumnTest idObject = (GeneratedColumnTest) context
                .createAndRegisterNewObject(GeneratedColumnTest.class);
        idObject.setName("aaa");

        GeneratedColumnDep dependent = (GeneratedColumnDep) idObject
                .getDataContext()
                .createAndRegisterNewObject(GeneratedColumnDep.class);
        dependent.setName("aaa");
        dependent.setToMaster(idObject);

        context.commitChanges();

        // this will throw an exception if id wasn't generated
        int id1 = DataObjectUtils.intPKForObject(idObject);
        assertTrue(id1 >= 0);

        int id2 = DataObjectUtils.intPKForObject(dependent);
        assertTrue(id2 >= 0);

        assertEquals(id1, id2);

        // refetch from DB
        context.invalidateObjects(Arrays.asList(new Object[] {
                idObject, dependent
        }));

        assertNotNull(DataObjectUtils
                .objectForPK(context, GeneratedColumnTest.class, id1));
        assertNotNull(DataObjectUtils.objectForPK(context, GeneratedColumnDep.class, id2));
    }

    public void testMeaningfulPK() throws Exception {
        DataContext context = createDataContext();

        MeaningfulGeneratedColumnTest o = (MeaningfulGeneratedColumnTest) context
                .createAndRegisterNewObject(MeaningfulGeneratedColumnTest.class);
        o.setName("o1");
        o.setGeneratedColumn(new Integer(33333));

        context.commitChanges();
        assertNotNull(o.getGeneratedColumn());

        // Note - this only *appears* to work, but the following assertion will fail. For
        // now we address the issue by giving a warning in the modeler that generated PKs
        // can't be meaningful.
        // assertEquals(new Integer(33333), o.getObjectId().getIdSnapshot().get(
        // MeaningfulGeneratedColumnTest.GENERATED_COLUMN_PK_COLUMN));
    }
}