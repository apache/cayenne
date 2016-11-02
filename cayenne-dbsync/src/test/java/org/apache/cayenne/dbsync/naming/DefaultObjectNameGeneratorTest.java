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
package org.apache.cayenne.dbsync.naming;

import org.apache.cayenne.dbsync.reverse.db.ExportedKey;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DefaultObjectNameGeneratorTest {

    private DefaultObjectNameGenerator generator = new DefaultObjectNameGenerator();


    @Test
    public void testDbRelationshipName_LowerCase_Underscores() {

        ExportedKey key = new ExportedKey("artist", "artist_id", null,
                "painting", "artist_id", null, (short) 1);
        assertEquals("artist", generator.dbRelationshipName(key, false));
        assertEquals("paintings", generator.dbRelationshipName(key, true));

        key = new ExportedKey("person", "person_id", null,
                "person", "mother_id", null, (short) 1);
        assertEquals("mother", generator.dbRelationshipName(key, false));
        assertEquals("people", generator.dbRelationshipName(key, true));

        key = new ExportedKey("person", "person_id", null,
                "address", "shipping_address_id", null, (short) 1);
        assertEquals("shippingAddress", generator.dbRelationshipName(key, false));
        assertEquals("addresses", generator.dbRelationshipName(key, true));
    }

    @Test
    public void testDbRelationshipName_UpperCase_Underscores() {

        ExportedKey key = new ExportedKey("ARTIST", "ARTIST_ID", null,
                "PAINTING", "ARTIST_ID", null, (short) 1);
        assertEquals("artist", generator.dbRelationshipName(key, false));
        assertEquals("paintings", generator.dbRelationshipName(key, true));

        key = new ExportedKey("PERSON", "PERSON_ID", null,
                "PERSON", "MOTHER_ID", null, (short) 1);
        assertEquals("mother", generator.dbRelationshipName(key, false));
        assertEquals("people", generator.dbRelationshipName(key, true));

        key = new ExportedKey("PERSON", "PERSON_ID", null,
                "ADDRESS", "SHIPPING_ADDRESS_ID", null, (short) 1);
        assertEquals("shippingAddress", generator.dbRelationshipName(key, false));
        assertEquals("addresses", generator.dbRelationshipName(key, true));
    }

    @Test
    public void testObjEntityName() {
        assertEquals("Artist", generator.objEntityName(new DbEntity("ARTIST")));
        assertEquals("ArtistWork", generator.objEntityName(new DbEntity("ARTIST_WORK")));
    }

    @Test
    public void testObjAttributeName() {
        assertEquals("name", generator.objAttributeName(new DbAttribute("NAME")));
        assertEquals("artistName", generator.objAttributeName(new DbAttribute("ARTIST_NAME")));
    }

    @Test
    public void testObjRelationshipName() {
        assertEquals("mother", generator.objRelationshipName(new DbRelationship("mother")));
        assertEquals("persons", generator.objRelationshipName(new DbRelationship("persons")));
    }

}
