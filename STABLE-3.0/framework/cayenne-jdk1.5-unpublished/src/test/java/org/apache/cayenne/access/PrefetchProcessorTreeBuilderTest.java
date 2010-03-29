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

package org.apache.cayenne.access;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.art.Artist;
import org.apache.art.Gallery;
import org.apache.art.Painting;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.MockQueryMetadata;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.unit.CayenneCase;

public class PrefetchProcessorTreeBuilderTest extends CayenneCase {

    public void testBuildTreeNoPrefetches() {

        final ClassDescriptor descriptor = getDomain()
                .getEntityResolver()
                .getClassDescriptor("Artist");
        List dataRows = new ArrayList();
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
                createDataContext(),
                metadata);
        PrefetchProcessorTreeBuilder builder = new PrefetchProcessorTreeBuilder(
                resolver,
                dataRows,
                new HashMap());

        PrefetchProcessorNode processingTree = builder.buildTree(tree);

        assertTrue(processingTree.getChildren().isEmpty());
        assertFalse(processingTree.isPhantom());
        assertFalse(processingTree.isPartitionedByParent());
        assertTrue(processingTree.isDisjointPrefetch());
        assertSame(dataRows, processingTree.getDataRows());
        assertSame(descriptor.getEntity(), processingTree.getResolver().getEntity());
        assertNull(processingTree.getIncoming());
    }

    public void testBuildTreeWithPrefetches() {

        final ClassDescriptor descriptor = getDomain()
                .getEntityResolver()
                .getClassDescriptor("Artist");
        ObjEntity e2 = getObjEntity("Painting");
        ObjEntity e3 = getObjEntity("Gallery");
        ObjEntity e4 = getObjEntity("Exhibit");
        ObjEntity e5 = getObjEntity("ArtistExhibit");

        List mainRows = new ArrayList();
        Map extraRows = new HashMap();

        PrefetchTreeNode tree = new PrefetchTreeNode();
        tree.addPath(Artist.PAINTING_ARRAY_PROPERTY).setPhantom(false);
        tree.addPath(
                Artist.PAINTING_ARRAY_PROPERTY
                        + "."
                        + Painting.TO_GALLERY_PROPERTY
                        + "."
                        + Gallery.EXHIBIT_ARRAY_PROPERTY).setPhantom(false);
        tree.addPath(Artist.ARTIST_EXHIBIT_ARRAY_PROPERTY).setPhantom(false);

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
                createDataContext(),
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
