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
package org.apache.cayenne.unit;

import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.opentest4j.AssertionFailedError;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Self-test for CayenneTestsEnv
 */
public class CayenneTestEnvIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private TableHelper tArtist;

    @BeforeEach
    public void setUp() {
        tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME");
    }

    @Test
    public void blockingQueries_failsOnUncachedSelect() throws Exception {
        tArtist.insert(1, "a1");

        assertThrows(AssertionFailedError.class,
                () -> env.runWithQueriesBlocked(
                        () -> ObjectSelect.query(Artist.class).select(env.context())));
    }

    @Test
    public void blockingQueries_resetsAfterTaskThrows() throws Exception {
        tArtist.insert(1, "a1");

        assertThrows(RuntimeException.class,
                () -> env.runWithQueriesBlocked(() -> {
                    throw new RuntimeException("boom");
                }));

        // flag must have been cleared by the finally block in RuntimeTelemetry
        assertDoesNotThrow(() -> ObjectSelect.query(Artist.class).select(env.context()));
    }
}
