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

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.apache.cayenne.unit.util.ValidationDelegate;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 */
@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextValidationIT extends RuntimeCase {

    @Inject
    private DataContext context;

    @Test
    public void testValidatingObjectsOnCommitProperty() throws Exception {
        context.setValidatingObjectsOnCommit(true);
        assertTrue(context.isValidatingObjectsOnCommit());

        context.setValidatingObjectsOnCommit(false);
        assertFalse(context.isValidatingObjectsOnCommit());
    }

    @Test
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

    @Test
    public void testValidationModifyingContext() throws Exception {

        ValidationDelegate delegate = (object, validationResult) -> {

            Artist a = (Artist) object;
            Painting p = a.getObjectContext().newObject(Painting.class);
            p.setPaintingTitle("XXX");
            p.setToArtist(a);
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

        assertEquals(2, ObjectSelect.query(Painting.class).select(context).size());
    }
}
