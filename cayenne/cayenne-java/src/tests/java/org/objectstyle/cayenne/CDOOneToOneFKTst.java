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

import java.util.Arrays;

import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.testdo.relationship.ToOneFK1;
import org.objectstyle.cayenne.testdo.relationship.ToOneFK2;
import org.objectstyle.cayenne.unit.RelationshipTestCase;

/**
 * Tests the behavior of one-to-one relationship where to-one is pointing to an FK.
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
public class CDOOneToOneFKTst extends RelationshipTestCase {

    protected DataContext context;

    protected void setUp() throws Exception {
        deleteTestData();
        context = createDataContext();
    }

    public void testReadRelationship() {
        ToOneFK2 src = (ToOneFK2) context.createAndRegisterNewObject(ToOneFK2.class);
        ToOneFK1 target = (ToOneFK1) context.createAndRegisterNewObject(ToOneFK1.class);
        src.setToOneToFK(target);
        context.commitChanges();

        context.invalidateObjects(Arrays.asList(new Object[] {
                src, target
        }));

        ToOneFK2 src1 = (ToOneFK2) DataObjectUtils
                .objectForPK(context, src.getObjectId());
        assertNotNull(src1.getToOneToFK());
        // resolve HOLLOW
        assertSame(src1, src1.getToOneToFK().getToPK());

        context.invalidateObjects(Arrays.asList(new Object[] {
                src1, src1.getToOneToFK()
        }));

        ToOneFK1 target2 = (ToOneFK1) DataObjectUtils.objectForPK(context, target
                .getObjectId());
        assertNotNull(target2.getToPK());

        // resolve HOLLOW
        assertSame(target2, target2.getToPK().getToOneToFK());
    }

    public void test2Null() throws Exception {
        ToOneFK2 src = (ToOneFK2) context.createAndRegisterNewObject(ToOneFK2.class);
        context.commitChanges();
        context = createDataContext();

        // test database data
        ToOneFK2 src2 = (ToOneFK2) context.refetchObject(src.getObjectId());

        // *** TESTING THIS ***
        assertNull(src2.getToOneToFK());
    }

    public void testReplaceNull1() throws Exception {
        ToOneFK2 src = (ToOneFK2) context.createAndRegisterNewObject(ToOneFK2.class);
        context.commitChanges();
        context = createDataContext();

        // test database data
        ToOneFK2 src2 = (ToOneFK2) context.refetchObject(src.getObjectId());
        assertEquals(src.getObjectId(), src2.getObjectId());

        // *** TESTING THIS ***
        src2.setToOneToFK(null);
        assertNull(src2.getToOneToFK());
    }

    public void testReplaceNull2() throws Exception {
        ToOneFK2 src = (ToOneFK2) context.createAndRegisterNewObject(ToOneFK2.class);
        context.commitChanges();

        ToOneFK1 target = (ToOneFK1) context.createAndRegisterNewObject(ToOneFK1.class);

        // *** TESTING THIS ***
        src.setToOneToFK(target);

        // test before save
        assertSame(target, src.getToOneToFK());

        // do save
        context.commitChanges();
        context = createDataContext();

        // test database data
        ToOneFK2 src2 = (ToOneFK2) context.refetchObject(src.getObjectId());
        ToOneFK1 target2 = src2.getToOneToFK();
        assertNotNull(target2);
        assertEquals(src.getObjectId(), src2.getObjectId());
        assertEquals(target.getObjectId(), target2.getObjectId());
    }

    public void testNewAdd() throws Exception {
        ToOneFK2 src = (ToOneFK2) context.createAndRegisterNewObject(ToOneFK2.class);
        ToOneFK1 target = (ToOneFK1) context.createAndRegisterNewObject(ToOneFK1.class);

        // *** TESTING THIS ***
        src.setToOneToFK(target);

        // test before save
        assertSame(target, src.getToOneToFK());

        // do save
        context.commitChanges();
        context = createDataContext();

        // test database data
        ToOneFK2 src2 = (ToOneFK2) context.refetchObject(src.getObjectId());
        ToOneFK1 target2 = src2.getToOneToFK();
        assertNotNull(target2);
        assertEquals(src.getObjectId(), src2.getObjectId());
        assertEquals(target.getObjectId(), target2.getObjectId());
    }

    // technically replacing a related object with a new one is possible...
    // though this seems pretty evil...
    /*
     * public void testReplace() throws Exception { ToOneFK2 src = (ToOneFK2)
     * context.createAndRegisterNewObject(ToOneFK2.class); ToOneFK1 target = (ToOneFK1)
     * context.createAndRegisterNewObject(ToOneFK1.class); src.setToOneToFK(target);
     * assertSame(target, src.getToOneToFK()); context.commitChanges(); // replace target
     * ToOneFK1 target1 = (ToOneFK1) context.createAndRegisterNewObject(ToOneFK1.class);
     * src.setToOneToFK(target1); assertSame(target1, src.getToOneToFK()); // delete an
     * old target, since the column is not nullable... context.deleteObject(target);
     * context.commitChanges(); context = createDataContext(); // test database data
     * ToOneFK2 src2 = (ToOneFK2) context.refetchObject(src.getObjectId()); ToOneFK1
     * target2 = src2.getToOneToFK(); assertNotNull(target2);
     * assertEquals(src.getObjectId(), src2.getObjectId());
     * assertEquals(target1.getObjectId(), target2.getObjectId()); }
     */

    public void testTakeObjectSnapshotDependentFault() throws Exception {
        ToOneFK2 src = (ToOneFK2) context.createAndRegisterNewObject(ToOneFK2.class);
        ToOneFK1 target = (ToOneFK1) context.createAndRegisterNewObject(ToOneFK1.class);
        src.setToOneToFK(target);
        context.commitChanges();
        context = createDataContext();
        ToOneFK2 src2 = (ToOneFK2) context.refetchObject(src.getObjectId());

        assertTrue(src2.readPropertyDirectly("toOneToFK") instanceof Fault);

        // test that taking a snapshot does not trigger a fault, and generally works well
        context.currentSnapshot(src2);
        assertTrue(src2.readPropertyDirectly("toOneToFK") instanceof Fault);
    }

    public void testDelete() throws Exception {
        ToOneFK2 src = (ToOneFK2) context.createAndRegisterNewObject(ToOneFK2.class);
        ToOneFK1 target = (ToOneFK1) context.createAndRegisterNewObject(ToOneFK1.class);
        src.setToOneToFK(target);
        context.commitChanges();

        src.setToOneToFK(null);
        context.deleteObject(target);
        context.commitChanges();

        // test database data
        context = createDataContext();
        ToOneFK2 src2 = (ToOneFK2) context.refetchObject(src.getObjectId());
        assertNull(src.getToOneToFK());
        assertEquals(src.getObjectId(), src2.getObjectId());
    }
}
