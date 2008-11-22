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

package org.apache.cayenne.unit;

import java.util.Arrays;
import java.util.Collections;

import junit.framework.AssertionFailedError;

import org.apache.art.Artist;
import org.apache.art.Painting;
import org.apache.art.PaintingInfo;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.query.SelectQuery;

/**
 * A test case checking that TestDataDomain query blocking works.
 * 
 */
public class TestDataDomainTest extends CayenneCase {

    public void testBlockUnblock() {

        assertFalse(getDomain().isBlockingQueries());

        blockQueries();
        try {
            assertTrue(getDomain().isBlockingQueries());
        }
        finally {
            unblockQueries();
        }

        assertFalse(getDomain().isBlockingQueries());
    }

    public void testQuery() {
        DataContext context = createDataContext();
        assertSame(getDomain(), context.getParentDataDomain());

        blockQueries();
        try {
            context.performQuery(new SelectQuery(Artist.class));
        }
        catch (AssertionFailedError e) {
            // expected...
            return;
        }
        finally {
            unblockQueries();
        }

        fail("Must have failed on fault resolution");
    }

    public void testFaultFired() {
        DataContext context = createDataContext();
        assertSame(getDomain(), context.getParentDataDomain());

        Artist a = context.newObject(Artist.class);
        a.setArtistName("aa");
        context.commitChanges();

        context.invalidateObjects(Collections.singleton(a));

        blockQueries();
        try {
            a.getArtistName();
        }
        catch (AssertionFailedError e) {
            // expected...
            return;
        }
        finally {
            unblockQueries();
        }

        fail("Must have failed on fault resolution");
    }

    public void testRelatedFaultFired() {
        DataContext context = createDataContext();
        assertSame(getDomain(), context.getParentDataDomain());

        Painting p = context.newObject(Painting.class);
        p.setPaintingTitle("aaaa");
        PaintingInfo pi = context.newObject(PaintingInfo.class);
        pi.setPainting(p);
        context.commitChanges();

        context.invalidateObjects(Arrays.asList(p, pi));

        p.getPaintingTitle();

        blockQueries();
        try {
            p.getToPaintingInfo().getTextReview();
        }
        catch (AssertionFailedError e) {
            // expected...
            return;
        }
        finally {
            unblockQueries();
        }

        fail("Must have failed on fault resolution");
    }
}
