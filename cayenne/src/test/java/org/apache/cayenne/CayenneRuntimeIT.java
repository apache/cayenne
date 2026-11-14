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
package org.apache.cayenne;

import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.apache.cayenne.unit.RecordingTransactionListener;
import org.apache.cayenne.validation.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CayenneRuntimeIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void performInTransaction_Local_Callback() {

        RecordingTransactionListener callback = new RecordingTransactionListener();

        Artist a = env.runtime().performInTransaction(() -> {
            Artist localArtist = env.runtime().newContext().newObject(Artist.class);
            localArtist.setArtistName("A1");
            localArtist.getObjectContext().commitChanges();
            return localArtist;
        }, callback);

        assertEquals("A1", a.getArtistName());
        assertEquals(PersistenceState.COMMITTED, a.getPersistenceState());
        assertEquals(1, callback.willCommitCalls);
        assertEquals(1, callback.willAddConnectionCalls);
        assertEquals(0, callback.willRollbackCalls);
        assertEquals(1, callback.decorateConnectionCalls);
    }

    @Test
    public void performInTransaction_Local_Callback_Rollback() {

        RecordingTransactionListener callback = new RecordingTransactionListener();

        try {
            env.runtime().performInTransaction(() -> {
                Artist localArtist = env.runtime().newContext().newObject(Artist.class);
                localArtist.getObjectContext().commitChanges();
                return localArtist;
            }, callback);
        } catch (ValidationException v) {
            assertEquals(1, callback.willRollbackCalls);
            assertEquals(0, callback.willAddConnectionCalls);
            assertEquals(0, callback.willCommitCalls);
        }
    }

    @Test
    public void rollbackTransaction() {
        assertEquals(0, ObjectSelect.query(Artist.class).selectCount(env.context()));

        try {
            env.runtime().performInTransaction(() -> {
                // Default PK batch size is 20
                for (int i = 0; i < 30; i++) {
                    Artist artist = env.context().newObject(Artist.class);
                    artist.setArtistName("test" + i);
                    env.context().commitChanges();
                }

                // this should fail with validation error
                env.context().newObject(Artist.class);
                env.context().commitChanges();
                return null;
            });
        } catch (Exception ignored) {
        }

        assertEquals(0, ObjectSelect.query(Artist.class).selectCount(env.context()));
    }
}
