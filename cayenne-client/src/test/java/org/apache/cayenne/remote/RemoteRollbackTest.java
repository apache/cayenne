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
package org.apache.cayenne.remote;

import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable2;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

/**
 * This is a test primarily for CAY-1103
 */
@UseServerRuntime(ClientCase.MULTI_TIER_PROJECT)
public class RemoteRollbackTest extends RemoteCayenneCase {
    
    public void testRollbackNew() {
        ClientMtTable1 o1 = clientContext.newObject(ClientMtTable1.class);
        o1.setGlobalAttribute1("a");

        ClientMtTable2 p1 = clientContext.newObject(ClientMtTable2.class);
        p1.setGlobalAttribute("p1");
        p1.setTable1(o1);

        ClientMtTable2 p2 = clientContext.newObject(ClientMtTable2.class);
        p2.setGlobalAttribute("p2");
        p2.setTable1(o1);

        ClientMtTable2 p3 = clientContext.newObject(ClientMtTable2.class);
        p3.setGlobalAttribute("p3");
        p3.setTable1(o1);

        // before:
        assertEquals(o1, p1.getTable1());
        assertEquals(3, o1.getTable2Array().size());

        clientContext.rollbackChanges();

        // after:
        assertEquals(PersistenceState.TRANSIENT, o1.getPersistenceState());

        // TODO: should we expect relationships to be unset?
        // assertNull(p1.getToClientMtTable1());
        // assertEquals(0, o1.getClientMtTable2Array().size());
    }

    public void testRollbackNewObject() {
        String o1Name = "revertTestClientMtTable1";
        ClientMtTable1 o1 = clientContext.newObject(ClientMtTable1.class);
        o1.setGlobalAttribute1(o1Name);

        clientContext.rollbackChanges();

        assertEquals(PersistenceState.TRANSIENT, o1.getPersistenceState());
        clientContext.commitChanges();
    }

    // Catches a bug where new objects were unregistered within an object iterator, thus
    // modifying the
    // collection the iterator was iterating over (ConcurrentModificationException)
    public void testRollbackWithMultipleNewObjects() {
        String o1Name = "rollbackTestClientMtTable1";
        String o2Title = "rollbackTestClientMtTable2";
        ClientMtTable1 o1 = clientContext.newObject(ClientMtTable1.class);
        o1.setGlobalAttribute1(o1Name);

        ClientMtTable2 o2 = clientContext.newObject(ClientMtTable2.class);
        o2.setGlobalAttribute(o2Title);
        o2.setTable1(o1);

        try {
            clientContext.rollbackChanges();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("rollbackChanges should not have caused the exception " + e.getMessage());
        }

        assertEquals(PersistenceState.TRANSIENT, o1.getPersistenceState());
    }

    public void testRollbackRelationshipModification() {
        String o1Name = "relationshipModClientMtTable1";
        String o2Title = "relationshipTestClientMtTable2";
        ClientMtTable1 o1 = clientContext.newObject(ClientMtTable1.class);
        o1.setGlobalAttribute1(o1Name);
        ClientMtTable2 o2 = clientContext.newObject(ClientMtTable2.class);
        o2.setGlobalAttribute(o2Title);
        o2.setTable1(o1);
        
        assertEquals(1, o1.getTable2Array().size());
        clientContext.commitChanges();

        assertEquals(1, o1.getTable2Array().size());
        o2.setTable1(null);
        assertEquals(0, o1.getTable2Array().size());
        clientContext.rollbackChanges();

        assertEquals(1, o1.getTable2Array().size());
        assertEquals(o1, o2.getTable1());
    }

    public void testRollbackDeletedObject() {
        String o1Name = "deleteTestClientMtTable1";
        ClientMtTable1 o1 = clientContext.newObject(ClientMtTable1.class);
        o1.setGlobalAttribute1(o1Name);
        clientContext.commitChanges();
        // Save... cayenne doesn't yet handle deleting objects that are uncommitted
        clientContext.deleteObjects(o1);
        clientContext.rollbackChanges();

        //TODO: The state is committed for Cayenne context, but Hollow for DataContext?!
        // Now check everything is as it should be
        assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());
    }

    public void testRollbackModifiedObject() {
        String o1Name = "initialTestClientMtTable1";
        ClientMtTable1 o1 = clientContext.newObject(ClientMtTable1.class);
        o1.setGlobalAttribute1(o1Name);
        clientContext.commitChanges();

        o1.setGlobalAttribute1("a new value");

        clientContext.rollbackChanges();

        // Make sure the inmemory changes have been rolled back
        assertEquals(PersistenceState.COMMITTED, o1.getPersistenceState());
        assertEquals(o1Name, o1.getGlobalAttribute1());
    }
}
