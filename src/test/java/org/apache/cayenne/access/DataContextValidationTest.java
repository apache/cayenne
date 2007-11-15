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

import org.apache.art.Artist;
import org.apache.art.Painting;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.util.ValidationDelegate;
import org.apache.cayenne.validation.ValidationResult;

/**
 * @author Andrus Adamchik
 */
public class DataContextValidationTest extends CayenneCase {

    public void testValidatingObjectsOnCommitProperty() throws Exception {
        DataContext context = createDataContext();

        context.setValidatingObjectsOnCommit(true);
        assertTrue(context.isValidatingObjectsOnCommit());

        context.setValidatingObjectsOnCommit(false);
        assertFalse(context.isValidatingObjectsOnCommit());
    }

    public void testValidatingObjectsOnCommit() throws Exception {
        DataContext context = createDataContext();

        // test that validation is called properly

        context.setValidatingObjectsOnCommit(true);
        Artist a1 = (Artist) context.newObject(Artist.class);
        a1.setArtistName("a1");
        context.commitChanges();
        assertTrue(a1.isValidateForSaveCalled());

        context.setValidatingObjectsOnCommit(false);
        Artist a2 = (Artist) context.newObject(Artist.class);
        a2.setArtistName("a2");
        context.commitChanges();
        assertFalse(a2.isValidateForSaveCalled());
    }

    public void testValidationModifyingContext() throws Exception {
        deleteTestData();

        ValidationDelegate delegate = new ValidationDelegate() {

            public void validateForSave(Object object, ValidationResult validationResult) {

                Artist a = (Artist) object;
                Painting p = (Painting) a.getObjectContext().newObject(Painting.class);
                p.setPaintingTitle("XXX");
                p.setToArtist(a);
            }
        };

        DataContext context = createDataContext();

        context.setValidatingObjectsOnCommit(true);
        Artist a1 = (Artist) context.newObject(Artist.class);
        a1.setValidationDelegate(delegate);
        a1.setArtistName("a1");

        // add another artist to ensure that modifying context works when more than one
        // object is committed
        Artist a2 = (Artist) context.newObject(Artist.class);
        a2.setValidationDelegate(delegate);
        a2.setArtistName("a2");
        context.commitChanges();

        assertEquals(2, context.performQuery(new SelectQuery(Painting.class)).size());
    }
}
