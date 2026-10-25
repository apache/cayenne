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
package org.apache.cayenne.docs;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.dba.oracle.OracleAdapter;
import org.apache.cayenne.dba.sqlserver.SQLServerAdapter;
import org.apache.cayenne.docs.persistent.Painting;
import org.apache.cayenne.query.SQLExec;
import org.apache.cayenne.query.SQLSelect;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class SQLSelectTest extends BaseTest {

    @Test
    public void select() {
        createArtistsDataSet();

        // tag::select[]
        // Selecting objects
        List<Painting> paintings = SQLSelect
                .query(Painting.class, "SELECT * FROM PAINTING WHERE TITLE LIKE 'G%'")
                .upperColumnNames()
                .localCache()
                .limit(100)
                .select(context);

        // Selecting scalar values
        List<String> paintingNames = SQLSelect
                .scalarQuery("SELECT TITLE FROM PAINTING WHERE ESTIMATED_PRICE > 100000", String.class)
                .select(context);

        // Selecting DataRow with predefined types
        List<DataRow> rows = SQLSelect
                .dataRowQuery("SELECT * FROM ARTIST", Integer.class, String.class, LocalDate.class)
                .select(context);

        // Selecting Object[] with predefined types
        List<Object[]> arrays = SQLSelect
                .columnQuery("SELECT * FROM ARTIST", Integer.class, String.class, LocalDate.class)
                .select(context);
        // end::select[]

        assertEquals(2, paintings.size());
        assertEquals(List.of("Guernica"), paintingNames);
        assertEquals(3, rows.size());
        assertEquals(3, arrays.size());
        assertInstanceOf(LocalDate.class, arrays.get(0)[2]);
    }

    @Test
    public void dialect() {
        createArtistsDataSet();

        // tag::dialect[]
        List<String> firstName = SQLSelect
                // default SQL, used with any database that has no dialect below
                .scalarQuery("SELECT NAME FROM ARTIST ORDER BY NAME LIMIT 1", String.class)
                .dialect(SQLServerAdapter.class, "SELECT TOP 1 NAME FROM ARTIST ORDER BY NAME")
                .dialect(OracleAdapter.class, "SELECT NAME FROM ARTIST ORDER BY NAME FETCH FIRST 1 ROWS ONLY")
                .select(context);
        // end::dialect[]

        assertEquals(List.of("Aivazovsky"), firstName);
    }

    @Test
    public void dataNode() {
        createArtistsDataSet();

        // tag::dataNode[]
        List<String> names = SQLSelect
                .scalarQuery("SELECT NAME FROM ARTIST ORDER BY NAME", String.class)
                .dataNode("docs")
                .select(context);

        int inserted = SQLExec
                .query("INSERT INTO ARTIST (ID, NAME) VALUES (55, 'Picasso')")
                .dataNode("docs")
                .update(context);
        // end::dataNode[]

        assertEquals("Aivazovsky", names.get(0));
        assertEquals(1, inserted);
    }

    @Test
    public void exec() {
        // tag::exec[]
        int inserted = SQLExec
                .query("INSERT INTO ARTIST (ID, NAME) VALUES (55, 'Picasso')")
                .update(context);
        // end::exec[]

        assertEquals(1, inserted);
    }
}
