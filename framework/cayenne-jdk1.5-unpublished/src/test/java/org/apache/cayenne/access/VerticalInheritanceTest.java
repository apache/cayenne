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
package org.apache.cayenne.access;

import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.test.DBHelper;
import org.apache.cayenne.test.TableHelper;
import org.apache.cayenne.testdo.inheritance.vertical.IvRoot;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CayenneResources;

public class VerticalInheritanceTest extends CayenneCase {

    @Override
    protected AccessStack buildAccessStack() {
        return CayenneResources.getResources().getAccessStack("InheritanceVerticalStack");
    }

    public void testSelectQuery() throws Exception {
        DBHelper dbHelper = getDbHelper();

        TableHelper ivRootTable = new TableHelper(dbHelper, "IV_ROOT");
        ivRootTable.setColumns("ID", "NAME");

        TableHelper ivSub1Table = new TableHelper(dbHelper, "IV_SUB1");
        ivSub1Table.setColumns("ID", "SUB1_NAME");

        // delete
        ivSub1Table.deleteAll();
        ivRootTable.deleteAll();

        // insert
        ivRootTable.insert(1, "xROOT");
        ivRootTable.insert(2, "xSUB1_ROOT");
        ivSub1Table.insert(2, "xSUB1");

        SelectQuery query = new SelectQuery(IvRoot.class);
        // List<IvRoot> results = createDataContext().performQuery(query);

        // assertEquals(2, results.size());
    }
}
