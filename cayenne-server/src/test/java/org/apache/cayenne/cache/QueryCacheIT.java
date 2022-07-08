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
package org.apache.cayenne.cache;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

@UseServerRuntime(CayenneProjects.TESTMAP_PROJECT)
public class QueryCacheIT extends ServerCase {

    @Inject
    private ObjectContext context1;
    
    @Inject
    private ObjectContext context2;

    @Test
    public void testLocalCache() {
        
        Artist a = context1.newObject(Artist.class);
        a.setArtistName("artist");
        context1.commitChanges();
        
        ObjectSelect<Artist> q = ObjectSelect.query(Artist.class).localCache();
        List<Artist> result1 = q.select(context1);
        List<Artist> result2 = q.select(context2);
        
        assertNotSame(
                result1.get(0).getObjectContext(), 
                result2.get(0).getObjectContext());
    }
    
    @Test(expected = UnsupportedOperationException.class)
    public void testLocalCacheStoresAnImmutableList() {
        
        Artist a = context1.newObject(Artist.class);
        a.setArtistName("artist");
        context1.commitChanges();
        
        ObjectSelect<Artist> q = ObjectSelect.query(Artist.class).localCache();
        List<Artist> result1 = context1.performQuery(q);
        assertEquals(1, result1.size());
        
        // Mutate the returned list. This should not change the cache.
        result1.add(context1.newObject(Artist.class));
        List<Artist> result2 = context1.performQuery(q);
        assertEquals("the list stored in the local query cache cannot be mutated after being returned", 1, result2.size());

        result2.add(context1.newObject(Artist.class));
        List<Artist> result3 = context1.performQuery(q);
        assertEquals("the list stored in the local query cache cannot be mutated after being returned", 1, result3.size());
    }
    
    @Test
    public void testSharedCacheStoresAnImmutableList() {
        
        Artist a = context1.newObject(Artist.class);
        a.setArtistName("artist");
        context1.commitChanges();
        
        ObjectSelect<Artist> q = ObjectSelect.query(Artist.class).sharedCache();
        List<Artist> result1 = context1.performQuery(q);
        assertEquals(1, result1.size());
        
        // Mutate the returned list. This should not change the cache.
        result1.add(context1.newObject(Artist.class));
        List<Artist> result2 = context1.performQuery(q);
        assertEquals("the list stored in the shared query cache cannot be mutated after being returned", 1, result2.size());
    }
}
