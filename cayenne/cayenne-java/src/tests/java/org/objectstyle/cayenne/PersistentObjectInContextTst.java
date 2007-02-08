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

import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.access.ClientServerChannel;
import org.objectstyle.cayenne.query.ObjectIdQuery;
import org.objectstyle.cayenne.remote.ClientChannel;
import org.objectstyle.cayenne.remote.ClientConnection;
import org.objectstyle.cayenne.remote.service.LocalConnection;
import org.objectstyle.cayenne.testdo.mt.ClientMtTable1;
import org.objectstyle.cayenne.testdo.mt.ClientMtTable2;
import org.objectstyle.cayenne.testdo.mt.MtTable1;
import org.objectstyle.cayenne.testdo.mt.MtTable2;
import org.objectstyle.cayenne.unit.AccessStack;
import org.objectstyle.cayenne.unit.CayenneTestCase;
import org.objectstyle.cayenne.unit.CayenneTestResources;
import org.objectstyle.cayenne.util.PersistentObjectHolder;

public class PersistentObjectInContextTst extends CayenneTestCase {

    protected AccessStack buildAccessStack() {
        return CayenneTestResources
                .getResources()
                .getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    protected ObjectContext createObjectContext() {
        // wrap ClientServerChannel in LocalConnection to enable logging...
        ClientConnection connector = new LocalConnection(new ClientServerChannel(
                getDomain()), LocalConnection.HESSIAN_SERIALIZATION);
        return new CayenneContext(new ClientChannel(connector));
    }

    public void testResolveToManyReverseResolved() throws Exception {
        createTestData("prepare");

        ObjectContext context = createObjectContext();

        ObjectId gid = new ObjectId(
                "MtTable1",
                MtTable1.TABLE1_ID_PK_COLUMN,
                new Integer(1));
        ClientMtTable1 t1 = (ClientMtTable1) DataObjectUtils.objectForQuery(
                context,
                new ObjectIdQuery(gid));

        assertNotNull(t1);

        List t2s = t1.getTable2Array();
        assertEquals(2, t2s.size());
        Iterator it = t2s.iterator();
        while (it.hasNext()) {
            ClientMtTable2 t2 = (ClientMtTable2) it.next();

            PersistentObjectHolder holder = (PersistentObjectHolder) t2.getTable1Direct();
            assertFalse(holder.isFault());
            assertSame(t1, holder.getValue());
        }
    }

    public void testToOneRelationship() throws Exception {
        createTestData("prepare");

        ObjectContext context = createObjectContext();

        ObjectId gid = new ObjectId(
                "MtTable2",
                MtTable2.TABLE2_ID_PK_COLUMN,
                new Integer(1));
        ClientMtTable2 mtTable21 = (ClientMtTable2) DataObjectUtils.objectForQuery(
                context,
                new ObjectIdQuery(gid));

        assertNotNull(mtTable21);

        ClientMtTable1 mtTable1 = mtTable21.getTable1();
        assertNotNull("To one relationship incorrectly resolved to null", mtTable1);
        assertEquals("g1", mtTable1.getGlobalAttribute1());
    }

    public void testResolveToOneReverseResolved() throws Exception {
        createTestData("prepare");

        ObjectContext context = createObjectContext();

        ObjectId gid = new ObjectId(
                "MtTable2",
                MtTable2.TABLE2_ID_PK_COLUMN,
                new Integer(1));
        ClientMtTable2 mtTable21 = (ClientMtTable2) DataObjectUtils.objectForQuery(
                context,
                new ObjectIdQuery(gid));

        assertNotNull(mtTable21);

        ClientMtTable1 mtTable1 = mtTable21.getTable1();
        assertNotNull("To one relationship incorrectly resolved to null", mtTable1);

        List list = mtTable1.getTable2Array();
        assertNotNull(list);
        assertTrue(list instanceof ValueHolder);

        assertTrue(((ValueHolder) list).isFault());

        // resolve it here...
        assertEquals(2, list.size());
        Iterator it = list.iterator();
        while (it.hasNext()) {
            ClientMtTable2 t2 = (ClientMtTable2) it.next();

            PersistentObjectHolder holder = (PersistentObjectHolder) t2.getTable1Direct();
            assertFalse(holder.isFault());
            assertSame(mtTable1, holder.getValue());
        }

        assertEquals("g1", mtTable1.getGlobalAttribute1());
    }
}
