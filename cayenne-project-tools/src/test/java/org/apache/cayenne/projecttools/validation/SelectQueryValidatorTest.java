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
package org.apache.cayenne.projecttools.validation;

import java.util.List;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.apache.cayenne.validation.ValidationFailure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SelectQueryValidatorTest {

    private DataMap map;

    @BeforeEach
    public void createMap() {
        map = new DataMap("map");

        DbEntity artistTable = new DbEntity("ARTIST");
        artistTable.addAttribute(new DbAttribute("ARTIST_ID"));
        artistTable.addAttribute(new DbAttribute("NAME"));
        map.addDbEntity(artistTable);

        ObjEntity artist = new ObjEntity("Artist");
        artist.setDbEntityName("ARTIST");
        artist.addAttribute(new ObjAttribute("name", String.class.getName(), artist));
        map.addObjEntity(artist);

        ObjEntity painting = new ObjEntity("Painting");
        painting.addAttribute(new ObjAttribute("title", String.class.getName(), painting));
        map.addObjEntity(painting);

        ObjRelationship paintings = new ObjRelationship("paintings");
        paintings.setTargetEntityName("Painting");
        artist.addRelationship(paintings);

        ObjRelationship toArtist = new ObjRelationship("artist");
        toArtist.setTargetEntityName("Artist");
        painting.addRelationship(toArtist);
    }

    @Test
    public void valid() {
        assertEquals(List.of(), validate("""
                select name, count(paintings) as total from Artist
                where name like $name and exists (from Painting where artist = enclosing(self))
                having count(paintings) > 1
                order by total desc
                prefetch paintings joint"""));
        assertEquals(List.of(), validate("from db:ARTIST where NAME = 'a' order by ARTIST_ID"));
        assertEquals(List.of(), validate("from Artist where db:NAME = 'a'"));
    }

    @Test
    public void empty() {
        assertEquals(List.of("Query 'q' is empty"), validate(" "));
    }

    @Test
    public void invalidSyntax() {
        List<String> failures = validate("from Artist where");
        assertEquals(1, failures.size());
        assertTrue(failures.get(0).startsWith("Query 'q' can't be parsed: "), failures.get(0));
    }

    @Test
    public void unknownRoot() {
        assertEquals(List.of("Query 'q': Unknown ObjEntity 'Gallery'"), validate("from Gallery where x = 1"));
        assertEquals(List.of("Query 'q': Unknown DbEntity 'GALLERY'"), validate("from db:GALLERY"));
    }

    @Test
    public void invalidPaths() {
        assertEquals(List.of(
                "Query 'q': Can't resolve path 'nme' in Artist",
                "Query 'q': Can't resolve path 'titl' in Painting",
                "Query 'q': Can't resolve path 'db:NME' in ARTIST",
                "Query 'q': Can't resolve path 'paintings.x' in Artist",
                "Query 'q': Can't resolve prefetch path 'galleries' in Artist",
                "Query 'q': Prefetch path 'name' doesn't end with a relationship"), validate("""
                select nme from Artist
                where exists (from Painting where titl = 'a') and db:NME = 'b'
                order by paintings.x
                prefetch galleries, name"""));
    }

    private List<String> validate(String ql) {
        SelectQueryDescriptor query = new SelectQueryDescriptor();
        query.setName("q");
        query.setQl(ql);
        map.removeQueryDescriptor("q");
        map.addQueryDescriptor(query);

        return new DefaultProjectValidator().validate(query).getFailures().stream()
                .map(ValidationFailure::getDescription)
                .toList();
    }
}
