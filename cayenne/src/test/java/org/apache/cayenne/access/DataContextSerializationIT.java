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

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.configuration.DefaultRuntimeProperties;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsEnv;
import org.apache.cayenne.util.Util;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DataContextSerializationIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    protected DataContext context;
    protected CayenneRuntime runtime;

    protected TableHelper tArtist;

    
    @BeforeEach
    public void setUp() throws Exception {
        context = env.context();
        runtime = env.runtime();
        CayenneRuntime.bindThreadInjector(runtime.getInjector());

        tArtist = env.table("ARTIST", "ARTIST_ID", "ARTIST_NAME");
    }

    protected void createSingleArtistDataSet() throws Exception {
        tArtist.insert(33001, "aaa");
    }

    
    @AfterEach
    public void tearDown() throws Exception {
        CayenneRuntime.bindThreadInjector(null);
    }

    @Test
    public void serializeResolver() throws Exception {

        DataContext deserializedContext = Util.cloneViaSerialization(context);

        assertNotNull(deserializedContext.getEntityResolver());
        assertSame(context.getEntityResolver(), deserializedContext.getEntityResolver());
    }

    @Test
    public void serializeChannel() throws Exception {

        DataContext deserializedContext = Util.cloneViaSerialization(context);

        assertNotNull(deserializedContext.getChannel());
        assertSame(context.getChannel(), deserializedContext.getChannel());
    }

    @Test
    public void serializeNestedChannel() throws Exception {

        ObjectContext child = runtime.newContext(context);

        ObjectContext deserializedContext = Util.cloneViaSerialization(child);

        assertNotNull(deserializedContext.getChannel());
        assertNotNull(deserializedContext.getEntityResolver());
    }

    @Test
    public void serializeWithSharedCache() throws Exception {

        createSingleArtistDataSet();

        DataContext deserializedContext = Util.cloneViaSerialization(context);

        assertNotSame(context, deserializedContext);
        assertNotSame(context.getObjectStore(), deserializedContext.getObjectStore());
        assertSame(
                context.getParentDataDomain(),
                deserializedContext.getParentDataDomain());
        assertSame(context.getObjectStore().getDataRowCache(), deserializedContext
                .getObjectStore()
                .getDataRowCache());
        assertSame(
                deserializedContext.getParentDataDomain().getSharedSnapshotCache(),
                deserializedContext.getObjectStore().getDataRowCache());

        assertNotNull(deserializedContext.getEntityResolver());
        assertSame(context.getEntityResolver(), deserializedContext.getEntityResolver());

        Artist a = Cayenne.objectForPK(deserializedContext, Artist.class, 33001);
        assertNotNull(a);
        a.setArtistName(a.getArtistName() + "___");
        deserializedContext.commitChanges();
    }

    @Test
    public void serializeWithLocalCache() throws Exception {

        createSingleArtistDataSet();

        // manually assemble a DataContext with local cache....
        DataDomain domain = context.getParentDataDomain();
        DataRowStore snapshotCache = new DataRowStore(
                domain.getName(),
                new DefaultRuntimeProperties(domain.getProperties()),
                domain.getEventManager());

        Map<Object, Persistent> map = new HashMap<>();

        DataContext localCacheContext = new DataContext(domain, new ObjectStore(
                snapshotCache,
                map));
        localCacheContext.setValidatingObjectsOnCommit(domain
                .isValidatingObjectsOnCommit());
        localCacheContext.setUsingSharedSnapshotCache(false);

        assertNotSame(domain.getSharedSnapshotCache(), localCacheContext
                .getObjectStore()
                .getDataRowCache());

        DataContext deserializedContext = Util.cloneViaSerialization(localCacheContext);

        assertNotSame(localCacheContext, deserializedContext);
        assertNotSame(
                localCacheContext.getObjectStore(),
                deserializedContext.getObjectStore());

        assertSame(
                localCacheContext.getParentDataDomain(),
                deserializedContext.getParentDataDomain());
        assertNotSame(
                localCacheContext.getObjectStore().getDataRowCache(),
                deserializedContext.getObjectStore().getDataRowCache());
        assertNotSame(
                deserializedContext.getParentDataDomain().getSharedSnapshotCache(),
                deserializedContext.getObjectStore().getDataRowCache());

        Artist a = Cayenne.objectForPK(deserializedContext, Artist.class, 33001);
        assertNotNull(a);
        a.setArtistName(a.getArtistName() + "___");

        // this blows per CAY-796
        deserializedContext.commitChanges();
    }

    @Test
    public void serializeNew() throws Exception {

        Artist artist = (Artist) context.newObject("Artist");
        artist.setArtistName("artist1");
        assertNotNull(artist.getObjectId());

        DataContext deserializedContext = Util.cloneViaSerialization(context);
        assertSame(
                context.getParentDataDomain(),
                deserializedContext.getParentDataDomain());

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

    @Test
    public void serializeCommitted() throws Exception {

        Artist artist = (Artist) context.newObject("Artist");
        artist.setArtistName("artist1");
        assertNotNull(artist.getObjectId());
        context.commitChanges();

        DataContext deserializedContext = Util.cloneViaSerialization(context);

        assertSame(
                context.getParentDataDomain(),
                deserializedContext.getParentDataDomain());

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
        List<?> paintings = deserializedArtist.getPaintingArray();
        assertNotNull(paintings);
        assertEquals(0, paintings.size());
    }

    @Test
    public void serializeModified() throws Exception {

        Artist artist = (Artist) context.newObject("Artist");
        artist.setArtistName("artist1");
        assertNotNull(artist.getObjectId());
        context.commitChanges();
        artist.setArtistName("artist2");

        DataContext deserializedContext = Util.cloneViaSerialization(context);

        assertSame(
                context.getParentDataDomain(),
                deserializedContext.getParentDataDomain());

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

    @Test
    public void serializeObjectCreator() throws Exception {
        DataContext deserializedContext = Util.cloneViaSerialization(context);
        assertNotNull(deserializedContext.objectCreator);
        assertSame(deserializedContext, deserializedContext.objectCreator.context);
    }
}
