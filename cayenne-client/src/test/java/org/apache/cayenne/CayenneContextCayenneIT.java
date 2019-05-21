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
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.testdo.mt.ClientMtTable1;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@UseServerRuntime(CayenneProjects.MULTI_TIER_PROJECT)
public class CayenneContextCayenneIT extends ClientCase {

    @Inject
    private CayenneContext context;

    @Test
    public void testObjectForPK() throws Exception {

        context.performGenericQuery(new SQLTemplate(
                ClientMtTable1.class,
                "insert into MT_TABLE1 "
                        + "(TABLE1_ID, GLOBAL_ATTRIBUTE1, SERVER_ATTRIBUTE1) "
                        + "values (1, 'g1', 's1')"));

        ClientMtTable1 o = Cayenne.objectForPK(context, ClientMtTable1.class, 1);
        assertNotNull(o);
        assertEquals("g1", o.getGlobalAttribute1());
    }
}
