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

package org.apache.cayenne;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.meaningful_pk.ClientMeaningfulPk;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

@UseServerRuntime(CayenneProjects.MEANINGFUL_PK_PROJECT)
public class CayenneContextMeaningfulPKIT extends ClientCase {

    @Inject
    private CayenneContext clientContext;

    @Inject
    private DBHelper dbHelper;

    private TableHelper tMeaningfulPK;

    @Before
    public void setUp() throws Exception {
        tMeaningfulPK = new TableHelper(dbHelper, "MEANINGFUL_PK");
        tMeaningfulPK.setColumns("PK");
    }

    private void deleteAndCreateTwoMeaningfulPKsDataSet() throws Exception {
        tMeaningfulPK.deleteAll();
        tMeaningfulPK.insert("A");
        tMeaningfulPK.insert("B");
    }

    @Test
    public void testMeaningfulPK() throws Exception {
        deleteAndCreateTwoMeaningfulPKsDataSet();

        List<?> results = ObjectSelect.query(ClientMeaningfulPk.class)
                .orderBy(ClientMeaningfulPk.PK.desc())
                .select(clientContext);
        assertEquals(2, results.size());
    }

}
