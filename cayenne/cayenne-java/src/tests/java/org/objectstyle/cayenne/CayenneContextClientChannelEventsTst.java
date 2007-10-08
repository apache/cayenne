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

import org.objectstyle.cayenne.access.ClientServerChannel;
import org.objectstyle.cayenne.query.ObjectIdQuery;
import org.objectstyle.cayenne.remote.ClientChannel;
import org.objectstyle.cayenne.remote.service.LocalConnection;
import org.objectstyle.cayenne.testdo.mt.ClientMtTable1;
import org.objectstyle.cayenne.testdo.mt.ClientMtTable2;
import org.objectstyle.cayenne.unit.AccessStack;
import org.objectstyle.cayenne.unit.CayenneTestCase;
import org.objectstyle.cayenne.unit.CayenneTestResources;

/**
 * Tests peer context synchronization via ClientChannel events.
 * 
 * @author Andrus Adamchik
 */
public class CayenneContextClientChannelEventsTst extends CayenneTestCase {

    protected AccessStack buildAccessStack() {
        return CayenneTestResources
                .getResources()
                .getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    public void testSyncSimpleProperty() throws Exception {
        // this resets snapshot cache...
        createDataContext();

        deleteTestData();
        createTestData("testSyncSimpleProperty");

        DataChannel serverChannel = new ClientServerChannel(getDomain(), false);
        ClientChannel clientChannel = new ClientChannel(
                new LocalConnection(serverChannel),
                true);

        CayenneContext c1 = new CayenneContext(clientChannel);
        CayenneContext c2 = new CayenneContext(clientChannel);

        ClientMtTable1 o1 = (ClientMtTable1) DataObjectUtils.objectForQuery(
                c1,
                new ObjectIdQuery(new ObjectId("MtTable1", "TABLE1_ID", 1)));

        ClientMtTable1 o2 = (ClientMtTable1) DataObjectUtils.objectForQuery(
                c2,
                new ObjectIdQuery(new ObjectId("MtTable1", "TABLE1_ID", 1)));

        assertEquals("g1", o1.getGlobalAttribute1());
        assertEquals("g1", o2.getGlobalAttribute1());

        o1.setGlobalAttribute1("X");
        c1.commitChanges();

        assertEquals("X", o2.getGlobalAttribute1());
        assertFalse(c1.internalGraphManager().hasChanges());
        assertFalse(c2.internalGraphManager().hasChanges());
    }

    public void testSyncToOneRelationship() throws Exception {
        // this resets snapshot cache...
        createDataContext();

        deleteTestData();
        createTestData("testSyncToOneRelationship");

        DataChannel serverChannel = new ClientServerChannel(getDomain(), false);
        ClientChannel clientChannel = new ClientChannel(
                new LocalConnection(serverChannel),
                true);

        CayenneContext c1 = new CayenneContext(clientChannel);
        CayenneContext c2 = new CayenneContext(clientChannel);

        ClientMtTable2 o1 = (ClientMtTable2) DataObjectUtils.objectForQuery(
                c1,
                new ObjectIdQuery(new ObjectId("MtTable2", "TABLE2_ID", 1)));

        ClientMtTable2 o2 = (ClientMtTable2) DataObjectUtils.objectForQuery(
                c2,
                new ObjectIdQuery(new ObjectId("MtTable2", "TABLE2_ID", 1)));

        assertEquals("g1", o1.getTable1().getGlobalAttribute1());
        assertEquals("g1", o2.getTable1().getGlobalAttribute1());

        ClientMtTable1 o1r = (ClientMtTable1) DataObjectUtils.objectForQuery(
                c1,
                new ObjectIdQuery(new ObjectId("MtTable1", "TABLE1_ID", 2)));
        o1.setTable1(o1r);
        c1.commitChanges();

        assertEquals("g2", o2.getTable1().getGlobalAttribute1());
        assertFalse(c1.internalGraphManager().hasChanges());
        assertFalse(c2.internalGraphManager().hasChanges());
    }

    public void testSyncToManyRelationship() throws Exception {
        // this resets snapshot cache...
        createDataContext();

        deleteTestData();
        createTestData("testSyncToManyRelationship");

        DataChannel serverChannel = new ClientServerChannel(getDomain(), false);
        ClientChannel clientChannel = new ClientChannel(
                new LocalConnection(serverChannel),
                true);

        CayenneContext c1 = new CayenneContext(clientChannel);
        CayenneContext c2 = new CayenneContext(clientChannel);

        ClientMtTable1 o1 = (ClientMtTable1) DataObjectUtils.objectForQuery(
                c1,
                new ObjectIdQuery(new ObjectId("MtTable1", "TABLE1_ID", 1)));

        ClientMtTable1 o2 = (ClientMtTable1) DataObjectUtils.objectForQuery(
                c2,
                new ObjectIdQuery(new ObjectId("MtTable1", "TABLE1_ID", 1)));

        assertEquals(1, o1.getTable2Array().size());
        assertEquals(1, o2.getTable2Array().size());

        ClientMtTable2 o1r = (ClientMtTable2) c1.newObject(ClientMtTable2.class);
        o1r.setGlobalAttribute("X");
        o1.addToTable2Array(o1r);

        c1.commitChanges();

        assertEquals(2, o1.getTable2Array().size());
        assertEquals(2, o2.getTable2Array().size());
        assertFalse(c1.internalGraphManager().hasChanges());
        assertFalse(c2.internalGraphManager().hasChanges());
    }

    public void testSyncToManyRelationship1() throws Exception {
        // this resets snapshot cache...
        createDataContext();

        deleteTestData();
        createTestData("testSyncToManyRelationship");

        DataChannel serverChannel = new ClientServerChannel(getDomain(), false);
        ClientChannel clientChannel = new ClientChannel(
                new LocalConnection(serverChannel),
                true);

        CayenneContext c1 = new CayenneContext(clientChannel);
        CayenneContext c2 = new CayenneContext(clientChannel);

        ClientMtTable1 o1 = (ClientMtTable1) DataObjectUtils.objectForQuery(
                c1,
                new ObjectIdQuery(new ObjectId("MtTable1", "TABLE1_ID", 1)));

        // do not resolve objects in question in the second context and see if the merge
        // causes any issues...

        assertEquals(1, o1.getTable2Array().size());

        ClientMtTable2 o1r = (ClientMtTable2) c1.newObject(ClientMtTable2.class);
        o1r.setGlobalAttribute("X");
        o1.addToTable2Array(o1r);

        c1.commitChanges();

        assertEquals(2, o1.getTable2Array().size());

        assertFalse(c1.internalGraphManager().hasChanges());
        assertFalse(c2.internalGraphManager().hasChanges());

        ClientMtTable1 o2 = (ClientMtTable1) DataObjectUtils.objectForQuery(
                c2,
                new ObjectIdQuery(new ObjectId("MtTable1", "TABLE1_ID", 1)));
        assertEquals(2, o2.getTable2Array().size());
    }
}
