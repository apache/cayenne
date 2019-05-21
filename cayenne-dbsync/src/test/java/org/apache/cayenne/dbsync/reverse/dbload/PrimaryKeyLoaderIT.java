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

package org.apache.cayenne.dbsync.reverse.dbload;

import java.util.Collection;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PrimaryKeyLoaderIT extends BaseLoaderIT {

    @Test
    public void testPrimaryKeyLoad() throws Exception {
        createDbEntities();
        DbEntity artist = getDbEntity(nameForDb("ARTIST"));
        DbAttribute artistId = new DbAttribute(nameForDb("ARTIST_ID"));
        DbAttribute artistName = new DbAttribute(nameForDb("ARTIST_NAME"));
        DbAttribute artistId1 = new DbAttribute(nameForDb("ARTIST_ID1"));

        artist.addAttribute(artistId);
        artist.addAttribute(artistName);
        artist.addAttribute(artistId1);
        assertFalse(artistId.isPrimaryKey());
        assertFalse(artistName.isPrimaryKey());
        assertFalse(artistId1.isPrimaryKey());

        PrimaryKeyLoader loader = new PrimaryKeyLoader(EMPTY_CONFIG, new DefaultDbLoaderDelegate());
        loader.load(connection.getMetaData(), store);

        assertTrue(artistId.isPrimaryKey());
        assertFalse(artistId1.isPrimaryKey());
        assertFalse(artistName.isPrimaryKey());
        Collection<DbAttribute> pk = artist.getPrimaryKeys();
        assertEquals(1, pk.size());
        assertEquals(artistId, pk.iterator().next());
    }

}
