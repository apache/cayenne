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
import org.objectstyle.cayenne.remote.ClientChannel;
import org.objectstyle.cayenne.remote.service.LocalConnection;
import org.objectstyle.cayenne.testdo.mt.ClientMtDeleteCascade;
import org.objectstyle.cayenne.testdo.mt.ClientMtDeleteDeny;
import org.objectstyle.cayenne.testdo.mt.ClientMtDeleteNullify;
import org.objectstyle.cayenne.testdo.mt.ClientMtDeleteRule;
import org.objectstyle.cayenne.unit.AccessStack;
import org.objectstyle.cayenne.unit.CayenneTestCase;
import org.objectstyle.cayenne.unit.CayenneTestResources;

public class CayenneContextDeleteRulesTst extends CayenneTestCase {

    protected AccessStack buildAccessStack() {
        return CayenneTestResources
                .getResources()
                .getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    private CayenneContext createClientContext() {
        ClientServerChannel serverChannel = new ClientServerChannel(getDomain());
        LocalConnection connection = new LocalConnection(serverChannel);
        ClientChannel clientChannel = new ClientChannel(connection);
        return new CayenneContext(clientChannel);
    }

    public void testNullifyToOne() {
        CayenneContext context = createClientContext();

        ClientMtDeleteNullify object = (ClientMtDeleteNullify) context
                .newObject(ClientMtDeleteNullify.class);
        object.setName("object");

        ClientMtDeleteRule related = (ClientMtDeleteRule) context
                .newObject(ClientMtDeleteRule.class);
        object.setName("related");

        object.setNullify(related);
        context.commitChanges();

        context.deleteObject(object);
        assertFalse(related.getFromNullify().contains(object));
        assertNull(object.getNullify());

        // And be sure that the commit works afterwards, just for sanity
        context.commitChanges();
    }

    public void testDenyToOne() {

        CayenneContext context = createClientContext();

        ClientMtDeleteDeny object = (ClientMtDeleteDeny) context
                .newObject(ClientMtDeleteDeny.class);
        ClientMtDeleteRule related = (ClientMtDeleteRule) context
                .newObject(ClientMtDeleteRule.class);
        object.setDeny(related);
        context.commitChanges();

        try {
            context.deleteObject(object);
            fail("Should have thrown an exception");
        }
        catch (DeleteDenyException e) {
            // expected
        }

        object.setDeny(null);
        context.deleteObject(object);
        context.commitChanges();
    }

    public void testCascadeToOne() {
        CayenneContext context = createClientContext();

        ClientMtDeleteCascade object = (ClientMtDeleteCascade) context
                .newObject(ClientMtDeleteCascade.class);
        object.setName("object");

        ClientMtDeleteRule related = (ClientMtDeleteRule) context
                .newObject(ClientMtDeleteRule.class);
        object.setName("related");

        object.setCascade(related);
        context.commitChanges();

        context.deleteObject(object);
        assertEquals(PersistenceState.DELETED, related.getPersistenceState());
        assertTrue(context.deletedObjects().contains(related));

        // And be sure that the commit works afterwards, just for sanity
        context.commitChanges();
    }

    public void testCascadeToOneNewObject() {
        CayenneContext context = createClientContext();

        ClientMtDeleteRule related = (ClientMtDeleteRule) context
                .newObject(ClientMtDeleteRule.class);
        context.commitChanges();

        ClientMtDeleteCascade object = (ClientMtDeleteCascade) context
                .newObject(ClientMtDeleteCascade.class);
        object.setName("object");
        object.setCascade(related);

        context.deleteObject(object);
        assertEquals(PersistenceState.TRANSIENT, object.getPersistenceState());
        assertEquals(PersistenceState.DELETED, related.getPersistenceState());
        assertFalse(context.deletedObjects().contains(object));
        assertTrue(context.deletedObjects().contains(related));

        context.commitChanges();
    }
}
