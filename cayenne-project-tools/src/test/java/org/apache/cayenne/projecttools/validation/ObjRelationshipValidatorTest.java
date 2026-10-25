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

import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ObjRelationshipValidatorTest {

    private ObjRelationship groups;
    private ObjRelationship artists;
    private ObjRelationship paintings;
    private ObjRelationship paintingArtist;
    private ObjRelationship paintingGallery;
    private ObjRelationship galleryPaintings;

    @BeforeEach
    public void buildModel() {
        DataMap map = new DataMap("M");

        DbEntity artist = addDbEntity(map, "ARTIST", "ARTIST_ID");
        DbEntity group = addDbEntity(map, "GROUP", "GROUP_ID");
        DbEntity gallery = addDbEntity(map, "GALLERY", "GALLERY_ID");
        gallery.addAttribute(new DbAttribute("GALLERY_NAME"));
        DbEntity painting = addDbEntity(map, "PAINTING", "PAINTING_ID");
        painting.addAttribute(new DbAttribute("ARTIST_ID"));
        painting.addAttribute(new DbAttribute("GALLERY_NAME"));

        // a join table keyed by the two FKs
        DbEntity artistGroup = addDbEntity(map, "ARTIST_GROUP", "ARTIST_ID");
        DbAttribute groupId = new DbAttribute("GROUP_ID");
        groupId.setPrimaryKey(true);
        artistGroup.addAttribute(groupId);

        addDbRelationship(artist, "artistGroups", "ARTIST_GROUP", "ARTIST_ID", "ARTIST_ID", true, false);
        addDbRelationship(artistGroup, "artist", "ARTIST", "ARTIST_ID", "ARTIST_ID", false, true);
        addDbRelationship(group, "artistGroups", "ARTIST_GROUP", "GROUP_ID", "GROUP_ID", true, false);
        addDbRelationship(artistGroup, "group", "GROUP", "GROUP_ID", "GROUP_ID", false, true);

        // a plain FK to PK
        addDbRelationship(artist, "paintings", "PAINTING", "ARTIST_ID", "ARTIST_ID", true, false);
        addDbRelationship(painting, "artist", "ARTIST", "ARTIST_ID", "ARTIST_ID", false, true);

        // a FK to a non-PK column
        addDbRelationship(painting, "gallery", "GALLERY", "GALLERY_NAME", "GALLERY_NAME", false, true);
        addDbRelationship(gallery, "paintings", "PAINTING", "GALLERY_NAME", "GALLERY_NAME", true, false);

        ObjEntity artistEntity = addObjEntity(map, "Artist", "ARTIST");
        ObjEntity groupEntity = addObjEntity(map, "Group", "GROUP");
        ObjEntity galleryEntity = addObjEntity(map, "Gallery", "GALLERY");
        ObjEntity paintingEntity = addObjEntity(map, "Painting", "PAINTING");

        groups = addObjRelationship(artistEntity, "groups", "Group", "artistGroups.group");
        artists = addObjRelationship(groupEntity, "artists", "Artist", "artistGroups.artist");
        paintings = addObjRelationship(artistEntity, "paintings", "Painting", "paintings");
        paintingArtist = addObjRelationship(paintingEntity, "artist", "Artist", "artist");
        paintingGallery = addObjRelationship(paintingEntity, "gallery", "Gallery", "gallery");
        galleryPaintings = addObjRelationship(galleryEntity, "paintings", "Painting", "paintings");
    }

    @Test
    public void flattenedManyToManyIsNotReported() {
        assertFalse(hasFailure(groups, "join not to PK"));
        assertFalse(hasFailure(artists, "join not to PK"));
    }

    @Test
    public void fkToPkIsNotReported() {
        assertFalse(hasFailure(paintings, "join not to PK"));
        assertFalse(hasFailure(paintingArtist, "join not to PK"));
    }

    @Test
    public void joinNotToPkOnEitherSideIsReported() {
        assertTrue(hasFailure(paintingGallery, "join not to PK"));
        assertTrue(hasFailure(galleryPaintings, "join not to PK"));
    }

    private static boolean hasFailure(ObjRelationship relationship, String description) {
        ValidationResult result = new ValidationResult();
        new ObjRelationshipValidator(ValidationConfig::new).validate(relationship, result);
        return result.getFailures(relationship).stream()
                .anyMatch(failure -> failure.getDescription().contains(description));
    }

    private static DbEntity addDbEntity(DataMap map, String name, String pkName) {
        DbEntity entity = new DbEntity(name);
        DbAttribute pk = new DbAttribute(pkName);
        pk.setPrimaryKey(true);
        entity.addAttribute(pk);
        map.addDbEntity(entity);
        return entity;
    }

    private static void addDbRelationship(DbEntity source, String name, String target, String from, String to,
                                          boolean toMany, boolean foreignKey) {
        DbRelationship relationship = new DbRelationship(name);
        source.addRelationship(relationship);
        relationship.setTargetEntityName(target);
        relationship.setToMany(toMany);
        relationship.setForeignKey(foreignKey);
        relationship.addJoin(new DbJoin(relationship, from, to));
    }

    private static ObjEntity addObjEntity(DataMap map, String name, String dbEntityName) {
        ObjEntity entity = new ObjEntity(name);
        entity.setDbEntityName(dbEntityName);
        map.addObjEntity(entity);
        return entity;
    }

    private static ObjRelationship addObjRelationship(ObjEntity source, String name, String target, String path) {
        ObjRelationship relationship = new ObjRelationship(name);
        source.addRelationship(relationship);
        relationship.setTargetEntityName(target);
        relationship.setDbRelationshipPath(CayennePath.of(path));
        return relationship;
    }
}
