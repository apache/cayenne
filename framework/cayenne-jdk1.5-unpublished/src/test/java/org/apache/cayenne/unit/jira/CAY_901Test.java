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
package org.apache.cayenne.unit.jira;

import java.util.Map;

import org.apache.art.Artist;
import org.apache.art.Gallery;
import org.apache.art.Painting;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.unit.CayenneCase;

public class CAY_901Test extends CayenneCase {

    public void testMultipleToOneDeletion() throws Exception {
        deleteTestData();

        ObjectContext context = createDataContext();

        Painting p = context.newObject(Painting.class);
        p.setPaintingTitle("P1");

        Artist a = context.newObject(Artist.class);
        a.setArtistName("A1");

        Gallery g = context.newObject(Gallery.class);
        g.setGalleryName("G1");

        p.setToArtist(a);
        p.setToGallery(g);
        context.commitChanges();

        p.setToArtist(null);
        p.setToGallery(null);

        context.commitChanges();

        SQLTemplate q = new SQLTemplate(Painting.class, "SELECT * from PAINTING");
        q.setColumnNamesCapitalization(CapsStrategy.UPPER);
        q.setFetchingDataRows(true);
        
        Map row = (Map) DataObjectUtils.objectForQuery(context, q);
        assertEquals("P1", row.get("PAINTING_TITLE"));
        assertEquals(null, row.get("ARTIST_ID"));
        assertEquals(null, row.get("GALLERY_ID"));
    }
}
