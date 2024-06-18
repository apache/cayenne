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

import java.util.List;

import org.apache.cayenne.DataRow;
import org.apache.cayenne.QueryResponse;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.QueryChain;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class DataContextQueryChainIT extends RuntimeCase {

    @Inject
    private DataContext context;

    @Test
    public void testSelectQuery() {
        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("X");
        context.commitChanges();

        QueryChain chain = new QueryChain();
        chain.addQuery(ObjectSelect.query(Artist.class));
        chain.addQuery(ObjectSelect.query(Artist.class));

        QueryResponse r = context.performGenericQuery(chain);

        // data comes back as datarows
        assertEquals(2, r.size());
        r.reset();
        r.next();
        List<?> l1 = r.currentList();
        r.next();
        List<?> l2 = r.currentList();

        assertTrue(l1.get(0) instanceof DataRow);
        assertTrue(l2.get(0) instanceof DataRow);
    }
}
