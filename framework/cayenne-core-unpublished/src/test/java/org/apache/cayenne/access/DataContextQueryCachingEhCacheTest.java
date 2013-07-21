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

import org.apache.cayenne.cache.EhCacheQueryCache;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextQueryCachingEhCacheTest extends DataContextQueryCachingTest {
    
    protected EhCacheQueryCache domainCache;
    protected EhCacheQueryCache contextCache;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");

        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");

        tPainting = new TableHelper(dbHelper, "PAINTING");
        tPainting.setColumns(
                "PAINTING_ID",
                "PAINTING_TITLE",
                "ARTIST_ID",
                "ESTIMATED_PRICE");

        domain = context.getParentDataDomain();
        oldCache = domain.getQueryCache();
        
        domainCache = new EhCacheQueryCache();
        contextCache = new EhCacheQueryCache();
        domain.setQueryCache(domainCache);
        context.setQueryCache(contextCache);
    }
    
    @Override
    protected void tearDownBeforeInjection() throws Exception {
        domainCache.shutdown();
        contextCache.shutdown();
    }
}
