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
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.testdo.mt.ClientMtTable2;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.apache.cayenne.validation.ValidationException;

@UseServerRuntime(ClientCase.MULTI_TIER_PROJECT)
public class CayenneContextValidationTest extends ClientCase {

    @Inject
    private DBHelper dbHelper;

    @Inject
    private CayenneContext context;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("MT_TABLE2");
        dbHelper.deleteAll("MT_TABLE1");
    }

    public void testValidate() throws Exception {

        ClientMtTable1 o1 = context.newObject(ClientMtTable1.class);
        o1.setGlobalAttribute1("G1");
        o1.resetValidation(false);

        // this one is not validating
        ClientMtTable2 o2 = context.newObject(ClientMtTable2.class);
        o2.setTable1(o1);

        context.commitChanges();
        assertTrue(o1.isValidatedForInsert());
        assertFalse(o1.isValidatedForDelete());
        assertFalse(o1.isValidatedForUpdate());

        o1.resetValidation(false);
        o1.setGlobalAttribute1("G2");

        context.commitChanges();
        assertFalse(o1.isValidatedForInsert());
        assertFalse(o1.isValidatedForDelete());
        assertTrue(o1.isValidatedForUpdate());

        o1.resetValidation(false);
        context.deleteObjects(o1);
        context.deleteObjects(o2);

        context.commitChanges();
        assertFalse(o1.isValidatedForInsert());
        assertTrue(o1.isValidatedForDelete());
        assertFalse(o1.isValidatedForUpdate());

        ClientMtTable1 o11 = context.newObject(ClientMtTable1.class);
        o11.setGlobalAttribute1("G1");
        o11.resetValidation(true);

        try {
            context.commitChanges();
            fail("Validation failure must have prevented commit");
        }
        catch (ValidationException e) {
            // expected
        }
    }
}
