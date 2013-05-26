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

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.dba.frontbase.FrontBaseAdapter;
import org.apache.cayenne.dba.openbase.OpenBaseAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class DataContextDecoratedStackTest extends ServerCase {

    @Inject
    private ServerRuntime runtime;

    @Inject
    private DBHelper dbHelper;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");
    }

    public void testCommitDecorated() {
        DataDomain dd = runtime.getDataDomain();
        DataChannel decorator = new DataChannelDecorator(dd);
        DataContext context = (DataContext) runtime.newContext(decorator);

        Artist a = context.newObject(Artist.class);
        a.setArtistName("XXX");
        context.commitChanges();

        SQLTemplate query = new SQLTemplate(
                Artist.class,
                "select #result('count(1)' 'int' 'x') from ARTIST");
        query.setFetchingDataRows(true);
        query.setTemplate(
                FrontBaseAdapter.class.getName(),
                "select #result('COUNT(ARTIST_ID)' 'int' 'x') from ARTIST");
        query.setTemplate(
                OpenBaseAdapter.class.getName(),
                "select #result('COUNT(ARTIST_ID)' 'int' 'x') from ARTIST");
        Map<?, ?> count = (Map<?, ?>) Cayenne.objectForQuery(context, query);
        assertNotNull(count);
        assertEquals(new Integer(1), count.get("x"));
    }

    public void testGetParentDataDomain() {
        DataDomain dd = runtime.getDataDomain();
        DataChannel decorator = new DataChannelDecorator(dd);
        DataContext context = (DataContext) runtime.newContext(decorator);

        assertSame(dd, context.getParentDataDomain());
    }

    class DataChannelDecorator implements DataChannel {

        protected DataChannel channel;

        protected DataChannelDecorator() {

        }

        public DataChannelDecorator(DataChannel channel) {
            setChannel(channel);
        }

        public EntityResolver getEntityResolver() {
            return channel.getEntityResolver();
        }

        public EventManager getEventManager() {
            return channel.getEventManager();
        }

        public QueryResponse onQuery(ObjectContext originatingContext, Query query) {
            return channel.onQuery(originatingContext, query);
        }

        public GraphDiff onSync(
                ObjectContext originatingContext,
                GraphDiff changes,
                int syncType) {
            return channel.onSync(originatingContext, changes, syncType);
        }

        public DataChannel getChannel() {
            return channel;
        }

        public void setChannel(DataChannel channel) {
            this.channel = channel;
        }
    }

}
