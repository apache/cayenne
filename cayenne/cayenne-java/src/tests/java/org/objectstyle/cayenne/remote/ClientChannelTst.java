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
package org.objectstyle.cayenne.remote;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.objectstyle.cayenne.CayenneContext;
import org.objectstyle.cayenne.MockPersistentObject;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.Persistent;
import org.objectstyle.cayenne.QueryResponse;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.remote.ClientChannel;
import org.objectstyle.cayenne.unit.CayenneTestCase;
import org.objectstyle.cayenne.util.GenericResponse;

public class ClientChannelTst extends CayenneTestCase {

    public void testOnQuerySelect() {
        final MockPersistentObject o1 = new MockPersistentObject();
        ObjectId oid1 = new ObjectId("test_entity");
        o1.setObjectId(oid1);

        MockClientConnection connection = new MockClientConnection(new GenericResponse(Arrays
                .asList(new Object[] {
                    o1
                })));

        ClientChannel channel = new ClientChannel(connection);

        CayenneContext context = new CayenneContext(channel);
        ObjEntity entity = new ObjEntity("test_entity");
        entity.setClassName(MockPersistentObject.class.getName());

        DataMap dataMap = new DataMap("test");
        dataMap.addObjEntity(entity);
        Collection entities = Collections.singleton(dataMap);
        context.setEntityResolver(new EntityResolver(entities));

        QueryResponse response = channel.onQuery(context, new SelectQuery("test_entity"));
        assertNotNull(response);
        List list = response.firstList();
        assertNotNull(list);
        assertEquals(1, list.size());
        Persistent o1_1 = (Persistent) list.get(0);
        
        assertEquals(o1.getObjectId(), o1_1.getObjectId());

        // ObjectContext must be injected
        assertEquals(context, o1_1.getObjectContext());
        assertSame(o1_1, context.getGraphManager().getNode(oid1));
    }

    public void testOnQuerySelectOverrideCached() {
        ObjEntity entity = new ObjEntity("test_entity");
        entity.setClassName(MockPersistentObject.class.getName());

        DataMap dataMap = new DataMap("test");
        dataMap.addObjEntity(entity);
        Collection entities = Collections.singleton(dataMap);
        EntityResolver resolver = new EntityResolver(entities);

        CayenneContext context = new CayenneContext();
        context.setEntityResolver(resolver);

        ObjectId oid = new ObjectId("test_entity", "x", "y");

        MockPersistentObject o1 = new MockPersistentObject(oid);
        context.getGraphManager().registerNode(oid, o1);
        assertSame(o1, context.getGraphManager().getNode(oid));

        // another object with the same GID ... we must merge it with cached and return
        // cached object instead of the one fetched
        MockPersistentObject o2 = new MockPersistentObject(oid);

        MockClientConnection connection = new MockClientConnection(new GenericResponse(Arrays
                .asList(new Object[] {
                    o2
                })));

        ClientChannel channel = new ClientChannel(connection);

        context.setChannel(channel);
        QueryResponse response = channel.onQuery(context, new SelectQuery("test_entity"));
        assertNotNull(response);

        List list = response.firstList();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertTrue("Expected cached object, got: " + list, list.contains(o1));
        assertSame(o1, context.getGraphManager().getNode(oid));
    }

    public void testOnQuerySelectOverrideModifiedCached() {
        ObjEntity entity = new ObjEntity("test_entity");
        entity.setClassName(MockPersistentObject.class.getName());
        DataMap dataMap = new DataMap("test");
        dataMap.addObjEntity(entity);
        Collection entities = Collections.singleton(dataMap);
        EntityResolver resolver = new EntityResolver(entities);
        CayenneContext context = new CayenneContext();
        context.setEntityResolver(resolver);

        ObjectId oid = new ObjectId("test_entity", "x", "y");

        MockPersistentObject o1 = new MockPersistentObject(oid);
        o1.setPersistenceState(PersistenceState.MODIFIED);
        context.getGraphManager().registerNode(oid, o1);
        assertSame(o1, context.getGraphManager().getNode(oid));

        // another object with the same GID ... we must merge it with cached and return
        // cached object instead of the one fetched
        MockPersistentObject o2 = new MockPersistentObject(oid);
        MockClientConnection connection = new MockClientConnection(new GenericResponse(Arrays
                .asList(new Object[] {
                    o2
                })));

        ClientChannel channel = new ClientChannel(connection);

        context.setChannel(channel);
        QueryResponse response = channel.onQuery(context, new SelectQuery("test_entity"));
        assertNotNull(response);
        assertEquals(1, response.size());
        List list = response.firstList();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertTrue("Expected cached object, got: " + list, list.contains(o1));
        assertSame(o1, context.getGraphManager().getNode(oid));
    }
}
