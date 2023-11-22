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

package org.apache.cayenne.exp.property;

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectId;
import org.junit.Test;

import static org.apache.cayenne.exp.ExpressionFactory.*;
import static org.apache.cayenne.exp.property.PropertyFactory.createNumericId;
import static org.junit.Assert.*;

/**
 * @since 4.2
 */
public class IdPropertyTest {

    @Test
    public void expressionContentAttribute() {
        assertEquals(dbIdPathExp("ARTIST_ID"),
                createNumericId("ARTIST_ID", "Artist", Long.class).getExpression());
    }

    @Test
    public void expressionContentPathAttribute() {
        assertEquals(dbIdPathExp("path.ARTIST_ID"),
                createNumericId("ARTIST_ID", "path", "Artist", Long.class).getExpression());
    }

    @Test
    public void eqObjectIdAttribute() {
        ObjectId id = ObjectId.of("Artist", "ARTIST_ID", 2);
        assertEquals(matchDbIdExp("ARTIST_ID", 2),
                createNumericId("ARTIST_ID", "Artist", Integer.class).eq(id));
    }

    @Test(expected = CayenneRuntimeException.class)
    public void eqObjectIdWrongAttribute() {
        ObjectId id = ObjectId.of("Artist", "ARTIST_ID", 2);
        createNumericId("ARTIST_PK", "Artist", Integer.class).eq(id);
    }

    @Test
    public void eqObjectIdCompound() {
        Map<String, Object> key = new HashMap<>();
        key.put("ARTIST_ID", 2);
        key.put("SERIAL", 1);
        ObjectId id = ObjectId.of("Artist", key);
        assertEquals(matchDbIdExp("ARTIST_ID", 2),
                createNumericId("ARTIST_ID", "Artist", Integer.class).eq(id));
    }

    @Test(expected = CayenneRuntimeException.class)
    public void eqObjectIdWrongCompound() {
        Map<String, Object> key = new HashMap<>();
        key.put("ARTIST_ID", 2);
        key.put("SERIAL", 1);
        ObjectId id = ObjectId.of("Artist", key);
        createNumericId("Artist", "ARTIST_ID", Integer.class).eq(id);
    }
}