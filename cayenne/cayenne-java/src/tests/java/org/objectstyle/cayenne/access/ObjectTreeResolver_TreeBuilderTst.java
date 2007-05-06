/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.access;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectstyle.art.Artist;
import org.objectstyle.art.Gallery;
import org.objectstyle.art.Painting;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.access.ObjectTreeResolver.TreeBuilder;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.query.MockQueryMetadata;
import org.objectstyle.cayenne.query.PrefetchTreeNode;
import org.objectstyle.cayenne.query.QueryMetadata;
import org.objectstyle.cayenne.unit.CayenneTestCase;

public class ObjectTreeResolver_TreeBuilderTst extends CayenneTestCase {

    public void testBuildTreeNoPrefetches() {

        final ObjEntity entity = getObjEntity("Artist");
        List dataRows = new ArrayList();
        dataRows.add(new DataRow(4));
        dataRows.add(new DataRow(4));

        QueryMetadata metadata = new MockQueryMetadata() {

            public ObjEntity getObjEntity() {
                return entity;
            }

            public DbEntity getDbEntity() {
                return entity.getDbEntity();
            }

            public DataMap getDataMap() {
                return entity.getDataMap();
            }

            public boolean isRefreshingObjects() {
                return true;
            }

            public boolean isResolvingInherited() {
                return true;
            }
        };

        PrefetchTreeNode tree = new PrefetchTreeNode();
        ObjectTreeResolver resolver = new ObjectTreeResolver(
                createDataContext(),
                metadata);
        TreeBuilder builder = resolver.new TreeBuilder(dataRows, new HashMap());

        PrefetchProcessorNode processingTree = builder.buildTree(tree);

        assertTrue(processingTree.getChildren().isEmpty());
        assertFalse(processingTree.isPhantom());
        assertFalse(processingTree.isPartitionedByParent());
        assertTrue(processingTree.isDisjointPrefetch());
        assertSame(dataRows, processingTree.getDataRows());
        assertSame(entity, processingTree.getResolver().getEntity());
        assertNull(processingTree.getIncoming());
    }

    public void testBuildTreeWithPrefetches() {

        final ObjEntity e1 = getObjEntity("Artist");
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

            public ObjEntity getObjEntity() {
                return e1;
            }

            public DbEntity getDbEntity() {
                return e1.getDbEntity();
            }

            public DataMap getDataMap() {
                return e1.getDataMap();
            }

            public boolean isRefreshingObjects() {
                return true;
            }

            public boolean isResolvingInherited() {
                return true;
            }
        };

        ObjectTreeResolver resolver = new ObjectTreeResolver(
                createDataContext(),
                metadata);
        TreeBuilder builder = resolver.new TreeBuilder(mainRows, extraRows);

        PrefetchProcessorNode n1 = builder.buildTree(tree);

        assertSame(mainRows, n1.getDataRows());
        assertSame(e1, n1.getResolver().getEntity());

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
        assertFalse(n3.isPartitionedByParent());

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
