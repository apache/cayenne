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

import java.util.Map;

import org.apache.art.Artist;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.intercept.DataChannelDecorator;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.unit.CayenneCase;

public class DataContextDecoratedStackTest extends CayenneCase {

    protected void setUp() throws Exception {
        deleteTestData();
    }

    public void testCommitDecorated() {
        DataDomain dd = getDomain();
        DataChannel decorator = new DataChannelDecorator(dd);
        DataContext context = new DataContext(decorator, new ObjectStore(dd
                .getSharedSnapshotCache()));

        Artist a = (Artist) context.newObject(Artist.class);
        a.setArtistName("XXX");
        context.commitChanges();

        SQLTemplate query = new SQLTemplate(
                Artist.class,
                "select #result('count(1)' 'int' 'c') from ARTIST");
        query.setFetchingDataRows(true);
        Map count = (Map) DataObjectUtils.objectForQuery(context, query);
        assertNotNull(count);
        assertEquals(new Integer(1), count.get("c"));
    }

    public void testGetParentDataDomain() {
        DataDomain dd = getDomain();
        DataChannel decorator = new DataChannelDecorator(dd);
        DataContext context = new DataContext(decorator, new ObjectStore(dd
                .getSharedSnapshotCache()));

        assertSame(dd, context.getParentDataDomain());
    }
}
