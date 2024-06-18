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
package org.apache.cayenne.access;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.List;

import static org.junit.Assert.assertEquals;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextJoinAliasesIT extends RuntimeCase {
    
    @Inject
    ObjectContext context;
    
    @Inject
    DBHelper dbHelper;
    
    protected TableHelper tArtist;
    protected TableHelper tExhibit;
    protected TableHelper tGallery;
    protected TableHelper tArtistExhibit;

    @Before
    public void setUp() throws Exception {
        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");
        
        tExhibit = new TableHelper(dbHelper, "EXHIBIT");
        tExhibit.setColumns("EXHIBIT_ID", "GALLERY_ID", "OPENING_DATE", "CLOSING_DATE");
        
        tGallery = new TableHelper(dbHelper, "GALLERY");
        tGallery.setColumns("GALLERY_ID", "GALLERY_NAME");
        
        tArtistExhibit = new TableHelper(dbHelper, "ARTIST_EXHIBIT");
        tArtistExhibit.setColumns("EXHIBIT_ID", "ARTIST_ID");
    }
    
    protected void createMatchAllDataSet() throws Exception {
        tArtist.insert(1, "Picasso");
        tArtist.insert(2, "Dali");
        tArtist.insert(3, "X");
        tArtist.insert(4, "Y");
        tGallery.insert(1, "G1");
        tGallery.insert(2, "G2");
        tGallery.insert(3, "G3");
        
        Timestamp now = new Timestamp(System.currentTimeMillis());
        
        tExhibit.insert(1, 2, now, now);
        tExhibit.insert(2, 2, now, now);
        tExhibit.insert(3, 1, now, now);
        tExhibit.insert(4, 1, now, now);
        tExhibit.insert(5, 3, now, now);
        
        tArtistExhibit.insert(1, 1);
        tArtistExhibit.insert(1, 3);
        tArtistExhibit.insert(3, 1);
        tArtistExhibit.insert(4, 2);
        tArtistExhibit.insert(4, 4);
        tArtistExhibit.insert(5, 2);
    }

    @Test
    public void testMatchAll() throws Exception {
        // select all galleries that have exhibits by both Picasso and Dali...

        createMatchAllDataSet();

        Artist picasso = Cayenne.objectForPK(context, Artist.class, 1);
        Artist dali = Cayenne.objectForPK(context, Artist.class, 2);

        List<Gallery> galleries = ObjectSelect.query(Gallery.class)
                .where(ExpressionFactory.matchAllExp("|exhibitArray.artistExhibitArray.toArtist", picasso, dali))
                .select(context);

        assertEquals(1, galleries.size());
        assertEquals("G1", galleries.get(0).getGalleryName());

    }

}
