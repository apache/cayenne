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
public class SetAllowNullToDbTest extends MergeCase {

    public void test() throws Exception {
        DbEntity dbEntity = map.getDbEntity("PAINTING");
        assertNotNull(dbEntity);

        // create and add new column to model and db
        DbAttribute column = new DbAttribute("NEWCOL2", Types.VARCHAR, dbEntity);

        try {

            column.setMandatory(true);
            column.setMaxLength(10);
            dbEntity.addAttribute(column);
            assertTokensAndExecute(2, 0);

            // check that is was merged
            assertTokensAndExecute(0, 0);

            // set null
            column.setMandatory(false);

            // merge to db
            assertTokensAndExecute(1, 0);

            // check that is was merged
            assertTokensAndExecute(0, 0);

            // clean up
        }
        finally {
            dbEntity.removeAttribute(column.getName());
            assertTokensAndExecute(1, 0);
            assertTokensAndExecute(0, 0);
        }
    }

}
