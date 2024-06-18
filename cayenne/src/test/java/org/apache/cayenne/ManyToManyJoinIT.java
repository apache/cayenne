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

import static org.junit.Assert.*;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.testdo.relationships_many_to_many_join.Author;
import org.apache.cayenne.testdo.relationships_many_to_many_join.SelfRelationship;
import org.apache.cayenne.testdo.relationships_many_to_many_join.SelfRelationshipSub;
import org.apache.cayenne.testdo.relationships_many_to_many_join.Song;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

@UseCayenneRuntime(CayenneProjects.RELATIONSHIPS_MANY_TO_MANY_JOIN_PROJECT)
public class ManyToManyJoinIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Test
    public void testManyToManyJoinWithFlattenedRelationship() throws Exception {
    	Author author = context.newObject(Author.class);
    	author.setName("Bob Dylan");
    	
        Song song = context.newObject(Song.class);
        song.setName("House of the Rising Sun");

        song.addToAuthors(author);
        
        context.commitChanges();
        assertEquals(author, song.getAuthors().iterator().next());
    }

    @Test
    public void testManyToManySelfRelationship() {
        SelfRelationship parent1 = context.newObject(SelfRelationship.class);
        parent1.setName("parent1");

        SelfRelationshipSub child1 = context.newObject(SelfRelationshipSub.class);
        child1.setName("child1");

        SelfRelationshipSub child2 = context.newObject(SelfRelationshipSub.class);
        child2.setName("child2");

        // this sets both forward and reverse relationships
        child2.addToSelfParents(parent1);

        // this still couldn't set reverse relationship, as it present in the Subclass only
        parent1.addToSelfChildren(child1);

        context.commitChanges();

        assertEquals(2, parent1.getSelfChildren().size());

        assertEquals(1, child2.getSelfParents().size());
        assertEquals(1, child1.getSelfParents().size());
    }

}
