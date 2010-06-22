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
package org.apache.cayenne;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ClientCase.MULTI_TIER_PROJECT)
public class CayenneContextSQLTemplateTest extends ClientCase {

    @Inject
    private DBHelper dbHelper;

    @Inject
    private CayenneContext context;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("MT_TABLE2");
        dbHelper.deleteAll("MT_TABLE1");
    }

    public void testObjectRoot() throws Exception {

        assertNull(Cayenne.objectForPK(context, ClientMtTable1.class, 1));
        context.performGenericQuery(new SQLTemplate(
                ClientMtTable1.class,
                "insert into MT_TABLE1 "
                        + "(TABLE1_ID, GLOBAL_ATTRIBUTE1, SERVER_ATTRIBUTE1) "
                        + "values (1, 'g1', 's1')"));

        assertNotNull(Cayenne.objectForPK(context, ClientMtTable1.class, 1));
    }
}
