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
package org.apache.cayenne.merge;

import java.sql.Types;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.TESTMAP_PROJECT)
public class SetPrimaryKeyToDbTest extends MergeCase {

    public void test() throws Exception {
        dropTableIfPresent("NEW_TABLE");
        assertTokensAndExecute(0, 0);

        DbEntity dbEntity1 = new DbEntity("NEW_TABLE");

        DbAttribute e1col1 = new DbAttribute("ID1", Types.INTEGER, dbEntity1);
        e1col1.setMandatory(true);
        e1col1.setPrimaryKey(true);
        dbEntity1.addAttribute(e1col1);
        map.addDbEntity(dbEntity1);

        assertTokensAndExecute(1, 0);
        assertTokensAndExecute(0, 0);

        DbAttribute e1col2 = new DbAttribute("ID2", Types.INTEGER, dbEntity1);
        e1col2.setMandatory(true);
        dbEntity1.addAttribute(e1col2);

        assertTokensAndExecute(2, 0);
        assertTokensAndExecute(0, 0);

        e1col1.setPrimaryKey(false);
        e1col2.setPrimaryKey(true);

        assertTokensAndExecute(1, 0);
        assertTokensAndExecute(0, 0);
    }
}
