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
package org.apache.cayenne.query;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class StatementFetchSizeTest extends ServerCase {

    @Inject
    private ObjectContext context;

    public void test() {
        SelectQuery query = new SelectQuery(Artist.class);
        query.setStatementFetchSize(10);

        assertEquals(10, query
                .getMetaData(context.getEntityResolver())
                .getStatementFetchSize());
        context.performQuery(query);

        SQLTemplate template = new SQLTemplate(
                Artist.class,
                "SELECT ARTIST_ID FROM ARTIST");
        template.setStatementFetchSize(10);

        assertEquals(10, template
                .getMetaData(context.getEntityResolver())
                .getStatementFetchSize());
        context.performQuery(template);

        EJBQLQuery ejbql = new EJBQLQuery("select a from Artist a");
        ejbql.setStatementFetchSize(10);

        assertEquals(10, ejbql
                .getMetaData(context.getEntityResolver())
                .getStatementFetchSize());
        context.performQuery(ejbql);

        ObjectId id = new ObjectId("Artist", Artist.ARTIST_ID_PK_COLUMN, 1);
        RelationshipQuery relationshipQuery = new RelationshipQuery(
                id,
                Artist.PAINTING_ARRAY_PROPERTY,
                true);
        relationshipQuery.setStatementFetchSize(10);

        assertEquals(10, relationshipQuery
                .getMetaData(context.getEntityResolver())
                .getStatementFetchSize());
        context.performQuery(relationshipQuery);
    }
}
