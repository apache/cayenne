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

package org.apache.cayenne.access.translator.select;

import java.sql.Connection;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.ServerCaseDataSourceFactory;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class QueryAssemblerTest extends ServerCase {

    @Inject
    private DataNode dataNode;

    @Inject
    private ServerCaseDataSourceFactory dataSourceFactory;

    private Connection connection;

    private TstQueryAssembler qa;

    @Override
    protected void setUpAfterInjection() throws Exception {
        this.connection = dataSourceFactory.getSharedDataSource().getConnection();
        this.qa = new TstQueryAssembler(new SelectQuery<Object>(), dataNode, connection);
    }

    @Override
    protected void tearDownBeforeInjection() throws Exception {
        connection.close();
    }

    public void testGetQuery() throws Exception {
        assertNotNull(qa.getQuery());
    }

    public void testAddToParamList() throws Exception {

        assertEquals(0, qa.getAttributes().size());
        assertEquals(0, qa.getValues().size());

        qa.addToParamList(new DbAttribute(), new Object());
        assertEquals(1, qa.getAttributes().size());
        assertEquals(1, qa.getValues().size());
    }

    public void testCreateStatement() throws Exception {
        assertNotNull(qa.createStatement());
    }
}
