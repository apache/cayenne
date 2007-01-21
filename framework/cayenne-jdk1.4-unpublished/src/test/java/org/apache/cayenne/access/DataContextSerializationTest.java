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

import java.util.List;

import org.apache.art.Artist;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.util.Util;

/**
 * @author Andrus Adamchik
 */
public class DataContextSerializationTest extends CayenneCase {

    protected void setUp() throws Exception {
        fixSharedConfiguration();
    }

    protected void fixSharedConfiguration() {
        // for context to deserialize properly,
        // Configuration singleton must have the right default domain
        Configuration config = Configuration.getSharedConfiguration();
        if (getDomain() != config.getDomain()) {
            if (config.getDomain() != null) {
                config.removeDomain(config.getDomain().getName());
            }
            config.addDomain(getDomain());
        }
    }

    public void testSerializeResolver() throws Exception {

        DataContext context = createDataContextWithSharedCache();

        DataContext deserializedContext = (DataContext) Util
                .cloneViaSerialization(context);

        assertNotNull(deserializedContext.getEntityResolver());
        assertSame(context.getEntityResolver(), deserializedContext.getEntityResolver());
    }

    public void testSerializeChannel() throws Exception {

        DataContext context = createDataContextWithSharedCache();

        DataContext deserializedContext = (DataContext) Util
                .cloneViaSerialization(context);

        assertNotNull(deserializedContext.getChannel());
        assertSame(context.getChannel(), deserializedContext.getChannel());
    }

    public void testSerializeWithSharedCache() throws Exception {

        DataContext context = createDataContextWithSharedCache();

        DataContext deserializedContext = (DataContext) Util
                .cloneViaSerialization(context);

        assertNotSame(context, deserializedContext);
        assertNotSame(context.getObjectStore(), deserializedContext.getObjectStore());
        assertSame(context.getParentDataDomain(), deserializedContext
                .getParentDataDomain());
        assertSame(context.getObjectStore().getDataRowCache(), deserializedContext
                .getObjectStore()
                .getDataRowCache());
        assertSame(
                deserializedContext.getParentDataDomain().getSharedSnapshotCache(),
                deserializedContext.getObjectStore().getDataRowCache());

        assertNotNull(deserializedContext.getEntityResolver());
        assertSame(context.getEntityResolver(), deserializedContext.getEntityResolver());
    }

    public void testSerializeWithLocalCache() throws Exception {

        DataContext context = createDataContextWithLocalCache();

        assertNotSame(context.getParentDataDomain().getSharedSnapshotCache(), context
                .getObjectStore()
                .getDataRowCache());

        DataContext deserializedContext = (DataContext) Util
                .cloneViaSerialization(context);

        assertNotSame(context, deserializedContext);
        assertNotSame(context.getObjectStore(), deserializedContext.getObjectStore());

        assertSame(context.getParentDataDomain(), deserializedContext
                .getParentDataDomain());
        assertNotSame(context.getObjectStore().getDataRowCache(), deserializedContext
                .getObjectStore()
                .getDataRowCache());
        assertNotSame(
                deserializedContext.getParentDataDomain().getSharedSnapshotCache(),
                deserializedContext.getObjectStore().getDataRowCache());
    }

    public void testSerializeNew() throws Exception {

        DataContext context = createDataContextWithSharedCache();

        Artist artist = (Artist) context.newObject("Artist");
        artist.setArtistName("artist1");
        assertNotNull(artist.getObjectId());

        DataContext deserializedContext = (DataContext) Util
                .cloneViaSerialization(context);
        assertSame(context.getParentDataDomain(), deserializedContext
                .getParentDataDomain());

        // there should be only one object registered
        Artist deserializedArtist = (Artist) deserializedContext
                .getObjectStore()
                .getObjectIterator()
                .next();

        assertNotNull(deserializedArtist);
        assertEquals(PersistenceState.NEW, deserializedArtist.getPersistenceState());
        assertTrue(deserializedArtist.getObjectId().isTemporary());
        assertEquals("artist1", deserializedArtist.getArtistName());
        assertSame(deserializedContext, deserializedArtist.getObjectContext());
    }

    public void testSerializeCommitted() throws Exception {

        DataContext context = createDataContextWithSharedCache();

        Artist artist = (Artist) context.newObject("Artist");
        artist.setArtistName("artist1");
        assertNotNull(artist.getObjectId());
        context.commitChanges();

        DataContext deserializedContext = (DataContext) Util
                .cloneViaSerialization(context);

        assertSame(context.getParentDataDomain(), deserializedContext
                .getParentDataDomain());

        // there should be only one object registered
        Artist deserializedArtist = (Artist) deserializedContext
                .getObjectStore()
                .getObjectIterator()
                .next();

        assertNotNull(deserializedArtist);

        // deserialized as hollow...
        assertEquals(PersistenceState.HOLLOW, deserializedArtist.getPersistenceState());
        assertFalse(deserializedArtist.getObjectId().isTemporary());
        assertEquals("artist1", deserializedArtist.getArtistName());
        assertSame(deserializedContext, deserializedArtist.getObjectContext());

        // test that to-many relationships are initialized
        List paintings = deserializedArtist.getPaintingArray();
        assertNotNull(paintings);
        assertEquals(0, paintings.size());
    }

    public void testSerializeModified() throws Exception {

        DataContext context = createDataContextWithSharedCache();

        Artist artist = (Artist) context.newObject("Artist");
        artist.setArtistName("artist1");
        assertNotNull(artist.getObjectId());
        context.commitChanges();
        artist.setArtistName("artist2");

        DataContext deserializedContext = (DataContext) Util
                .cloneViaSerialization(context);

        assertSame(context.getParentDataDomain(), deserializedContext
                .getParentDataDomain());

        // there should be only one object registered
        Artist deserializedArtist = (Artist) deserializedContext
                .getObjectStore()
                .getObjectIterator()
                .next();

        assertNotNull(deserializedArtist);

        // deserialized as hollow...
        assertEquals(PersistenceState.MODIFIED, deserializedArtist.getPersistenceState());
        assertFalse(deserializedArtist.getObjectId().isTemporary());
        assertEquals("artist2", deserializedArtist.getArtistName());
        assertSame(deserializedContext, deserializedArtist.getObjectContext());
    }
}
