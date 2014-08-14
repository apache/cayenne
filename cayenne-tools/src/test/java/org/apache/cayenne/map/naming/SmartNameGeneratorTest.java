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
package org.apache.cayenne.map.naming;

import junit.framework.TestCase;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;

public class SmartNameGeneratorTest extends TestCase {
    public void testStrategy() throws Exception {
        SmartNameGenerator strategy = new SmartNameGenerator();
        
        ExportedKey key = new ExportedKey("ARTIST", "ARTIST_ID", null,
                "PAINTING", "ARTIST_ID", null);
        assertEquals(strategy.createDbRelationshipName(key, false), "artist"); 
        assertEquals(strategy.createDbRelationshipName(key, true), "paintings");
        
        key = new ExportedKey("PERSON", "PERSON_ID", null,
                "PERSON", "MOTHER_ID", null);
        assertEquals(strategy.createDbRelationshipName(key, false), "mother"); 
        assertEquals(strategy.createDbRelationshipName(key, true), "people");
        
        key = new ExportedKey("PERSON", "PERSON_ID", null,
                "ADDRESS", "SHIPPING_ADDRESS_ID", null);
        assertEquals(strategy.createDbRelationshipName(key, false), "shippingAddress"); 
        assertEquals(strategy.createDbRelationshipName(key, true), "addresses");
        
        assertEquals(strategy.createObjEntityName(new DbEntity("ARTIST")), "Artist");
        assertEquals(strategy.createObjEntityName(new DbEntity("ARTIST_WORK")), "ArtistWork");
        
        assertEquals(strategy.createObjAttributeName(new DbAttribute("NAME")), "name");
        assertEquals(strategy.createObjAttributeName(new DbAttribute("ARTIST_NAME")), "artistName");
        
        assertEquals(strategy.createObjRelationshipName(new DbRelationship("mother")), "mother");
        assertEquals(strategy.createObjRelationshipName(new DbRelationship("persons")), "persons");
    }
}
