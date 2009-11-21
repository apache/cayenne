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
package org.apache.cayenne.jpa.itest.ch2;

import org.apache.cayenne.itest.jpa.EntityManagerCase;
import org.apache.cayenne.jpa.itest.ch2.entity.BidiOneToManyOwned;
import org.apache.cayenne.jpa.itest.ch2.entity.BidiOneToManyOwner;

public class _2_1_8_2_BidiManyToOneRelationshipTest extends EntityManagerCase {

    public void testDefaultsSelect() throws Exception {
        getDbHelper().deleteAll("BidiOneToManyOwner");
        getDbHelper().deleteAll("BidiOneToManyOwned");

        getDbHelper().insert("BidiOneToManyOwned", new String[] {
            "id"
        }, new Object[] {
            5
        });

        getDbHelper().insert("BidiOneToManyOwner", new String[] {
                "id", "owned_id"
        }, new Object[] {
                4, 5
        });

        getDbHelper().insert("BidiOneToManyOwned", new String[] {
            "id"
        }, new Object[] {
            6
        });

        getDbHelper().insert("BidiOneToManyOwner", new String[] {
                "id", "owned_id"
        }, new Object[] {
                5, 6
        });

        BidiOneToManyOwned owned = getEntityManager().find(BidiOneToManyOwned.class, 5);
        assertNotNull(owned);
        assertEquals(1, owned.getOwners().size());

        BidiOneToManyOwner owner = getEntityManager().find(BidiOneToManyOwner.class, 4);
        assertNotNull(owner);
        assertTrue(owned.getOwners().contains(owner));
        
        BidiOneToManyOwner owner1 = getEntityManager().find(BidiOneToManyOwner.class, 5);
        assertNotNull(owner1);
        assertFalse(owned.getOwners().contains(owner1));
    }
}
