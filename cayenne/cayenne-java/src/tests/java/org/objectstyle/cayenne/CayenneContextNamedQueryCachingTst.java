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

import java.util.Collections;
import java.util.List;

import org.objectstyle.cayenne.access.ClientServerChannel;
import org.objectstyle.cayenne.query.NamedQuery;
import org.objectstyle.cayenne.remote.ClientChannel;
import org.objectstyle.cayenne.remote.service.LocalConnection;
import org.objectstyle.cayenne.unit.AccessStack;
import org.objectstyle.cayenne.unit.CayenneTestCase;
import org.objectstyle.cayenne.unit.CayenneTestResources;
import org.objectstyle.cayenne.unit.TestLocalConnection;

public class CayenneContextNamedQueryCachingTst extends CayenneTestCase {

    protected TestLocalConnection connection;
    protected CayenneContext context;

    protected AccessStack buildAccessStack() {
        return CayenneTestResources
                .getResources()
                .getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    protected void setUp() throws Exception {
        super.setUp();

        ClientServerChannel serverChannel = new ClientServerChannel(getDomain());
        connection = new TestLocalConnection(
                serverChannel,
                LocalConnection.HESSIAN_SERIALIZATION);
        ClientChannel clientChannel = new ClientChannel(connection);
        context = new CayenneContext(clientChannel);
    }

    public void testLocalCache() throws Exception {
        deleteTestData();
        createTestData("prepare");

        NamedQuery q1 = new NamedQuery("MtQueryWithLocalCache");

        List result1 = context.performQuery(q1);
        assertEquals(3, result1.size());

        connection.setBlockingMessages(true);
        try {
            List result2 = context.performQuery(q1);
            assertSame(result1, result2);
        }
        finally {
            connection.setBlockingMessages(false);
        }

        // refresh
        q1.setForceNoCache(true);
        List result3 = context.performQuery(q1);
        assertNotSame(result1, result3);
        assertEquals(3, result3.size());
    }

    public void testLocalCacheParameterized() throws Exception {
        deleteTestData();
        createTestData("prepare");

        NamedQuery q1 = new NamedQuery("ParameterizedMtQueryWithLocalCache", Collections
                .singletonMap("g", "g1"));

        NamedQuery q2 = new NamedQuery("ParameterizedMtQueryWithLocalCache", Collections
                .singletonMap("g", "g2"));

        List result1 = context.performQuery(q1);
        assertEquals(1, result1.size());

        connection.setBlockingMessages(true);
        try {
            List result2 = context.performQuery(q1);
            assertSame(result1, result2);
        }
        finally {
            connection.setBlockingMessages(false);
        }

        List result3 = context.performQuery(q2);
        assertNotSame(result1, result3);
        assertEquals(1, result3.size());
        
        connection.setBlockingMessages(true);
        try {
            List result4 = context.performQuery(q2);
            assertSame(result3, result4);
            
            List result5 = context.performQuery(q1);
            assertSame(result1, result5);
        }
        finally {
            connection.setBlockingMessages(false);
        }
    }
}
