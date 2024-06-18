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

import org.apache.cayenne.DataRow;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.MockQueryMetadata;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class PrefetchProcessorTreeBuilderIT extends RuntimeCase {

    @Inject
    private DataContext context;

    @Inject
    private EntityResolver resolver;

    @Test
    public void testBuildTreeNoPrefetches() {

        final ClassDescriptor descriptor = resolver.getClassDescriptor("Artist");
        List<DataRow> dataRows = new ArrayList<>();
        dataRows.add(new DataRow(4));
        dataRows.add(new DataRow(4));

        QueryMetadata metadata = new MockQueryMetadata() {

            @Override
            public ClassDescriptor getClassDescriptor() {
                return descriptor;
            }

            @Override
            public ObjEntity getObjEntity() {
                return descriptor.getEntity();
            }

            @Override
            public DbEntity getDbEntity() {
                return getObjEntity().getDbEntity();
            }

            @Override
            public DataMap getDataMap() {
                return getObjEntity().getDataMap();
            }

            @Override
            public boolean isRefreshingObjects() {
                return true;
            }

            @Override
            public boolean isResolvingInherited() {
                return true;
            }
        };

        PrefetchTreeNode tree = new PrefetchTreeNode();
        HierarchicalObjectResolver resolver = new HierarchicalObjectResolver(
                context,
                metadata);
        PrefetchProcessorTreeBuilder builder = new PrefetchProcessorTreeBuilder(
                resolver,
                dataRows,
                new HashMap<>());

        PrefetchProcessorNode processingTree = builder.buildTree(tree);

        assertTrue(processingTree.getChildren().isEmpty());
        assertFalse(processingTree.isPhantom());
        assertFalse(processingTree.isPartitionedByParent());
        assertTrue(processingTree.isDisjointPrefetch());
        assertSame(dataRows, processingTree.getDataRows());
        assertSame(descriptor.getEntity(), processingTree.getResolver().getEntity());
        assertNull(processingTree.getIncoming());
    }

    @Test
    public void testBuildTreeWithPrefetches() {

        final ClassDescriptor descriptor = resolver.getClassDescriptor("Artist");
        ObjEntity e2 = resolver.getObjEntity("Painting");
        ObjEntity e3 = resolver.getObjEntity("Gallery");
        ObjEntity e4 = resolver.getObjEntity("Exhibit");
        ObjEntity e5 = resolver.getObjEntity("ArtistExhibit");

        List<DataRow> mainRows = new ArrayList<>();
        Map<CayennePath, List<?>> extraRows = new HashMap<>();

        PrefetchTreeNode tree = new PrefetchTreeNode();
        tree.addPath(Artist.PAINTING_ARRAY.getName()).setPhantom(false);
        tree.addPath(Artist.PAINTING_ARRAY.dot(Painting.TO_GALLERY).dot(Gallery.EXHIBIT_ARRAY).getName()).setPhantom(false);
        tree.addPath(Artist.ARTIST_EXHIBIT_ARRAY.getName()).setPhantom(false);

        QueryMetadata metadata = new MockQueryMetadata() {

            @Override
            public ClassDescriptor getClassDescriptor() {
                return descriptor;
            }

            @Override
            public ObjEntity getObjEntity() {
                return descriptor.getEntity();
            }

            @Override
            public DbEntity getDbEntity() {
                return getObjEntity().getDbEntity();
            }

            @Override
            public DataMap getDataMap() {
                return getObjEntity().getDataMap();
            }

            @Override
            public boolean isRefreshingObjects() {
                return true;
            }

            @Override
            public boolean isResolvingInherited() {
                return true;
            }
        };

        HierarchicalObjectResolver resolver = new HierarchicalObjectResolver(
                context,
                metadata);
        PrefetchProcessorTreeBuilder builder = new PrefetchProcessorTreeBuilder(
                resolver,
                mainRows,
                extraRows);

        PrefetchProcessorNode n1 = builder.buildTree(tree);

        assertSame(mainRows, n1.getDataRows());
        assertSame(descriptor.getEntity(), n1.getResolver().getEntity());

        PrefetchProcessorNode n2 = (PrefetchProcessorNode) n1.getNode("paintingArray");
        assertNotNull(n2);
        assertSame(e2, n2.getResolver().getEntity());
        assertFalse(n2.isPhantom());
        assertTrue(n2.isPartitionedByParent());

        PrefetchProcessorNode n3 = (PrefetchProcessorNode) n1
                .getNode("paintingArray.toGallery");
        assertNotNull(n3);
        assertSame(e3, n3.getResolver().getEntity());
        assertTrue(n3.isPhantom());
        assertTrue(n3.isPartitionedByParent());

        PrefetchProcessorNode n4 = (PrefetchProcessorNode) n1
                .getNode("paintingArray.toGallery.exhibitArray");
        assertNotNull(n4);
        assertSame(e4, n4.getResolver().getEntity());
        assertFalse(n4.isPhantom());
        assertTrue(n4.isPartitionedByParent());

        PrefetchProcessorNode n5 = (PrefetchProcessorNode) n1
                .getNode("artistExhibitArray");
        assertNotNull(n5);
        assertSame(e5, n5.getResolver().getEntity());
        assertFalse(n5.isPhantom());
        assertTrue(n5.isPartitionedByParent());
    }

}
