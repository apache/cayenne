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
package org.apache.cayenne.cache;

import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class QueryCacheTest extends ServerCase {

    @Inject
    private ObjectContext context1;
    
    @Inject
    private ObjectContext context2;
    
    public void testLocalCache() {
        
        Artist a = context1.newObject(Artist.class);
        a.setArtistName("artist");
        context1.commitChanges();
        
        SelectQuery q = new SelectQuery(Artist.class);
        q.setCacheStrategy(QueryCacheStrategy.LOCAL_CACHE);
        List<Artist> result1 = context1.performQuery(q);
        List<Artist> result2 = context2.performQuery(q);
        
        assertNotSame(
                result1.get(0).getObjectContext(), 
                result2.get(0).getObjectContext());
    }
}
