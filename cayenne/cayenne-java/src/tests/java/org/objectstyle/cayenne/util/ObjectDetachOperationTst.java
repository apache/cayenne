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
package org.objectstyle.cayenne.util;

import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.testdo.mt.ClientMtTable1;
import org.objectstyle.cayenne.testdo.mt.MtTable1;
import org.objectstyle.cayenne.unit.AccessStack;
import org.objectstyle.cayenne.unit.CayenneTestCase;
import org.objectstyle.cayenne.unit.CayenneTestResources;

public class ObjectDetachOperationTst extends CayenneTestCase {

    protected AccessStack buildAccessStack() {
        return CayenneTestResources
                .getResources()
                .getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    public void testDetachCommitted() {

        DataContext context = createDataContext();
        EntityResolver serverResover = context.getEntityResolver();
        EntityResolver clientResolver = serverResover.getClientEntityResolver();
        ObjectDetachOperation op = new ObjectDetachOperation(clientResolver);

        ObjectId oid = new ObjectId("MtTable1", MtTable1.TABLE1_ID_PK_COLUMN, 456);
        MtTable1 so = new MtTable1();
        so.setObjectId(oid);
        so.setGlobalAttribute1("gx");
        so.setPersistenceState(PersistenceState.COMMITTED);
        so.setObjectContext(context);
        context.getGraphManager().registerNode(oid, so);

        Object detached = op.detach(
                so,
                serverResover.getClassDescriptor("MtTable1"),
                null);
        assertNotNull(detached);
        assertNotSame(so, detached);
        assertTrue(detached instanceof ClientMtTable1);

        ClientMtTable1 co = (ClientMtTable1) detached;
        assertEquals(oid, co.getObjectId());
        assertEquals("gx", co.getGlobalAttribute1());
        assertEquals(PersistenceState.TRANSIENT, co.getPersistenceState());
        assertNull(co.getObjectContext());
    }

    public void testDetachHollow() throws Exception {

        createTestData("testDetachHollow");

        DataContext context = createDataContext();
        EntityResolver serverResover = context.getEntityResolver();
        EntityResolver clientResolver = serverResover.getClientEntityResolver();
        ObjectDetachOperation op = new ObjectDetachOperation(clientResolver);

        ObjectId oid = new ObjectId("MtTable1", MtTable1.TABLE1_ID_PK_COLUMN, 1);
        MtTable1 so = new MtTable1();
        so.setObjectId(oid);
        so.setPersistenceState(PersistenceState.HOLLOW);
        so.setObjectContext(context);
        context.getGraphManager().registerNode(oid, so);

        Object detached = op.detach(
                so,
                serverResover.getClassDescriptor("MtTable1"),
                null);
        assertNotNull(detached);
        assertNotSame(so, detached);
        assertTrue(detached instanceof ClientMtTable1);

        ClientMtTable1 co = (ClientMtTable1) detached;
        assertEquals(oid, co.getObjectId());
        assertEquals("g1", co.getGlobalAttribute1());
        assertEquals(PersistenceState.TRANSIENT, co.getPersistenceState());
        assertNull(co.getObjectContext());
    }
}
