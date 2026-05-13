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

import org.apache.cayenne.access.flush.operation.DbRowOpSorter;
import org.apache.cayenne.access.flush.operation.GraphBasedDbRowOpSorter;
import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.ArtGroup;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.CompoundPaintingLongNames;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsExt;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class CDOReflexiveRelICustomSorterIT {

    @RegisterExtension
    static final CayenneTestsExt env = CayenneTestsExt
            .forProject(CayenneProjects.TESTMAP_PROJECT)
            .withExtraModules(GraphSorterModule.class);

    @Test
    public void addDeleteNoCommit() {
        ObjectContext context = env.context();
        ArtGroup parentGroup = context.newObject(ArtGroup.class);
        parentGroup.setName("parent");

        ArtGroup childGroup1 = context.newObject(ArtGroup.class);
        childGroup1.setName("child1");
        childGroup1.setToParentGroup(parentGroup);

        context.deleteObjects(parentGroup);
    }

    @Test
    public void add() {
        ObjectContext context = env.context();
        ArtGroup parentGroup = context.newObject(ArtGroup.class);
        parentGroup.setName("parent");

        ArtGroup childGroup1 = context.newObject(ArtGroup.class);
        childGroup1.setName("child1");
        childGroup1.setToParentGroup(parentGroup);

        context.commitChanges();
    }

    @Test
    public void addDeleteWithCommit() {
        ObjectContext context = env.context();
        ArtGroup parentGroup = context.newObject(ArtGroup.class);
        parentGroup.setName("parent");

        ArtGroup childGroup1 = context.newObject(ArtGroup.class);
        childGroup1.setName("child1");
        childGroup1.setToParentGroup(parentGroup);
        context.commitChanges();

        context.deleteObjects(parentGroup);
        context.commitChanges();
    }

    @Test
    public void replaceDeleteNoCommit() {
        ObjectContext context = env.context();
        ArtGroup parentGroup1 = context.newObject(ArtGroup.class);
        parentGroup1.setName("parent1");
        ArtGroup parentGroup2 = context.newObject(ArtGroup.class);
        parentGroup2.setName("parent2");

        ArtGroup childGroup1 = context.newObject(ArtGroup.class);
        childGroup1.setName("child1");
        childGroup1.setToParentGroup(parentGroup1);

        childGroup1.setToParentGroup(parentGroup2);

        context.deleteObjects(parentGroup1);
        context.deleteObjects(parentGroup2);
    }

    @Test
    public void replaceDeleteWithCommit() {
        ObjectContext context = env.context();
        ArtGroup parentGroup1 = context.newObject(ArtGroup.class);
        parentGroup1.setName("parent1");
        ArtGroup parentGroup2 = context.newObject(ArtGroup.class);
        parentGroup2.setName("parent2");

        ArtGroup childGroup1 = context.newObject(ArtGroup.class);
        childGroup1.setName("child1");
        childGroup1.setToParentGroup(parentGroup1);
        childGroup1.setToParentGroup(parentGroup2);
        context.commitChanges();

        context.deleteObjects(parentGroup1);
        context.deleteObjects(parentGroup2);
        context.commitChanges();
    }

    @Test
    public void commitReplaceCommit() {
        ObjectContext context = env.context();
        ArtGroup parentGroup1 = context.newObject(ArtGroup.class);
        parentGroup1.setName("parent1");
        ArtGroup parentGroup2 = context.newObject(ArtGroup.class);
        parentGroup2.setName("parent2");

        ArtGroup childGroup1 = context.newObject(ArtGroup.class);
        childGroup1.setName("child1");
        childGroup1.setToParentGroup(parentGroup1);
        context.commitChanges();
        childGroup1.setToParentGroup(parentGroup2);
        context.commitChanges();
    }

    @Test
    public void complexInsertUpdateOrdering() {
        ObjectContext context = env.context();
        ArtGroup parentGroup = context.newObject(ArtGroup.class);
        parentGroup.setName("parent");
        context.commitChanges();

        ArtGroup childGroup1 = context.newObject(ArtGroup.class);
        childGroup1.setName("child1");
        childGroup1.setToParentGroup(parentGroup);
        context.commitChanges();

        childGroup1.setToParentGroup(null);
        context.commitChanges();
    }

    @Test
    @Disabled
    public void circledDependencyTest() {
        ObjectContext context = env.context();
        ArtGroup artGroup1 = context.newObject(ArtGroup.class);
        artGroup1.setName("artGroup1");

        ArtGroup artGroup2 = context.newObject(ArtGroup.class);
        artGroup2.setName("artGroup2");

        artGroup2.setToOneTarget("artGroupRelationship", artGroup1, true);
        context.commitChanges();
    }

    @Test
    public void linkedDependencyTest() {
        ObjectContext context = env.context();
        Gallery gallery = context.newObject(Gallery.class);
        gallery.setGalleryName("gallery1");

        Artist artist1 = context.newObject(Artist.class);
        artist1.setArtistName("artist1");

        Artist artist2 = context.newObject(Artist.class);
        artist2.setArtistName("artist2");

        Painting painting = context.newObject(Painting.class);
        painting.setPaintingTitle("painting1");
        painting.setToArtist(artist1);
        painting.setToGallery(gallery);

        CompoundPaintingLongNames compoundPaintingLongNames = context.newObject(CompoundPaintingLongNames.class);
        compoundPaintingLongNames.setPaintingTitle(painting.getPaintingTitle());
        compoundPaintingLongNames.setGalleryLongName(gallery.getGalleryName());
        compoundPaintingLongNames.setArtistLongName(artist1.getArtistName());
        compoundPaintingLongNames.setToGallery(gallery);
        compoundPaintingLongNames.setToArtist(artist1);
        context.commitChanges();

        Gallery updatedGallery = ObjectSelect.query(Gallery.class).selectFirst(context);
        updatedGallery.setGalleryName("gallery2");

        Artist updatedArtist1 = ObjectSelect.query(Artist.class).selectFirst(context);
        updatedArtist1.setArtistName("artist11");

        context.deleteObject(artist2);
        context.commitChanges();
    }

    public static class GraphSorterModule implements Module {
        @Override
        public void configure(Binder binder) {
            binder.bind(DbRowOpSorter.class).to(GraphBasedDbRowOpSorter.class);
        }
    }
}
