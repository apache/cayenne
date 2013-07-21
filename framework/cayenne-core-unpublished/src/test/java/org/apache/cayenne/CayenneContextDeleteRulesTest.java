/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.testdo.mt.ClientMtDeleteCascade;
import org.apache.cayenne.testdo.mt.ClientMtDeleteDeny;
import org.apache.cayenne.testdo.mt.ClientMtDeleteNullify;
import org.apache.cayenne.testdo.mt.ClientMtDeleteRule;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ClientCase.MULTI_TIER_PROJECT)
public class CayenneContextDeleteRulesTest extends ClientCase {

    @Inject
    private CayenneContext context;

    public void testNullifyToOne() {

        ClientMtDeleteNullify object = context.newObject(ClientMtDeleteNullify.class);
        object.setName("object");

        ClientMtDeleteRule related = context.newObject(ClientMtDeleteRule.class);
        object.setName("related");

        object.setNullify(related);
        context.commitChanges();

        context.deleteObjects(object);
        assertFalse(related.getFromNullify().contains(object));
        assertNull(object.getNullify());

        // And be sure that the commit works afterwards, just for sanity
        context.commitChanges();
    }

    public void testDenyToOne() {

        ClientMtDeleteDeny object = context.newObject(ClientMtDeleteDeny.class);
        ClientMtDeleteRule related = context.newObject(ClientMtDeleteRule.class);
        object.setDeny(related);
        context.commitChanges();

        try {
            context.deleteObjects(object);
            fail("Should have thrown an exception");
        }
        catch (DeleteDenyException e) {
            // expected
        }

        object.setDeny(null);
        context.deleteObjects(object);
        context.commitChanges();
    }

    public void testCascadeToOne() {

        ClientMtDeleteCascade object = context.newObject(ClientMtDeleteCascade.class);
        object.setName("object");

        ClientMtDeleteRule related = context.newObject(ClientMtDeleteRule.class);
        object.setName("related");

        object.setCascade(related);
        context.commitChanges();

        context.deleteObjects(object);
        assertEquals(PersistenceState.DELETED, related.getPersistenceState());
        assertTrue(context.deletedObjects().contains(related));

        // And be sure that the commit works afterwards, just for sanity
        context.commitChanges();
    }

    public void testCascadeToOneNewObject() {

        ClientMtDeleteRule related = context.newObject(ClientMtDeleteRule.class);
        context.commitChanges();

        ClientMtDeleteCascade object = context.newObject(ClientMtDeleteCascade.class);
        object.setName("object");
        object.setCascade(related);

        context.deleteObjects(object);
        assertEquals(PersistenceState.TRANSIENT, object.getPersistenceState());
        assertEquals(PersistenceState.DELETED, related.getPersistenceState());
        assertFalse(context.deletedObjects().contains(object));
        assertTrue(context.deletedObjects().contains(related));

        context.commitChanges();
    }
}
