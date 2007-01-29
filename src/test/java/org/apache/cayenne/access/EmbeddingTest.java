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
package org.apache.cayenne.access;

import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.embeddable.EmbedEntity1;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CayenneResources;

public class EmbeddingTest extends CayenneCase {

    public static final String EMBEDDING_ACCESS_STACK = "EmbeddingStack";

    protected AccessStack buildAccessStack() {
        return CayenneResources.getResources().getAccessStack(EMBEDDING_ACCESS_STACK);
    }

    public void testSelect() throws Exception {
        createTestData("testSelect");

        SelectQuery query = new SelectQuery(EmbedEntity1.class);
        query.addOrdering(EmbedEntity1.NAME_PROPERTY, true);

        ObjectContext context = createDataContext();

        List results = context.performQuery(query);
        assertEquals(2, results.size());
    }
}
