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

package org.apache.cayenne.dbsync.merge.token;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.dbsync.merge.token.db.AddColumnToDb;
import org.apache.cayenne.dbsync.merge.token.db.AddRelationshipToDb;
import org.apache.cayenne.dbsync.merge.token.db.CreateTableToDb;
import org.apache.cayenne.dbsync.merge.token.db.DropColumnToDb;
import org.apache.cayenne.dbsync.merge.token.db.DropRelationshipToDb;
import org.apache.cayenne.dbsync.merge.token.db.DropTableToDb;
import org.apache.cayenne.dbsync.merge.token.model.AddColumnToModel;
import org.apache.cayenne.dbsync.merge.token.model.AddRelationshipToModel;
import org.apache.cayenne.dbsync.merge.token.model.CreateTableToModel;
import org.apache.cayenne.dbsync.merge.token.model.DropColumnToModel;
import org.apache.cayenne.dbsync.merge.token.model.DropRelationshipToModel;
import org.apache.cayenne.dbsync.merge.token.model.DropTableToModel;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TokenSortTest {

    private List<String> toClassesNames(List<MergerToken> sort) {
        List<String> res = new ArrayList<String>(sort.size());
        for (MergerToken mergerToken : sort) {
            res.add(mergerToken.getClass().getSimpleName());
        }
        return res;
    }

    @Test
    public void testSortStability() throws Exception {
        List<MergerToken> tokens = Arrays.<MergerToken>asList(
                new AddRelationshipToModel(null, null),
                new AddColumnToModel(null, null),
                new AddColumnToDb(null, null),
                new CreateTableToModel(null),
                new DropTableToModel(null),
                new DropColumnToModel(null, null),
                new DropRelationshipToModel(null, null)
        );
        Collections.sort(tokens);
        List<String> actual = toClassesNames(tokens);

        List<String> expected = Arrays.asList(
                "DropRelationshipToModel",
                "DropColumnToModel",
                "DropTableToModel",
                "CreateTableToModel",
                "AddColumnToDb",
                "AddColumnToModel",
                "AddRelationshipToModel"
        );

        assertEquals(expected, actual);

        Collections.sort(tokens);
        actual = toClassesNames(tokens);
        assertEquals(expected, actual);

        List<MergerToken> tokens2 = Arrays.<MergerToken>asList(
                new AddColumnToModel(null, null),
                new AddRelationshipToModel(null, null),
                new CreateTableToModel(null),
                new AddColumnToDb(null, null),
                new DropColumnToModel(null, null),
                new DropRelationshipToModel(null, null),
                new DropTableToModel(null)
        );
        Collections.sort(tokens2);
        actual = toClassesNames(tokens);
        assertEquals(expected, actual);
    }

    @Test
    public void testToModelTokensCompare() throws Exception {
        List<MergerToken> tokens = Arrays.<MergerToken>asList(
                new DropColumnToModel(null, null),
                new DropRelationshipToModel(null, null),
                new DropTableToModel(null),
                new AddColumnToDb(null, null),
                new AddRelationshipToModel(null, null),
                new AddColumnToModel(null, null),
                new CreateTableToModel(null));
        Collections.sort(tokens);

        List<String> actual = toClassesNames(tokens);
        List<String> expected = Arrays.asList(
                "DropRelationshipToModel",
                "DropColumnToModel",
                "DropTableToModel",
                "CreateTableToModel",
                "AddColumnToDb",
                "AddColumnToModel",
                "AddRelationshipToModel"
        );

        assertEquals(expected, actual);
    }

    @Test
    public void testToDbTokensCompare() throws Exception {
        List<MergerToken> tokens = Arrays.<MergerToken>asList(
                new DropColumnToDb(null, null),
                new DropRelationshipToDb(null, null),
                new DropTableToDb(null),
                new AddColumnToModel(null, null),
                new AddRelationshipToDb(null, null),
                new AddColumnToDb(null, null),
                new CreateTableToDb(null));
        Collections.sort(tokens);

        List<String> actual = toClassesNames(tokens);
        List<String> expected = Arrays.asList(
                "DropRelationshipToDb",
                "DropColumnToDb",
                "DropTableToDb",
                "CreateTableToDb",
                "AddColumnToDb",
                "AddColumnToModel",
                "AddRelationshipToDb"
        );

        assertEquals(expected, actual);
    }

}
