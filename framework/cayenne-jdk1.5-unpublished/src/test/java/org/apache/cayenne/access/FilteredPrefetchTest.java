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

import org.apache.cayenne.unit.CayenneCase;

public class FilteredPrefetchTest extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    /**
     * Test prefetching with qualifier on the root query containing the path to the
     * prefetch.
     */
    public void testDisjointToManyConflictingQualifier() throws Exception {
        // TODO: CAY-319 - this is a known nasty limitation.
        // createTestData("testDisjointToManyConflictingQualifier");
        //
        // DataContext context = createDataContext();
        // Expression exp = ExpressionFactory.matchExp("paintingArray.paintingTitle",
        // "P_artist12");
        //
        // SelectQuery q = new SelectQuery(Artist.class, exp);
        // q.addPrefetch("paintingArray");
        //
        // List results = context.performQuery(q);
        //        
        // // block further queries
        // context.setDelegate(new QueryBlockingDelegate());
        // assertEquals(1, results.size());
        //
        // Artist a = (Artist) results.get(0);
        //        
        // List paintings = a.getPaintingArray();
        // assertFalse(((ToManyList) paintings).needsFetch());
        // assertEquals(2, paintings.size());
    }
}
