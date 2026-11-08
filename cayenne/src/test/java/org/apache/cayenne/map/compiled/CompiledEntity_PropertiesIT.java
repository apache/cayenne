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
package org.apache.cayenne.map.compiled;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Object access through the compiled attributes and relationships of an entity with a persistent class.
 */
public class CompiledEntity_PropertiesIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void entity() {
        CompiledEntity artist = env.mapping().entity("Artist");
        assertSame(artist, env.mapping().entity(Artist.class));
        assertSame(artist, env.mapping().entity(new Artist()));
        assertEquals(Artist.class, artist.objectClass());
        assertNull(artist.superEntity());
        assertFalse(artist.hasSubEntities());
        assertInstanceOf(Artist.class, artist.createObject());
    }

    @Test
    public void attributes() {
        CompiledEntity artist = env.mapping().entity("Artist");
        CompiledAttribute name = artist.attribute("artistName");
        assertNotNull(name);
        assertSame(artist, name.entity());
        assertEquals(String.class, name.valueClass());
        assertEquals("ARTIST_NAME", name.columnName());

        Artist a = new Artist();
        name.writePropertyDirectly(a, null, "a1");
        assertEquals("a1", a.getArtistName());
        assertEquals("a1", name.readPropertyDirectly(a));

        a.setArtistName("a2");
        assertEquals("a2", name.readProperty(a));
        assertTrue(name.equals("a2", "a2"));
        assertFalse(name.equals("a2", "a3"));

        // a PK column with no property is a synthetic attribute, outside the regular attributes
        assertEquals(1, artist.primaryKeyAttributes().size());
        CompiledAttribute pk = artist.primaryKeyAttributes().getFirst();
        assertTrue(pk.isSynthetic());
        assertTrue(pk.isPrimaryKey());
        assertEquals("ARTIST_ID", pk.columnName());
        assertFalse(artist.attributes().contains(pk));
    }

    @Test
    public void relationships() {
        CompiledEntity artist = env.mapping().entity("Artist");
        CompiledEntity painting = env.mapping().entity("Painting");

        CompiledRelationship paintings = artist.relationship(Artist.PAINTINGS.getName());
        assertInstanceOf(CompiledToManyRelationship.class, paintings);
        assertSame(painting, paintings.target());
        assertSame(artist, paintings.source());

        CompiledRelationship reverse = paintings.reverse();
        assertNotNull(reverse);
        assertEquals(Painting.ARTIST.getName(), reverse.name());
        assertInstanceOf(CompiledToOneRelationship.class, reverse);
        assertSame(painting.relationship(Painting.ARTIST.getName()), reverse);
        assertSame(paintings, reverse.reverse());
        assertEquals("artist", paintings.reverseDbPath().value());
        assertEquals("paintings", reverse.reverseDbPath().value());
    }

    @Test
    public void shallowMerge() {
        ObjectContext context = env.context();
        CompiledEntity artistEntity = env.mapping().entity("Artist");

        Artist from = context.newObject(Artist.class);
        from.setArtistName("123");
        from.setDateOfBirth(new Date(0));
        context.commitChanges();

        Artist to = new Artist();
        artistEntity.shallowMerge(from, to);
        assertEquals("123", to.getArtistName());
        assertEquals(new Date(0), to.getDateOfBirth());
    }

    @Test
    public void shallowMergeInvalidatesToOne() {
        ObjectContext context = env.context();
        CompiledEntity paintingEntity = env.mapping().entity("Painting");

        Artist artist = context.newObject(Artist.class);
        artist.setArtistName("a");
        Painting from = context.newObject(Painting.class);
        from.setPaintingTitle("p");
        from.setArtist(artist);
        context.commitChanges();

        Painting to = context.newObject(Painting.class);
        to.setArtist(artist);
        paintingEntity.shallowMerge(from, to);
        assertEquals("p", to.getPaintingTitle());

        // the to-one of the target is replaced with a fault, the to-many left alone
        CompiledRelationship toOne = paintingEntity.relationship(Painting.ARTIST.getName());
        assertTrue(toOne.isFaultValue(toOne.readPropertyDirectly(to)));
    }
}
