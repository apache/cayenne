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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.graph.ArcId;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.NullTestEntity;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.List;

public class DataContextCommitIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void flushToParent_Commit_New() {
        DataContext context = env.context();

        // commit new object
        Artist a = context.newObject(Artist.class);
        a.setArtistName("Test");

        assertTrue(context.hasChanges());

        ObjectId beforeId = a.getObjectId();
        GraphDiff diff = context.flushToParent(true);
        ObjectId afterId = a.getObjectId();

        assertNotNull(diff);
        assertFalse(context.hasChanges());
        assertNotEquals(beforeId, afterId);

        RecordingChangeHandler diffChecker = new RecordingChangeHandler();

        diff.apply(diffChecker);

        assertEquals(List.of("nodeIdChanged(" + beforeId + ", " + afterId + ")"), diffChecker.calls);
    }

    @Test
    public void flushToParent_Commit_Mix() {
        DataContext context = env.context();

        Artist a = context.newObject(Artist.class);
        a.setArtistName("Test");
        context.flushToParent(true);

        // commit a mix of new and modified
        Painting p = context.newObject(Painting.class);
        p.setPaintingTitle("PT");
        p.setArtist(a);
        a.setArtistName("Test_");

        ObjectId beforeId = p.getObjectId();
        GraphDiff diff = context.flushToParent(true);
        ObjectId afterId = p.getObjectId();

        assertNotNull(diff);
        assertFalse(context.hasChanges());

        RecordingChangeHandler diffChecker = new RecordingChangeHandler();

        diff.apply(diffChecker);
        assertEquals(List.of("nodeIdChanged(" + beforeId + ", " + afterId + ")"), diffChecker.calls);
    }

    @Test
    public void flushToParent_NewNoAttributes() {
        DataContext context = env.context();

        // commit new object with uninitialized attributes

        context.newObject(NullTestEntity.class);

        assertTrue(context.hasChanges());

        GraphDiff diff3 = context.flushToParent(true);
        assertNotNull(diff3);
        assertFalse(context.hasChanges());
    }

    /**
     * Records the calls it receives as strings, to compare a diff with the expected changes.
     */
    static class RecordingChangeHandler implements GraphChangeHandler {

        final List<String> calls = new ArrayList<>();

        @Override
        public void nodeIdChanged(ObjectId id, ObjectId newId) {
            calls.add("nodeIdChanged(" + id + ", " + newId + ")");
        }

        @Override
        public void nodeCreated(ObjectId id) {
            calls.add("nodeCreated(" + id + ")");
        }

        @Override
        public void nodeRemoved(ObjectId id) {
            calls.add("nodeRemoved(" + id + ")");
        }

        @Override
        public void nodePropertyChanged(ObjectId id, String property, Object oldValue, Object newValue) {
            calls.add("nodePropertyChanged(" + id + ", " + property + ")");
        }

        @Override
        public void arcCreated(ObjectId id, ObjectId targetId, ArcId arcId) {
            calls.add("arcCreated(" + id + ", " + targetId + ", " + arcId + ")");
        }

        @Override
        public void arcDeleted(ObjectId id, ObjectId targetId, ArcId arcId) {
            calls.add("arcDeleted(" + id + ", " + targetId + ", " + arcId + ")");
        }
    }
}
