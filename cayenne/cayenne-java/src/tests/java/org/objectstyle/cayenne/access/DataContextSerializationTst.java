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
import java.util.List;

import org.apache.log4j.Logger;
import org.objectstyle.art.Artist;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.unit.CayenneTestCase;
import org.objectstyle.cayenne.util.Util;

/**
 * @author Andrei Adamchik
 */
public class DataContextSerializationTst extends CayenneTestCase {

    private static Logger logObj = Logger.getLogger(DataContextSerializationTst.class);

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

        Artist artist = (Artist) context.createAndRegisterNewObject("Artist");
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
        assertSame(deserializedContext, deserializedArtist.getDataContext());
    }

    public void testSerializeCommitted() throws Exception {

        DataContext context = createDataContextWithSharedCache();

        Artist artist = (Artist) context.createAndRegisterNewObject("Artist");
        artist.setArtistName("artist1");
        assertNotNull(artist.getObjectId());
        context.commitChanges();

        DataContext deserializedContext = (DataContext) Util
                .cloneViaSerialization(context);

        logObj.warn("registered domains: "
                + new ArrayList(Configuration.getSharedConfiguration().getDomains()));
        logObj.warn(" domains in question: "
                + context.getParentDataDomain()
                + "--"
                + deserializedContext.getParentDataDomain());
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
        assertSame(deserializedContext, deserializedArtist.getDataContext());

        // test that to-many relationships are initialized
        List paintings = deserializedArtist.getPaintingArray();
        assertNotNull(paintings);
        assertEquals(0, paintings.size());
    }

    public void testSerializeModified() throws Exception {

        DataContext context = createDataContextWithSharedCache();

        Artist artist = (Artist) context.createAndRegisterNewObject("Artist");
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
        assertSame(deserializedContext, deserializedArtist.getDataContext());
    }
}
