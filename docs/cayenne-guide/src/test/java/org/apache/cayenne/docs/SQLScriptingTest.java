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

import org.apache.cayenne.docs.persistent.Artist;
import org.apache.cayenne.docs.persistent.Painting;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SQLExec;
import org.apache.cayenne.query.SQLSelect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SQLScriptingTest extends BaseTest {

    @Test
    public void variable() {
        createArtistsDataSet();

        // tag::variable[]
        // this will generate SQL like this: "delete from PUBLIC.PAINTING"
        SQLExec query = SQLExec.query("delete from $tableName")
                .params("tableName", "PUBLIC.PAINTING");
        // end::variable[]

        assertEquals(4, query.update(context));
    }

    @Test
    public void bindObjectEqual() {
        createArtistsDataSet();
        Artist dali = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("Dali")).selectOne(context);

        // tag::bindObjectEqual[]
        String sql = "SELECT * FROM PAINTING t0 WHERE #bindObjectEqual($a 't0.ARTIST_ID' 'ARTIST_ID' )";
        Artist artistParam = dali;

        SQLSelect<Painting> select = SQLSelect.query(Painting.class, sql)
                .param("a", artistParam);
        // end::bindObjectEqual[]

        assertEquals(2, select.select(context).size());
    }

    @Test
    public void bindObjectNotEqual() {
        createArtistsDataSet();
        Artist dali = ObjectSelect.query(Artist.class).where(Artist.ARTIST_NAME.eq("Dali")).selectOne(context);

        // tag::bindObjectNotEqual[]
        String sql = "SELECT * FROM PAINTING t0 WHERE #bindObjectNotEqual($a 't0.ARTIST_ID' 'ARTIST_ID' )";
        Artist artistParam = dali;

        SQLSelect<Painting> select = SQLSelect.query(Painting.class, sql)
                .param("a", artistParam);
        // end::bindObjectNotEqual[]

        assertEquals(2, select.select(context).size());
    }
}
