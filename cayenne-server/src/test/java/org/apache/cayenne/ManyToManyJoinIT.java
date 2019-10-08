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
package org.apache.cayenne;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.testdo.relationships_many_to_many_join.Author;
import org.apache.cayenne.testdo.relationships_many_to_many_join.Song;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@UseServerRuntime(CayenneProjects.RELATIONSHIPS_MANY_TO_MANY_JOIN_PROJECT)
public class ManyToManyJoinIT extends ServerCase {

    @Inject
    private ServerRuntime serverRuntime;

    @Inject
    private ObjectContext context;

    @Test
    public void testManyToManyJoinWithFlattenedRelationship() {
    	Author author = context.newObject(Author.class);
    	author.setName("Bob Dylan");

        Song song = context.newObject(Song.class);
        song.setName("House of the Rising Sun");

        song.addToAuthors(author);

        context.commitChanges();
        assertEquals(author, song.getAuthors().iterator().next());
    }

    @Test
    public void testManyToManyJoinWithFlattenedRelationshipEvents() {
        DataDomain domain = serverRuntime.getDataDomain();
        DataChannelFilter f1 = new MockDataChannelFilter() {
            @Override
            public GraphDiff onSync(
                    ObjectContext originatingContext,
                    GraphDiff changes,
                    int syncType,
                    DataChannelFilterChain filterChain) {

                GraphDiff response = filterChain.onSync(
                        originatingContext,
                        changes,
                        syncType);
                final AtomicInteger count = new AtomicInteger(0);
                changes.apply(new GraphChangeHandler() {
                    @Override
                    public void nodeIdChanged(Object nodeId, Object newId) {}

                    @Override
                    public void nodeCreated(Object nodeId) {}

                    @Override
                    public void nodeRemoved(Object nodeId) {}

                    @Override
                    public void nodePropertyChanged(Object nodeId, String property, Object oldValue, Object newValue) {}

                    @Override
                    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
                        count.incrementAndGet();
                    }

                    @Override
                    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {}
                });
                assertEquals(1, count.get());
                return response;
            }
        };
        domain.addFilter(f1);
        Author author = context.newObject(Author.class);
        author.setName("Bob Dylan");
        Song song = context.newObject(Song.class);
        song.setName("House of the Rising Sun");
        song.addToAuthors(author);
        context.commitChanges();
    }
}
