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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.testdo.qualified.Qualified1;
import org.apache.cayenne.testdo.qualified.Qualified2;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CayenneResources;

public class CDOQualifiedEntitiesTest extends CayenneCase {

    @Override
    protected AccessStack buildAccessStack() {
        return CayenneResources.getResources().getAccessStack(QUALIFIED_ACCESS_STACK);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    public void testReadToMany() throws Exception {
        if (getAccessStackAdapter().supportsNullBoolean()) {
            ObjectContext context = createDataContext();

            // prepare data set...
            SQLTemplate insert1 = new SQLTemplate(
                    Qualified1.class,
                    "insert into QUALIFIED1 (ID, NAME, DELETED) "
                            + "values (#bind($id), #bind($name), #bind($deleted 'BOOLEAN'))");
            Map<String, Object> parameters1 = new HashMap<String, Object>();
            parameters1.put("id", 1);
            parameters1.put("name", "OX1");
            parameters1.put("deleted", null);

            Map<String, Object> parameters2 = new HashMap<String, Object>();
            parameters2.put("id", 2);
            parameters2.put("name", "OX2");
            parameters2.put("deleted", true);

            insert1.setParameters(parameters1, parameters2);
            context.performQuery(insert1);

            SQLTemplate insert2 = new SQLTemplate(
                    Qualified2.class,
                    "insert into QUALIFIED2 (ID, NAME, DELETED, QUALIFIED1_ID) "
                            + "values (#bind($id), #bind($name), #bind($deleted 'BOOLEAN'), #bind($q1id))");
            Map<String, Object> parameters3 = new HashMap<String, Object>();
            parameters3.put("id", 1);
            parameters3.put("name", "OY1");

            parameters3.put("deleted", null);
            parameters3.put("q1id", 1);

            Map<String, Object> parameters4 = new HashMap<String, Object>();
            parameters4.put("id", 2);
            parameters4.put("name", "OY2");
            parameters4.put("deleted", true);
            parameters4.put("q1id", 1);

            Map<String, Object> parameters5 = new HashMap<String, Object>();
            parameters5.put("id", 3);
            parameters5.put("name", "OY3");
            parameters5.put("deleted", null);
            parameters5.put("q1id", 2);

            Map<String, Object> parameters6 = new HashMap<String, Object>();
            parameters6.put("id", 4);
            parameters6.put("name", "OY4");
            parameters6.put("deleted", true);
            parameters6.put("q1id", 2);

            insert2.setParameters(parameters3, parameters4, parameters5, parameters6);
            context.performQuery(insert2);

            SelectQuery rootSelect = new SelectQuery(Qualified1.class);
            List<Qualified1> roots = context.performQuery(rootSelect);

            assertEquals(1, roots.size());

            Qualified1 root = roots.get(0);

            assertEquals("OX1", root.getName());

            List<Qualified2> related = root.getQualified2s();
            assertEquals(1, related.size());

            Qualified2 r = related.get(0);
            assertEquals("OY1", r.getName());
        }
    }

    public void testReadToOne() throws Exception {
        if (getAccessStackAdapter().supportsNullBoolean()) {
            ObjectContext context = createDataContext();

            // prepare data set...
            SQLTemplate insert1 = new SQLTemplate(
                    Qualified1.class,
                    "insert into QUALIFIED1 (ID, NAME, DELETED) "
                            + "values (#bind($id), #bind($name), #bind($deleted 'BOOLEAN'))");
            Map<String, Object> parameters1 = new HashMap<String, Object>();
            parameters1.put("id", 1);
            parameters1.put("name", "OX1");
            parameters1.put("deleted", null);

            Map<String, Object> parameters2 = new HashMap<String, Object>();
            parameters2.put("id", 2);
            parameters2.put("name", "OX2");
            parameters2.put("deleted", true);

            insert1.setParameters(parameters1, parameters2);
            context.performQuery(insert1);

            SQLTemplate insert2 = new SQLTemplate(
                    Qualified2.class,
                    "insert into QUALIFIED2 (ID, NAME, DELETED, QUALIFIED1_ID) "
                            + "values (#bind($id), #bind($name), #bind($deleted 'BOOLEAN'), #bind($q1id))");
            Map<String, Object> parameters3 = new HashMap<String, Object>();
            parameters3.put("id", 1);
            parameters3.put("name", "OY1");
            parameters3.put("deleted", null);
            parameters3.put("q1id", 2);

            insert2.setParameters(parameters3);
            context.performQuery(insert2);

            SelectQuery rootSelect = new SelectQuery(Qualified2.class);
            List<Qualified2> roots = context.performQuery(rootSelect);
            assertEquals(1, roots.size());

            Qualified2 root = roots.get(0);
            assertEquals("OY1", root.getName());

            Qualified1 target = root.getQualified1();
            assertNull("" + target, target);
        }
    }
}
