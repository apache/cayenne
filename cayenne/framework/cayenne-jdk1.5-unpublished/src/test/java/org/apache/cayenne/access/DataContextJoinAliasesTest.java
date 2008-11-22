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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.art.Artist;
import org.apache.art.Gallery;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;

public class DataContextJoinAliasesTest extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testMatchAll() throws Exception {
        Map<String, Object> params = new HashMap<String, Object>();
        Date now = new Date();
        params.put("date1", now);
        params.put("date2", now);
        createTestData("testMatchAll", params);

        // select all galleries that have exhibits by both Picasso and Dali...

        ObjectContext context = createDataContext();

        Artist picasso = DataObjectUtils.objectForPK(context, Artist.class, 1);
        Artist dali = DataObjectUtils.objectForPK(context, Artist.class, 2);

        SelectQuery query = new SelectQuery(Gallery.class);
        query.andQualifier(ExpressionFactory.matchAllExp(
                "|exhibitArray.artistExhibitArray.toArtist",
                picasso,
                dali));

        List<Gallery> galleries = context.performQuery(query);

        assertEquals(1, galleries.size());
        assertEquals("G1", galleries.get(0).getGalleryName());
    }

}
