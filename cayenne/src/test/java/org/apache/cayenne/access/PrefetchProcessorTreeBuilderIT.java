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
import org.apache.cayenne.DataRowLayout;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.compiled.CompiledMapping;
import org.apache.cayenne.map.compiled.CompiledEntity;

import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Gallery;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PrefetchProcessorTreeBuilderIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private DataContext context;
    private CompiledMapping resolver;

    @BeforeEach
    public void setUp() {
        context = env.context();
        resolver = env.mapping();
    }


    @Test
    public void buildTreeNoPrefetches() {

        final CompiledEntity entity = resolver.entity("Artist");
        List<DataRow> dataRows = new ArrayList<>();
        dataRows.add(DataRow.of("Artist", DataRowLayout.of(), new Object[0], SnapshotVersion.next()));
        dataRows.add(DataRow.of("Artist", DataRowLayout.of(), new Object[0], SnapshotVersion.next()));


        PrefetchTreeNode tree = new PrefetchTreeNode();
        HierarchicalObjectResolver resolver = new HierarchicalObjectResolver(context, null, entity, true);
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
        assertSame(entity, processingTree.getResolver().getEntity());
        assertNull(processingTree.getIncoming());
    }

    @Test
    public void buildTreeWithPrefetches() {

        final CompiledEntity entity = resolver.entity("Artist");
        CompiledEntity e2 = resolver.entity("Painting");
        CompiledEntity e3 = resolver.entity("Gallery");
        CompiledEntity e4 = resolver.entity("Exhibit");
        CompiledEntity e5 = resolver.entity("ArtistExhibit");

        List<DataRow> mainRows = new ArrayList<>();
        Map<CayennePath, List<?>> extraRows = new HashMap<>();

        PrefetchTreeNode tree = new PrefetchTreeNode();
        tree.addPath(Artist.PAINTINGS.getName()).setPhantom(false);
        tree.addPath(Artist.PAINTINGS.dot(Painting.GALLERY).dot(Gallery.EXHIBITS).getName()).setPhantom(false);
        tree.addPath(Artist.ARTIST_EXHIBITS.getName()).setPhantom(false);


        HierarchicalObjectResolver resolver = new HierarchicalObjectResolver(context, null, entity, true);
        PrefetchProcessorTreeBuilder builder = new PrefetchProcessorTreeBuilder(
                resolver,
                mainRows,
                extraRows);

        PrefetchProcessorNode n1 = builder.buildTree(tree);

        assertSame(mainRows, n1.getDataRows());
        assertSame(entity, n1.getResolver().getEntity());

        PrefetchProcessorNode n2 = (PrefetchProcessorNode) n1.getNode("paintings");
        assertNotNull(n2);
        assertSame(e2, n2.getResolver().getEntity());
        assertFalse(n2.isPhantom());
        assertTrue(n2.isPartitionedByParent());

        PrefetchProcessorNode n3 = (PrefetchProcessorNode) n1
                .getNode("paintings.gallery");
        assertNotNull(n3);
        assertSame(e3, n3.getResolver().getEntity());
        assertTrue(n3.isPhantom());
        assertTrue(n3.isPartitionedByParent());

        PrefetchProcessorNode n4 = (PrefetchProcessorNode) n1
                .getNode("paintings.gallery.exhibits");
        assertNotNull(n4);
        assertSame(e4, n4.getResolver().getEntity());
        assertFalse(n4.isPhantom());
        assertTrue(n4.isPartitionedByParent());

        PrefetchProcessorNode n5 = (PrefetchProcessorNode) n1
                .getNode("artistExhibits");
        assertNotNull(n5);
        assertSame(e5, n5.getResolver().getEntity());
        assertFalse(n5.isPhantom());
        assertTrue(n5.isPartitionedByParent());
    }

}
