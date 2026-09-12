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
package org.apache.cayenne.query;

import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StatementFetchSizeIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void objectSelect() {
        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class).statementFetchSize(10);

        assertEquals(10, query
                .getMetaData(env.context().getEntityResolver())
                .getStatementFetchSize());
        env.context().select(query);
    }

    @Test
    public void objectSelectWithFetchSizeSmallerThanResult() {
        // a fetch size below the row count forces a multi-batch read on drivers that honor it (a cursor on
        // PostgreSQL, which needs the transaction opened by PostgresSelectAction); every row must still come back
        for (int i = 0; i < 5; i++) {
            env.context().newObject(Artist.class).setArtistName("artist" + i);
        }
        env.context().commitChanges();

        List<Artist> artists = ObjectSelect.query(Artist.class)
                .statementFetchSize(2)
                .select(env.runtime().newContext());
        assertEquals(5, artists.size());
    }

    @Test
    public void sqlTemplate() {
        SQLTemplate template = new SQLTemplate(
                Artist.class,
                "SELECT ARTIST_ID FROM ARTIST");
        template.setStatementFetchSize(10);

        assertEquals(10, template
                .getMetaData(env.context().getEntityResolver())
                .getStatementFetchSize());
        env.context().select(template);
    }

    @Test
    public void ejbqlQuery() {
        EJBQLQuery ejbql = new EJBQLQuery("select a from Artist a");
        ejbql.setStatementFetchSize(10);

        assertEquals(10, ejbql
                .getMetaData(env.context().getEntityResolver())
                .getStatementFetchSize());
        env.context().select(ejbql);
    }
}
