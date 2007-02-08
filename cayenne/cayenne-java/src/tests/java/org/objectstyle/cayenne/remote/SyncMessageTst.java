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

import junit.framework.TestCase;

import org.objectstyle.cayenne.DataChannel;
import org.objectstyle.cayenne.MockObjectContext;
import org.objectstyle.cayenne.ObjectContext;
import org.objectstyle.cayenne.graph.CompoundDiff;
import org.objectstyle.cayenne.graph.GraphDiff;
import org.objectstyle.cayenne.graph.NodeCreateOperation;
import org.objectstyle.cayenne.map.EntityResolver;
import org.objectstyle.cayenne.remote.SyncMessage;
import org.objectstyle.cayenne.remote.hessian.service.HessianUtil;

public class SyncMessageTst extends TestCase {

    public void testConstructor() {
        ObjectContext source = new MockObjectContext();
        GraphDiff diff = new CompoundDiff();
        SyncMessage message = new SyncMessage(source, DataChannel.FLUSH_NOCASCADE_SYNC, diff);

        assertSame(source, message.getSource());
        assertEquals(DataChannel.FLUSH_NOCASCADE_SYNC, message.getType());
        assertSame(diff, message.getSenderChanges());
    }

    public void testHessianSerialization() throws Exception {
        // id must be a serializable object; source doesn't have to be
        ObjectContext source = new MockObjectContext();
        GraphDiff diff = new NodeCreateOperation("id-string");
        SyncMessage message = new SyncMessage(source, DataChannel.FLUSH_NOCASCADE_SYNC, diff);

        Object d = HessianUtil.cloneViaClientServerSerialization(message, new EntityResolver());
        assertNotNull(d);
        assertTrue(d instanceof SyncMessage);

        SyncMessage ds = (SyncMessage) d;
        assertNull(ds.getSource());
        assertEquals(message.getType(), ds.getType());
        assertNotNull(ds.getSenderChanges());
    }

    public void testConstructorInvalid() {
        ObjectContext source = new MockObjectContext();
        new SyncMessage(source, DataChannel.FLUSH_NOCASCADE_SYNC, new CompoundDiff());
        new SyncMessage(source, DataChannel.FLUSH_CASCADE_SYNC, new CompoundDiff());
        new SyncMessage(null, DataChannel.ROLLBACK_CASCADE_SYNC, new CompoundDiff());

        int bogusType = 45678;
        try {
            new SyncMessage(source, bogusType, new CompoundDiff());
            fail("invalid type was allowed to go unnoticed...");
        }
        catch (IllegalArgumentException e) {

        }
    }
}
