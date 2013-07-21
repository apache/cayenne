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

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.apache.cayenne.unit.util.ValidationDelegate;
import org.apache.cayenne.validation.ValidationResult;

/**
 */
@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextValidationTest extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("PAINTING1");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");
    }

    public void testValidatingObjectsOnCommitProperty() throws Exception {
        context.setValidatingObjectsOnCommit(true);
        assertTrue(context.isValidatingObjectsOnCommit());

        context.setValidatingObjectsOnCommit(false);
        assertFalse(context.isValidatingObjectsOnCommit());
    }

    public void testValidatingObjectsOnCommit() throws Exception {
        // test that validation is called properly

        context.setValidatingObjectsOnCommit(true);
        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("a1");
        context.commitChanges();
        assertTrue(a1.isValidateForSaveCalled());

        context.setValidatingObjectsOnCommit(false);
        Artist a2 = context.newObject(Artist.class);
        a2.setArtistName("a2");
        context.commitChanges();
        assertFalse(a2.isValidateForSaveCalled());
    }

    public void testValidationModifyingContext() throws Exception {

        ValidationDelegate delegate = new ValidationDelegate() {

            public void validateForSave(Object object, ValidationResult validationResult) {

                Artist a = (Artist) object;
                Painting p = a.getObjectContext().newObject(Painting.class);
                p.setPaintingTitle("XXX");
                p.setToArtist(a);
            }
        };

        context.setValidatingObjectsOnCommit(true);
        Artist a1 = context.newObject(Artist.class);
        a1.setValidationDelegate(delegate);
        a1.setArtistName("a1");

        // add another artist to ensure that modifying context works when more than one
        // object is committed
        Artist a2 = context.newObject(Artist.class);
        a2.setValidationDelegate(delegate);
        a2.setArtistName("a2");
        context.commitChanges();

        assertEquals(2, context.performQuery(new SelectQuery(Painting.class)).size());
    }
}
