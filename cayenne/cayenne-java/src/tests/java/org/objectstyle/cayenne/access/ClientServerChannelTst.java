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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.objectstyle.cayenne.DataChannel;
import org.objectstyle.cayenne.MockDataChannel;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.QueryResponse;
import org.objectstyle.cayenne.ValueHolder;
import org.objectstyle.cayenne.graph.MockGraphDiff;
import org.objectstyle.cayenne.graph.NodeCreateOperation;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.query.MockQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.remote.QueryMessage;
import org.objectstyle.cayenne.remote.hessian.service.HessianUtil;
import org.objectstyle.cayenne.testdo.mt.ClientMtTable1;
import org.objectstyle.cayenne.testdo.mt.ClientMtTable1Subclass;
import org.objectstyle.cayenne.testdo.mt.ClientMtTable2;
import org.objectstyle.cayenne.testdo.mt.ClientMtTable3;
import org.objectstyle.cayenne.testdo.mt.MtTable1;
import org.objectstyle.cayenne.unit.AccessStack;
import org.objectstyle.cayenne.unit.CayenneTestCase;
import org.objectstyle.cayenne.unit.CayenneTestResources;

/**
 * @author Andrus Adamchik
 */
public class ClientServerChannelTst extends CayenneTestCase {

    protected AccessStack buildAccessStack() {
        return CayenneTestResources
                .getResources()
                .getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    public void testGetEntityResolver() throws Exception {
        EntityResolver resolver = new ClientServerChannel(getDomain())
                .getEntityResolver();
        assertNotNull(resolver);
        assertNull(resolver.lookupObjEntity(ClientMtTable1.class));
        assertNotNull(resolver.getClientEntityResolver().lookupObjEntity(
                ClientMtTable1.class));
    }

    public void testSynchronizeCommit() throws Exception {

        deleteTestData();
        SelectQuery query = new SelectQuery(MtTable1.class);

        DataContext context = createDataContext();

        assertEquals(0, context.performQuery(query).size());

        // no changes...
        ClientServerChannel channel = new ClientServerChannel(context, false);
        channel.onSync(context, new MockGraphDiff(), DataChannel.FLUSH_CASCADE_SYNC);

        assertEquals(0, context.performQuery(query).size());

        // introduce changes
        channel.onSync(
                context,
                new NodeCreateOperation(new ObjectId("MtTable1")),
                DataChannel.FLUSH_CASCADE_SYNC);

        assertEquals(1, context.performQuery(query).size());
    }

    public void testPerformQueryObjectIDInjection() throws Exception {
        createTestData("testOnSelectQueryObjectIDInjection");

        DataContext context = createDataContext();

        Query query = new SelectQuery("MtTable1");
        QueryResponse response = new ClientServerChannel(context, false).onQuery(
                null,
                query);

        assertNotNull(response);

        List results = response.firstList();

        assertNotNull(results);
        assertEquals(1, results.size());

        Object result = results.get(0);
        assertTrue(result instanceof ClientMtTable1);
        ClientMtTable1 clientObject = (ClientMtTable1) result;
        assertNotNull(clientObject.getObjectId());

        assertEquals(
                new ObjectId("MtTable1", MtTable1.TABLE1_ID_PK_COLUMN, 55),
                clientObject.getObjectId());
    }

    public void testPerformQueryValuePropagation() throws Exception {

        byte[] bytes = new byte[] {
                1, 2, 3
        };

        String chars = "abc";

        Map parameters = new HashMap();
        parameters.put("bytes", bytes);
        parameters.put("chars", chars);

        createTestData("testOnSelectQueryValuePropagation", parameters);

        DataContext context = createDataContext();

        Query query = new SelectQuery("MtTable3");
        QueryResponse response = new ClientServerChannel(context, false).onQuery(
                null,
                query);

        assertNotNull(response);

        List results = response.firstList();

        assertNotNull(results);
        assertEquals(1, results.size());

        Object result = results.get(0);
        assertTrue("Result is of wrong type: " + result, result instanceof ClientMtTable3);
        ClientMtTable3 clientObject = (ClientMtTable3) result;

        assertEquals(chars, clientObject.getCharColumn());
        assertEquals(new Integer(4), clientObject.getIntColumn());
        assertTrue(new EqualsBuilder()
                .append(clientObject.getBinaryColumn(), bytes)
                .isEquals());
    }

    public void testPerformQueryPropagationInheritance() throws Exception {

        Map parameters = new HashMap();
        parameters.put("GLOBAL_ATTRIBUTE1", "sub1");
        parameters.put("SERVER_ATTRIBUTE1", "xyz");
        createTestData("testOnSelectQueryValuePropagationInheritance", parameters);

        DataContext context = createDataContext();

        // must use real SelectQuery instead of mockup as root overriding depends on the
        // fact that Query inherits from AbstractQuery.
        SelectQuery query = new SelectQuery(ClientMtTable1.class);
        query.setResolvingInherited(true);

        // must pass through the serialization pipe before running query as
        // HessianSerializer has needed preprocessing hooks...
        Query preprocessedQuery = (Query) HessianUtil.cloneViaClientServerSerialization(
                query,
                context.getEntityResolver());

        QueryResponse response = new ClientServerChannel(context, false).onQuery(
                null,
                preprocessedQuery);

        assertNotNull(response);

        List results = response.firstList();

        assertNotNull(results);
        assertEquals(1, results.size());

        Object result = results.get(0);
        assertTrue(
                "Result is of wrong type: " + result,
                result instanceof ClientMtTable1Subclass);
        ClientMtTable1Subclass clientObject = (ClientMtTable1Subclass) result;

        assertEquals("sub1", clientObject.getGlobalAttribute1());
    }

    public void testOnQuery() {

        final boolean[] genericDone = new boolean[1];
        MockDataChannel parent = new MockDataChannel(new EntityResolver()) {

            public QueryResponse onQuery(ObjectContext context, Query query) {
                genericDone[0] = true;
                return super.onQuery(context, query);
            }
        };
        DataContext context = new DataContext(parent, new ObjectStore(
                new MockDataRowStore()));

        QueryMessage message = new QueryMessage(new MockQuery());
        new ClientServerChannel(context, false).onQuery(null, message.getQuery());
        assertTrue(genericDone[0]);
    }

    public void testOnQueryPrefetchingToMany() throws Exception {
        createTestData("testPrefetching");

        DataContext context = createDataContext();
        ClientServerChannel channel = new ClientServerChannel(context, false);

        SelectQuery q = new SelectQuery(ClientMtTable1.class);
        q.addOrdering(ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY, true);
        q.addPrefetch(ClientMtTable1.TABLE2ARRAY_PROPERTY);

        // must pass through the serialization pipe before running query as
        // HessianSerializer has needed preprocessing hooks...
        Query preprocessedQuery = (Query) HessianUtil.cloneViaClientServerSerialization(
                q,
                context.getEntityResolver());

        List results = channel.onQuery(null, preprocessedQuery).firstList();

        blockQueries();
        try {

            ClientMtTable1 o1 = (ClientMtTable1) results.get(0);
            assertNull(o1.getObjectContext());

            List children1 = o1.getTable2Array();

            assertEquals(2, children1.size());
            Iterator it = children1.iterator();
            while (it.hasNext()) {
                ClientMtTable2 o = (ClientMtTable2) it.next();
                assertNull(o.getObjectContext());
            }
        }
        finally {
            unblockQueries();
        }
    }

    public void testOnQueryPrefetchingToManyEmpty() throws Exception {
        createTestData("testPrefetching");

        DataContext context = createDataContext();
        ClientServerChannel channel = new ClientServerChannel(context, false);

        SelectQuery q = new SelectQuery(ClientMtTable1.class);
        q.addOrdering(ClientMtTable1.GLOBAL_ATTRIBUTE1_PROPERTY, true);
        q.addPrefetch(ClientMtTable1.TABLE2ARRAY_PROPERTY);

        // must pass through the serialization pipe before running query as
        // HessianSerializer has needed preprocessing hooks...
        Query preprocessedQuery = (Query) HessianUtil.cloneViaClientServerSerialization(
                q,
                context.getEntityResolver());

        List results = channel.onQuery(null, preprocessedQuery).firstList();

        blockQueries();
        try {

            ClientMtTable1 o2 = (ClientMtTable1) results.get(1);
            assertNull(o2.getObjectContext());

            List children2 = o2.getTable2Array();
            assertNotNull(children2);
            assertFalse(((ValueHolder) children2).isFault());
            assertEquals(0, children2.size());
        }
        finally {
            unblockQueries();
        }
    }
}
