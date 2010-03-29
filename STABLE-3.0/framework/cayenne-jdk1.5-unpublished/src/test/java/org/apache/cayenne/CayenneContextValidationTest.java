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

import org.apache.cayenne.access.ClientServerChannel;
import org.apache.cayenne.remote.ClientChannel;
import org.apache.cayenne.remote.service.LocalConnection;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable2;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CayenneResources;
import org.apache.cayenne.validation.ValidationException;

public class CayenneContextValidationTest extends CayenneCase {

    @Override
    protected AccessStack buildAccessStack() {
        return CayenneResources
                .getResources()
                .getAccessStack(MULTI_TIER_ACCESS_STACK);
    }

    public void testValidate() throws Exception {

        deleteTestData();
        DataChannel serverChannel = new ClientServerChannel(getDomain());
        ClientChannel clientChannel = new ClientChannel(
                new LocalConnection(serverChannel),
                true);

        CayenneContext c = new CayenneContext(clientChannel);
        
        ClientMtTable1 o1 = c.newObject(ClientMtTable1.class);
        o1.setGlobalAttribute1("G1");
        o1.resetValidation(false);
        
        // this one is not validating
        ClientMtTable2 o2 = c.newObject(ClientMtTable2.class);
        o2.setTable1(o1);
        
        c.commitChanges();
        assertTrue(o1.isValidatedForInsert());
        assertFalse(o1.isValidatedForDelete());
        assertFalse(o1.isValidatedForUpdate());
        
        o1.resetValidation(false);
        o1.setGlobalAttribute1("G2");
        
        c.commitChanges();
        assertFalse(o1.isValidatedForInsert());
        assertFalse(o1.isValidatedForDelete());
        assertTrue(o1.isValidatedForUpdate());
        
        o1.resetValidation(false);
        c.deleteObject(o1);
        c.deleteObject(o2);
        
        c.commitChanges();
        assertFalse(o1.isValidatedForInsert());
        assertTrue(o1.isValidatedForDelete());
        assertFalse(o1.isValidatedForUpdate());
        
        ClientMtTable1 o11 = c.newObject(ClientMtTable1.class);
        o11.setGlobalAttribute1("G1");
        o11.resetValidation(true);
        
        try {
            c.commitChanges();
            fail("Validation failure must have prevented commit");
        }
        catch (ValidationException e) {
           // expected
        }
    }
}
