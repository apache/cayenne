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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.configuration.CayenneRuntime;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.apache.cayenne.util.Util;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextSerializationTest extends ServerCase {

    @Inject
    protected DataContext context;

    @Inject
    protected ServerRuntime runtime;

    @Inject
    protected DBHelper dbHelper;

    @Inject
    protected JdbcEventLogger logger;

    protected TableHelper tArtist;

    @Override
    protected void setUpAfterInjection() throws Exception {
        CayenneRuntime.bindThreadInjector(runtime.getInjector());

        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");

        tArtist = new TableHelper(dbHelper, "ARTIST");
        tArtist.setColumns("ARTIST_ID", "ARTIST_NAME");
    }

    protected void createSingleArtistDataSet() throws Exception {
        tArtist.insert(33001, "aaa");
    }

    @Override
    protected void tearDownBeforeInjection() throws Exception {
        CayenneRuntime.bindThreadInjector(null);
    }

    public void testSerializeResolver() throws Exception {

        DataContext deserializedContext = Util.cloneViaSerialization(context);

        assertNotNull(deserializedContext.getEntityResolver());
        assertSame(context.getEntityResolver(), deserializedContext.getEntityResolver());
    }

    public void testSerializeChannel() throws Exception {

        DataContext deserializedContext = Util.cloneViaSerialization(context);

        assertNotNull(deserializedContext.getChannel());
        assertSame(context.getChannel(), deserializedContext.getChannel());
    }

    public void testSerializeNestedChannel() throws Exception {

        ObjectContext child = runtime.newContext(context);

        ObjectContext deserializedContext = Util.cloneViaSerialization(child);

        assertNotNull(deserializedContext.getChannel());
        assertNotNull(deserializedContext.getEntityResolver());
    }

    public void testSerializeWithSharedCache() throws Exception {

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

    public void testSerializeWithLocalCache() throws Exception {

        createSingleArtistDataSet();

        // manually assemble a DataContext with local cache....
        DataDomain domain = context.getParentDataDomain();
        DataRowStore snapshotCache = new DataRowStore(
                domain.getName(),
                domain.getProperties(),
                domain.getEventManager());

        Map<Object, Persistent> map = new HashMap<Object, Persistent>();

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

    public void testSerializeNew() throws Exception {

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

    public void testSerializeCommitted() throws Exception {

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

    public void testSerializeModified() throws Exception {

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
}
