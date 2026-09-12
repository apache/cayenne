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
import org.apache.cayenne.DataChannel;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.QueryResultItem;
import org.apache.cayenne.dba.frontbase.FrontBaseAdapter;
import org.apache.cayenne.dba.mysql.MySQLAdapter;
import org.apache.cayenne.event.EventManager;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class DataContextDecoratedStackIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void commitDecorated() {
        DataDomain dd = env.runtime().getDataDomain();
        DataChannel decorator = new DataChannelDecorator(dd);
        DataContext context = (DataContext) env.runtime().newContext(decorator);

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
                MySQLAdapter.class.getName(),
                "select #result('COUNT(ARTIST_ID)' 'int' 'x') from ARTIST");
        Map<?, ?> count = (Map<?, ?>) Cayenne.objectForQuery(context, query);
        assertNotNull(count);
        assertEquals(1, count.get("x"));
    }

    @Test
    public void getDataDomain() {
        DataDomain dd = env.runtime().getDataDomain();
        DataChannel decorator = new DataChannelDecorator(dd);
        DataContext context = (DataContext) env.runtime().newContext(decorator);

        assertSame(dd, decorator.getDataDomain());
        assertSame(dd, context.getChannel().getDataDomain());
    }

    @Test
    @SuppressWarnings("removal")
    public void getParentDataDomain() {
        DataDomain dd = env.runtime().getDataDomain();
        DataChannel decorator = new DataChannelDecorator(dd);
        DataContext context = (DataContext) env.runtime().newContext(decorator);

        assertSame(dd, context.getParentDataDomain());
    }

    class DataChannelDecorator implements DataChannel {

        private final DataChannel channel;

        public DataChannelDecorator(DataChannel channel) {
            this.channel = channel;
        }

        @Override
        public EntityResolver getEntityResolver() {
            return channel.getEntityResolver();
        }

        @Override
        public EventManager getEventManager() {
            return channel.getEventManager();
        }

        @Override
        public List<QueryResultItem> onQuery(ObjectContext context, Query query, boolean iteratedResult) {
            return channel.onQuery(context, query, iteratedResult);
        }

        @Override
        public GraphDiff onSync(
                ObjectContext context,
                GraphDiff changes,
                int syncType) {
            return channel.onSync(context, changes, syncType);
        }

        @Override
        public void onInvalidate(ObjectContext context, Collection<ObjectId> objectIds) {
            channel.onInvalidate(context, objectIds);
        }

        @Override
        public Persistent onIdQuery(ObjectContext context, ObjectId id) {
            return channel.onIdQuery(context, id);
        }

        @Override
        public List<? extends Persistent> onRelationshipQuery(ObjectContext context, ObjectId sourceId, String relationshipName) {
            return channel.onRelationshipQuery(context, sourceId, relationshipName);
        }

        @Override
        public DataChannel getParent() {
            return channel;
        }
    }

}
