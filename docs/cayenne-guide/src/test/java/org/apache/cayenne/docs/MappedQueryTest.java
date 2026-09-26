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

import org.apache.cayenne.QueryResult;
import org.apache.cayenne.UpdateResult;
import org.apache.cayenne.docs.persistent.Artist;
import org.apache.cayenne.query.MappedExec;
import org.apache.cayenne.query.MappedSelect;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MappedQueryTest extends BaseTest {

    @Test
    public void mappedSelect() {
        createArtistsDataSet();

        // tag::mappedSelect[]
        List<Artist> results = MappedSelect.query("artistsByName", Artist.class)
                .param("name", "Dali")
                .select(context);
        // end::mappedSelect[]

        assertEquals(1, results.size());
    }

    @Test
    public void mappedExec() {
        createArtistsDataSet();

        // tag::mappedExec[]
        List<QueryResult> result = MappedExec.query("updatePaintingTitle")
                .param("title", "Untitled")
                .execute(context);
        UpdateResult update = (UpdateResult) result.getFirst();
        System.out.println("Rows updated: " + update.count());
        // end::mappedExec[]

        assertEquals(4, update.count());
    }
}
