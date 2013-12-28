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
package org.apache.cayenne.query;

import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.mt.MtTable1;
import org.apache.cayenne.testdo.mt.MtTable1Subclass1;
import org.apache.cayenne.testdo.mt.MtTable1Subclass2;
import org.apache.cayenne.unit.di.client.ClientCase;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ClientCase.MULTI_TIER_PROJECT)
public class SelectQueryAbstractEntityTest extends ServerCase {
    
    @Inject
    private ObjectContext context;

    @Inject
    private DBHelper dbHelper;
    
    private TableHelper mtTable;
    
    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("MT_TABLE1");
        
        mtTable = new TableHelper(dbHelper, "MT_TABLE1");
        mtTable.setColumns("TABLE1_ID", "GLOBAL_ATTRIBUTE1", "SERVER_ATTRIBUTE1", "SUBCLASS_ATTRIBUTE1");
    }
    
    protected void createDataSet() throws Exception{     

        for (int i = 1; i <= 10; i++){
            mtTable.insert(i, "sub2", "sub2_" + i, "sub2attr");
        }
        
        for (int i = 11; i <= 20; i++){
            mtTable.insert(i, "sub1", "sub1_" + i, "sub1attr");
        }
       
    }
    
    public void testSublclassEntitySelect() throws Exception{
        createDataSet();
        
        SelectQuery<MtTable1Subclass1> query = 
                new SelectQuery<MtTable1Subclass1>(MtTable1Subclass1.class);
        final List<MtTable1Subclass1> sub1List = context.select(query);
        
        SelectQuery<MtTable1Subclass2> query2 = 
                new SelectQuery<MtTable1Subclass2>(MtTable1Subclass2.class);
        final List<MtTable1Subclass2> sub2List = context.select(query2);
        
        assertNotNull(sub1List);
        assertNotNull(sub2List);
    }
    
    public void test1AbstractEntitySelect() throws Exception{
        createDataSet();

        SelectQuery<MtTable1> query = new SelectQuery<MtTable1>(MtTable1.class);
        final List<MtTable1> list = context.select(query);

        assertNotNull(list);

        for (MtTable1 sub : list){
            if(sub instanceof MtTable1Subclass1){
                assertNotNull(((MtTable1Subclass1) sub).getSubclass1Attribute1());
            }
        }
    }
    
    public void test2AbstractEntitySelect() throws Exception{
        createDataSet();
        
        SelectQuery<MtTable1> query = new SelectQuery<MtTable1>(MtTable1.class);
        final List<MtTable1> list = context.select(query);
        
        assertNotNull(list);
        
        for (MtTable1 sub : list){
            if(sub instanceof MtTable1Subclass2){
                assertNotNull(((MtTable1Subclass2) sub).getSubclass2Attribute1());
            }
        }
    }
    
}
