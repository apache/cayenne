/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.cayenne.testdo.deleterules.ClientDeleteCascade;
import org.apache.cayenne.testdo.deleterules.ClientDeleteDeny;
import org.apache.cayenne.testdo.deleterules.ClientDeleteNullify;
import org.apache.cayenne.testdo.deleterules.ClientDeleteRule;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@UseServerRuntime(CayenneProjects.DELETE_RULES_PROJECT)
public class CayenneContextDeleteRulesIT extends ClientCase {

    @Inject
    private CayenneContext context;

    @Test
    public void testNullifyToOne() {

        ClientDeleteNullify object = context.newObject(ClientDeleteNullify.class);
        object.setName("object");

        ClientDeleteRule related = context.newObject(ClientDeleteRule.class);
        object.setName("related");

        object.setNullify(related);
        context.commitChanges();

        context.deleteObjects(object);
        assertFalse(related.getFromNullify().contains(object));
        assertNull(object.getNullify());

        // And be sure that the commit works afterwards, just for sanity
        context.commitChanges();
    }

    @Test
    public void testDenyToOne() {

        ClientDeleteDeny object = context.newObject(ClientDeleteDeny.class);
        ClientDeleteRule related = context.newObject(ClientDeleteRule.class);
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

    @Test
    public void testCascadeToOne() {

        ClientDeleteCascade object = context.newObject(ClientDeleteCascade.class);
        object.setName("object");

        ClientDeleteRule related = context.newObject(ClientDeleteRule.class);
        object.setName("related");

        object.setCascade(related);
        context.commitChanges();

        context.deleteObjects(object);
        assertEquals(PersistenceState.DELETED, related.getPersistenceState());
        assertTrue(context.deletedObjects().contains(related));

        // And be sure that the commit works afterwards, just for sanity
        context.commitChanges();
    }

    @Test
    public void testCascadeToOneNewObject() {

        ClientDeleteRule related = context.newObject(ClientDeleteRule.class);
        context.commitChanges();

        ClientDeleteCascade object = context.newObject(ClientDeleteCascade.class);
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
