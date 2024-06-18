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

import org.apache.cayenne.DataRow;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.runtime.CayenneRuntime;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class QueryChainIT extends RuntimeCase {

    @Inject
    private CayenneRuntime runtime;

    @Test
    public void testSelectQuery() {

        QueryChain chain = new QueryChain();
        chain.addQuery(ObjectSelect.query(Artist.class));
        chain.addQuery(ObjectSelect.query(Artist.class));

        QueryMetadata md = chain.getMetaData(runtime.getDataDomain().getEntityResolver());

        assertNotNull(md);
        assertTrue(md.isFetchingDataRows());
        assertNull(md.getObjEntity());
    }

    @Test
    public void testSelectQueryDataRows() {

        QueryChain chain = new QueryChain();
        ObjectSelect<DataRow> q1 = ObjectSelect.dataRowQuery(Artist.class);
        chain.addQuery(q1);

        ObjectSelect<DataRow> q2 = ObjectSelect.dataRowQuery(Artist.class);
        chain.addQuery(q2);

        QueryMetadata md = chain.getMetaData(runtime.getDataDomain().getEntityResolver());

        assertNotNull(md);
        assertTrue(md.isFetchingDataRows());
        assertNull(md.getObjEntity());
    }
}
