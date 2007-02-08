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

import java.util.List;

import org.objectstyle.cayenne.CayenneContext;
import org.objectstyle.cayenne.DataChannel;
import org.objectstyle.cayenne.ValueHolder;
import org.objectstyle.cayenne.access.ClientServerChannel;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.query.NamedQuery;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.remote.ClientChannel;
import org.objectstyle.cayenne.remote.service.LocalConnection;
import org.objectstyle.cayenne.testdo.mt.ClientMtTable1;
import org.objectstyle.cayenne.testdo.mt.ClientMtTable2;
import org.objectstyle.cayenne.unit.AccessStack;
import org.objectstyle.cayenne.unit.CayenneTestCase;
import org.objectstyle.cayenne.unit.CayenneTestResources;
import org.objectstyle.cayenne.util.PersistentObjectHolder;
import org.objectstyle.cayenne.util.PersistentObjectList;

public class ClientServerChannelQueryTst extends CayenneTestCase {

    protected AccessStack buildAccessStack() {
        return CayenneTestResources
                .getResources()
                .getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    public void testNamedQuery() throws Exception {
        createTestData("prepare");

        NamedQuery q = new NamedQuery("AllMtTable1");
        List results = buildContext().performQuery(q);

        assertEquals(2, results.size());
        assertTrue(results.get(0) instanceof ClientMtTable1);
    }

    public void testSelectQueryEntityNameRoot() throws Exception {
        createTestData("prepare");

        SelectQuery q = new SelectQuery("MtTable1");
        List results = buildContext().performQuery(q);

        assertEquals(2, results.size());

        assertTrue(results.get(0) instanceof ClientMtTable1);
    }

    public void testSelectQueryClientClassRoot() throws Exception {
        createTestData("prepare");

        SelectQuery q = new SelectQuery(ClientMtTable1.class);
        List results = buildContext().performQuery(q);

        assertEquals(2, results.size());
        assertTrue(results.get(0) instanceof ClientMtTable1);
    }

    public void testSelectQuerySimpleQualifier() throws Exception {
        createTestData("prepare");

        SelectQuery q = new SelectQuery(ClientMtTable1.class, Expression
                .fromString("globalAttribute1 = 'g1'"));
        List results = buildContext().performQuery(q);

        assertEquals(1, results.size());

        assertTrue(results.get(0) instanceof ClientMtTable1);
    }

    public void testSelectQueryToManyRelationshipQualifier() throws Exception {
        createTestData("prepare");

        SelectQuery q = new SelectQuery(ClientMtTable1.class, Expression
                .fromString("table2Array.globalAttribute = 'g1'"));
        List results = buildContext().performQuery(q);

        assertEquals(1, results.size());
        assertTrue(results.get(0) instanceof ClientMtTable1);
    }

    public void testSelectQueryOrdering() throws Exception {
        createTestData("prepare");

        SelectQuery q = new SelectQuery("MtTable1");
        q.addOrdering(ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY, true);
        List results = buildContext().performQuery(q);

        assertEquals(2, results.size());

        ClientMtTable1 o1 = (ClientMtTable1) results.get(0);
        ClientMtTable1 o2 = (ClientMtTable1) results.get(1);
        assertTrue(o1.getGlobalAttribute1().compareTo(o2.getGlobalAttribute1()) < 0);

        // now run the same query with reverse ordering to check that the first ordering
        // result wasn't coincidental.

        q.clearOrderings();
        q.addOrdering(ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY, false);
        List results1 = buildContext().performQuery(q);

        assertEquals(2, results1.size());

        ClientMtTable1 o3 = (ClientMtTable1) results1.get(0);
        ClientMtTable1 o4 = (ClientMtTable1) results1.get(1);
        assertTrue(o3.getGlobalAttribute1().compareTo(o4.getGlobalAttribute1()) > 0);
    }

    public void testSelectQueryPrefetchToOne() throws Exception {
        createTestData("prepare");

        SelectQuery q = new SelectQuery(ClientMtTable2.class, Expression
                .fromString("globalAttribute = 'g1'"));
        q.addPrefetch(ClientMtTable2.TABLE1_PROPERTY);
        List results = buildContext().performQuery(q);

        assertEquals(1, results.size());

        ClientMtTable2 result = (ClientMtTable2) results.get(0);

        ValueHolder holder = result.getTable1Direct();
        assertNotNull(holder);
        assertTrue(holder instanceof PersistentObjectHolder);
        PersistentObjectHolder objectHolder = (PersistentObjectHolder) holder;
        assertFalse(objectHolder.isFault());

        ClientMtTable1 target = (ClientMtTable1) objectHolder.getValue();
        assertNotNull(target);
    }

    public void testSelectQueryPrefetchToMany() throws Exception {
        createTestData("prepare");

        SelectQuery q = new SelectQuery(ClientMtTable1.class, Expression
                .fromString("globalAttribute1 = 'g1'"));
        q.addPrefetch(ClientMtTable1.TABLE2ARRAY_PROPERTY);
        List results = buildContext().performQuery(q);

        assertEquals(1, results.size());

        ClientMtTable1 result = (ClientMtTable1) results.get(0);

        List holder = result.getTable2ArrayDirect();
        assertNotNull(holder);
        assertTrue(holder instanceof PersistentObjectList);
        PersistentObjectList objectHolder = (PersistentObjectList) holder;
        assertFalse(objectHolder.isFault());
        assertEquals(2, objectHolder.size());
    }

    /**
     * Prepares ClientObjectContext that would access regular Cayenne stack over local
     * adapter with Hessian serialization.
     */
    protected CayenneContext buildContext() {
        DataChannel handler = new ClientServerChannel(getDomain());
        LocalConnection connector = new LocalConnection(
                handler,
                LocalConnection.HESSIAN_SERIALIZATION);

        return new CayenneContext(new ClientChannel(connector));
    }
}
