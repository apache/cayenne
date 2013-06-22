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

package org.apache.cayenne.ashwood;

import java.sql.Types;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.relationship.ReflexiveAndToOne;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.RELATIONSHIPS_PROJECT)
public class AshwoodEntitySorterTest extends ServerCase {

    @Inject
    protected DBHelper dbHelper;

    @Inject
    protected ObjectContext context;

    protected TableHelper tRelationshipHelper;
    protected TableHelper tReflexiveAndToOne;

    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("REFLEXIVE_AND_TO_ONE");
        dbHelper.deleteAll("FK_OF_DIFFERENT_TYPE");
        dbHelper.deleteAll("MEANINGFUL_FK");
        dbHelper.deleteAll("RELATIONSHIP_HELPER");

        tRelationshipHelper = new TableHelper(dbHelper, "RELATIONSHIP_HELPER");
        tRelationshipHelper.setColumns("RELATIONSHIP_HELPER_ID", "NAME");

        tReflexiveAndToOne = new TableHelper(dbHelper, "REFLEXIVE_AND_TO_ONE");
        tReflexiveAndToOne.setColumns("REFLEXIVE_AND_TO_ONE_ID", "PARENT_ID", "RELATIONSHIP_HELPER_ID", "NAME")
                .setColumnTypes(Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.VARCHAR);
    }

    public void testSortObjectsForEntityReflexiveWithFaults() throws Exception {

        tRelationshipHelper.insert(1, "rh1");
        tReflexiveAndToOne.insert(1, null, 1, "r1");
        tReflexiveAndToOne.insert(2, 1, 1, "r2");
        tReflexiveAndToOne.insert(3, 2, 1, "r3");

        AshwoodEntitySorter sorter = new AshwoodEntitySorter();
        sorter.setEntityResolver(context.getEntityResolver());

        ObjEntity entity = context.getEntityResolver().getObjEntity(ReflexiveAndToOne.class);

        List<?> objects = context.performQuery(new SelectQuery(ReflexiveAndToOne.class));
        Collections.shuffle(objects);
        assertEquals(3, objects.size());

        sorter.sortObjectsForEntity(entity, objects, true);

        assertEquals("r3", ((ReflexiveAndToOne) objects.get(0)).getName());
        assertEquals("r2", ((ReflexiveAndToOne) objects.get(1)).getName());
        assertEquals("r1", ((ReflexiveAndToOne) objects.get(2)).getName());
    }
}
