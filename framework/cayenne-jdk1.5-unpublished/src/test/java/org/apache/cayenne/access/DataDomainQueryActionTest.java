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
package org.apache.cayenne.access;

import java.io.Serializable;
import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.cache.MockQueryCache;
import org.apache.cayenne.cache.QueryCache;
import org.apache.cayenne.cache.QueryCacheEntryFactory;
import org.apache.cayenne.query.QueryCacheStrategy;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;
import org.apache.art.Painting;
import org.apache.cayenne.unit.CayenneCase;


public class DataDomainQueryActionTest extends CayenneCase {

    @Override
    public void setUp() throws Exception {
        deleteTestData();
    }
    
    @Override
    public void tearDown() {
        getDomain().resetProperties();
    }
   
    public void testCachedQuery() {
        
        DataDomain domain = getDomain();
        
        ObjectContext context = createDataContext();
        
        Painting p = context.newObject(Painting.class);
        p.setPaintingTitle("sample");
        
        SelectQuery query = new SelectQuery(Painting.class);
        
        query.addPrefetch(Painting.TO_GALLERY_PROPERTY);
        query.addPrefetch(Painting.TO_ARTIST_PROPERTY);
        query.addOrdering(Painting.PAINTING_TITLE_PROPERTY, SortOrder.ASCENDING);
        query.setCacheStrategy(QueryCacheStrategy.SHARED_CACHE);
        query.setPageSize(5);
        
        QueryCache cache = domain.queryCache;
        
        domain.queryCache = new MockQueryCache() {
           
            @Override
            public List get(QueryMetadata metadata, QueryCacheEntryFactory factory) {
                Object results = factory.createObject();
                assertTrue("Query cache is not serializable.", results instanceof Serializable);
                
                return null;
            }
            
            @Override
            public void put(QueryMetadata metadata, List results) {
                assertTrue("Query cache is not serializable.", results instanceof Serializable);
            }
        };
        
        DataDomainQueryAction action = new DataDomainQueryAction(context, domain, query);
        action.execute();
        
        domain.queryCache = cache;
    }
    
}
