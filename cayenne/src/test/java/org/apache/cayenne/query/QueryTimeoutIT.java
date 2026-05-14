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
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class QueryTimeoutIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void objectSelect() {
        ObjectSelect<Artist> objectSelect = ObjectSelect.query(Artist.class)
                .queryTimeout(10);
        assertEquals(10, objectSelect
                .getMetaData(env.context().getEntityResolver())
                .getQueryTimeout());
        objectSelect.select(env.context());
    }

    @Test
    public void sqlTemplate() {
        SQLTemplate sqlTemplate = new SQLTemplate();
        sqlTemplate.setDefaultTemplate("SELECT * FROM ARTIST");
        sqlTemplate.setQueryTimeout(10);
        assertEquals(10, sqlTemplate
                .getMetaData(env.context().getEntityResolver())
                .getQueryTimeout());
        env.context().performQuery(sqlTemplate);
    }

    @Test
    public void columnSelect() {
        ColumnSelect<String> columnSelect = ObjectSelect
                .columnQuery(Artist.class, Artist.ARTIST_NAME)
                .queryTimeout(10);
        assertEquals(10, columnSelect
                .getMetaData(env.context().getEntityResolver())
                .getQueryTimeout());
        env.context().performQuery(columnSelect);
    }

    @Test
    public void ejbql() {
        EJBQLQuery ejbqlQuery = new EJBQLQuery("select a from Artist a");
        ejbqlQuery.setQueryTimeout(10);
        assertEquals(10, ejbqlQuery
                .getMetaData(env.context().getEntityResolver())
                .getQueryTimeout());
        env.context().performQuery(ejbqlQuery);
    }

    @Test
    public void sqlSelect() {
        SQLSelect<Artist> sqlSelect = SQLSelect
                .query(Artist.class, "SELECT * FROM ARTIST")
                .queryTimeout(10);
        assertEquals(10, sqlSelect
                .getMetaData(env.context().getEntityResolver())
                .getQueryTimeout());
        env.context().performQuery(sqlSelect);
    }

    @Test
    public void sqlExec() {
        SQLExec sqlExec = SQLExec
                .query("SELECT * FROM ARTIST")
                .queryTimeout(10);
        assertEquals(10, sqlExec
                .getMetaData(env.context().getEntityResolver())
                .getQueryTimeout());
        env.context().performQuery(sqlExec);
    }

    @Test
    public void mappedSelect() {
        MappedSelect<Artist> mappedSelect = MappedSelect
                .query("SelectTestUpper", Artist.class)
                .queryTimeout(10);
        Query replacementQuery = mappedSelect.createReplacementQuery(env.context().getEntityResolver());
        assertEquals(10, replacementQuery
                .getMetaData(env.context().getEntityResolver())
                .getQueryTimeout());
        env.context().performQuery(replacementQuery);
    }
}
