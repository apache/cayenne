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

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class QueryTimeoutIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Test
    public void testObjectSelect() {
        ObjectSelect<Artist> objectSelect = ObjectSelect.query(Artist.class)
                .queryTimeout(10);
        assertEquals(10, objectSelect
                .getMetaData(context.getEntityResolver())
                .getQueryTimeout());
        objectSelect.select(context);
    }

    @Test
    public void testSQLTemplate() {
        SQLTemplate sqlTemplate = new SQLTemplate();
        sqlTemplate.setDefaultTemplate("SELECT * FROM ARTIST");
        sqlTemplate.setQueryTimeout(10);
        assertEquals(10, sqlTemplate
                .getMetaData(context.getEntityResolver())
                .getQueryTimeout());
        context.performQuery(sqlTemplate);
    }

    @Test
    public void testColumnSelect() {
        ColumnSelect<String> columnSelect = ObjectSelect
                .columnQuery(Artist.class, Artist.ARTIST_NAME)
                .queryTimeout(10);
        assertEquals(10, columnSelect
                .getMetaData(context.getEntityResolver())
                .getQueryTimeout());
        context.performQuery(columnSelect);
    }

    @Test
    public void testEjbql() {
        EJBQLQuery ejbqlQuery = new EJBQLQuery("select a from Artist a");
        ejbqlQuery.setQueryTimeout(10);
        assertEquals(10, ejbqlQuery
                .getMetaData(context.getEntityResolver())
                .getQueryTimeout());
        context.performQuery(ejbqlQuery);
    }

    @Test
    public void testSqlSelect() {
        SQLSelect<Artist> sqlSelect = SQLSelect
                .query(Artist.class, "SELECT * FROM ARTIST")
                .queryTimeout(10);
        assertEquals(10, sqlSelect
                .getMetaData(context.getEntityResolver())
                .getQueryTimeout());
        context.performQuery(sqlSelect);
    }

    @Test
    public void testSqlExec() {
        SQLExec sqlExec = SQLExec
                .query("SELECT * FROM ARTIST")
                .queryTimeout(10);
        assertEquals(10, sqlExec
                .getMetaData(context.getEntityResolver())
                .getQueryTimeout());
        context.performQuery(sqlExec);
    }

    @Test
    public void testMappedSelect() {
        MappedSelect<Artist> mappedSelect = MappedSelect
                .query("SelectTestUpper", Artist.class)
                .queryTimeout(10);
        Query replacementQuery = mappedSelect.createReplacementQuery(context.getEntityResolver());
        assertEquals(10, replacementQuery
                .getMetaData(context.getEntityResolver())
                .getQueryTimeout());
        context.performQuery(replacementQuery);
    }
}
