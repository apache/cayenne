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

import org.apache.cayenne.CayenneSqlException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SQLSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that failures happening while a statement is executed against the database surface as a
 * {@link CayenneSqlException} carrying the failing query and its translated SQL.
 */
public class CayenneSqlExceptionIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private DataContext context;

    @BeforeEach
    public void setUp() {
        context = env.context();
    }

    @Test
    public void selectWithInvalidSql() {
        // deliberately malformed SQL
        String badSql = "SELECT FROM NOT ARTIST WHERE";
        SQLSelect<Artist> query = SQLSelect.query(Artist.class, badSql);

        CayenneSqlException e = assertThrows(CayenneSqlException.class, () -> query.select(context));
//        e.printStackTrace();

        assertNotNull(e.getStatement(), "statement should have been captured");
        // SQLSelect is executed as an internally-routed SQLTemplate, so getQuery() reports that executed query
        assertNotNull(e.getQuery());
        assertTrue(e.getMessage().contains(badSql),
                () -> "Expected message to include the failing SQL, but was: " + e.getMessage());
    }

    @Test
    public void objectSelectWithBadExpressionParameter() {
        // comparing the numeric PK column to a non-numeric value produces valid SQL that the DB rejects when the
        // bogus parameter is bound and the statement is executed
        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class)
                .where(ExpressionFactory.matchDbExp(Artist.ARTIST_ID_PK_COLUMN, "not-a-number"));

        CayenneSqlException e = assertThrows(CayenneSqlException.class, () -> query.select(context));
//        e.printStackTrace();

        assertNotNull(e.getStatement(), "statement should have been captured");
        assertSame(query, e.getQuery());
        assertTrue(e.getStatement().sql().contains("ARTIST"),
                () -> "Expected SELECT SQL against ARTIST, but was: " + e.getStatement().sql());
    }

    @Test
    public void commitWithNonExistingColumn() {
        // point the "artistName" attribute at a column that does not exist in the ARTIST table, so the generated
        // INSERT references an unknown column and the DB rejects it on commit
        DbEntity artistDb = context.getEntityResolver().getDbEntity("ARTIST");
        DbAttribute nameColumn = artistDb.getAttribute("ARTIST_NAME");
        artistDb.removeAttribute("ARTIST_NAME");
        nameColumn.setName("BOGUS_COLUMN");
        artistDb.addAttribute(nameColumn);

        ObjEntity artistObj = context.getEntityResolver().getObjEntity("Artist");
        ObjAttribute nameAttribute = artistObj.getAttribute("artistName");
        nameAttribute.setDbAttributePath("BOGUS_COLUMN");

        context.getEntityResolver().refreshMappingCache();

        Artist artist = context.newObject(Artist.class);
        artist.setArtistName("aaa");

        CayenneSqlException e = assertThrows(CayenneSqlException.class, () -> context.commitChanges());
//        e.printStackTrace();

        assertNotNull(e.getStatement(), "statement should have been captured");
        assertNotNull(e.getQuery());
        assertTrue(e.getStatement().sql().contains("BOGUS_COLUMN"),
                () -> "Expected INSERT SQL to reference BOGUS_COLUMN, but was: " + e.getStatement().sql());
        assertTrue(e.getMessage().contains("BOGUS_COLUMN"),
                () -> "Expected message to include the failing SQL, but was: " + e.getMessage());
    }
}
