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

package org.apache.cayenne.unit.jira;

import java.sql.Types;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.relationship.FkOfDifferentType;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.RELATIONSHIPS_PROJECT)
public class CAY_191Test extends ServerCase {
    
    @Inject
    protected DataContext context;
    
    @Inject
    protected DBHelper dbHelper;
    
    protected TableHelper tRelationshipHelper;
    protected TableHelper tFkOfDifferentType;
    
    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("FK_OF_DIFFERENT_TYPE");
        dbHelper.update("REFLEXIVE_AND_TO_ONE").set("PARENT_ID", null, Types.INTEGER).execute();
        dbHelper.deleteAll("REFLEXIVE_AND_TO_ONE");
        dbHelper.deleteAll("RELATIONSHIP_HELPER");
        
        tRelationshipHelper = new TableHelper(dbHelper, "RELATIONSHIP_HELPER");
        tRelationshipHelper.setColumns("NAME", "RELATIONSHIP_HELPER_ID");
        
        tFkOfDifferentType = new TableHelper(dbHelper, "FK_OF_DIFFERENT_TYPE");
        tFkOfDifferentType.setColumns("ID", "RELATIONSHIP_HELPER_FK");
    }
    
    protected void createTestDataSet() throws Exception {
        tRelationshipHelper.insert("RH1", 1);
        tFkOfDifferentType.insert(1, 1);
    }

    public void testResolveToOneOverFKOfDifferentNumType() throws Exception {
        // this is mostly for legacy schemas, as on many dbs you won;t be able to even
        // create the FK constraint...

        createTestDataSet();

        FkOfDifferentType root = Cayenne.objectForPK(
                context,
                FkOfDifferentType.class,
                1);

        assertNotNull(root);
        assertNotNull(root.getRelationshipHelper());
        assertEquals("RH1", root.getRelationshipHelper().getName());
    }
}
