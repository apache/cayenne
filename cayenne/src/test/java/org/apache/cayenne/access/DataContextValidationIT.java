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

import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class DataContextValidationIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private DataContext newContext(boolean validatingObjectsOnCommit) {
        DataDomain domain = env.runtime().getDataDomain();
        return DataContext.builder(domain)
                .snapshotCache(domain.getSharedSnapshotCache())
                .validatingObjectsOnCommit(validatingObjectsOnCommit)
                .build();
    }

    @Test
    public void validatingObjectsOnCommitProperty() {
        assertTrue(newContext(true).isValidatingObjectsOnCommit());
        assertFalse(newContext(false).isValidatingObjectsOnCommit());
    }

    @Test
    public void validatingObjectsOnCommit() {
        // test that validation is called properly

        DataContext validating = newContext(true);
        Artist a1 = validating.newObject(Artist.class);
        a1.setArtistName("a1");
        validating.commitChanges();
        assertTrue(a1.isValidateForSaveCalled());

        DataContext nonValidating = newContext(false);
        Artist a2 = nonValidating.newObject(Artist.class);
        a2.setArtistName("a2");
        nonValidating.commitChanges();
        assertFalse(a2.isValidateForSaveCalled());
    }

    @Test
    public void validationModifyingContext() {

        Consumer<Artist> callback = a -> {
            Painting p = a.getObjectContext().newObject(Painting.class);
            p.setPaintingTitle("XXX");
            p.setToArtist(a);
        };

        assertTrue(env.context().isValidatingObjectsOnCommit());
        Artist a1 = env.context().newObject(Artist.class);
        a1.setValidationCallback(callback);
        a1.setArtistName("a1");

        // add another artist to ensure that modifying context works when more than one
        // object is committed
        Artist a2 = env.context().newObject(Artist.class);
        a2.setValidationCallback(callback);
        a2.setArtistName("a2");
        env.context().commitChanges();

        assertEquals(2, ObjectSelect.query(Painting.class).select(env.context()).size());
    }
}
