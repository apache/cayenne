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

package org.apache.cayenne.tx;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.log.JdbcEventLogger;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class TransactionThreadIT extends RuntimeCase {

    @Inject
    private DataContext context;

    @Inject
    private JdbcEventLogger logger;

    @Test
    public void testThreadConnectionReuseOnSelect() throws Exception {

        Transaction t = new CayenneTransaction(logger);
        BaseTransaction.bindThreadTransaction(t);

        try {
            ObjectSelect.query(Artist.class).select(context);
            assertEquals(1, t.getConnections().size());

            // delegate will fail if the second query opens a new connection
            ObjectSelect.query(Artist.class).select(context);

            assertEquals(1, t.getConnections().size());

        } finally {
            BaseTransaction.bindThreadTransaction(null);
            t.commit();
        }
    }
}
