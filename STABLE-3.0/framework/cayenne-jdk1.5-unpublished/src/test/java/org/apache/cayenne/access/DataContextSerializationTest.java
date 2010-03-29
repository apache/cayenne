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
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.util.Util;

/**
 */
public class DataContextSerializationTest extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        fixSharedConfiguration();
        deleteTestData();
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
    
    public void testSerializeNestedChannel() throws Exception {
        DataContext context = createDataContextWithSharedCache();
        ObjectContext child = context.createChildContext();

        ObjectContext deserializedContext = (ObjectContext) Util.cloneViaSerialization(child);

        assertNotNull(deserializedContext.getChannel());
        assertNotNull(deserializedContext.getEntityResolver());
    }

    public void testSerializeWithSharedCache() throws Exception {
        
        createTestData("prepare");

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
        
        Artist a = DataObjectUtils.objectForPK(deserializedContext, Artist.class, 33001);
        assertNotNull(a);
        a.setArtistName(a.getArtistName() + "___");
        deserializedContext.commitChanges();
    }

    public void testSerializeWithLocalCache() throws Exception {
        
        createTestData("prepare");

        DataContext context = createDataContextWithDedicatedCache();

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
        
        Artist a = DataObjectUtils.objectForPK(deserializedContext, Artist.class, 33001);
        assertNotNull(a);
        a.setArtistName(a.getArtistName() + "___");
        
        // this blows per CAY-796
        deserializedContext.commitChanges();
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
