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

package org.apache.cayenne.value.json;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SelectById;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.json.JsonOther;
import org.apache.cayenne.testdo.json.JsonVarchar;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.server.CayenneProjects;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;
import org.apache.cayenne.value.Json;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@UseServerRuntime(CayenneProjects.JSON_PROJECT)
public class JsonTypeIT extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private DBHelper dbHelper;

    @Inject
    private UnitDbAdapter unitDbAdapter;

    private TableHelper tJsonVarchar;
    private TableHelper tJsonOther;

    @Before
    public void setUp() throws Exception {
        tJsonVarchar = new TableHelper(dbHelper, "JSON_VARCHAR");
        tJsonVarchar.setColumns("ID", "DATA");

        tJsonOther = new TableHelper(dbHelper, "JSON_OTHER");
        tJsonOther.setColumns("ID", "DATA");
    }

    @Test
    public void testJson() {
        String jsonString = "{\"id\": 1, \"property\": \"value\"}";
        testJsonVarchar(jsonString);
        if (unitDbAdapter.supportsJsonType()) {
            testJsonOther(jsonString);
        }
        System.out.println("gg ez");
    }

    private void testJsonOther(String jsonString) {
        JsonOther jsonInsert = context.newObject(JsonOther.class);
        jsonInsert.setData(new Json(jsonString));
        context.commitChanges();

        JsonOther jsonSelect = context.selectOne(SelectById.query(JsonOther.class, jsonInsert.getObjectId()));
        Assert.assertEquals(jsonInsert.getData(), jsonSelect.getData());
    }

    private void testJsonVarchar(String jsonString) {
        JsonVarchar jsonInsert = context.newObject(JsonVarchar.class);
        jsonInsert.setData(new Json(jsonString));
        context.commitChanges();

        JsonVarchar jsonSelect = context.selectOne(SelectById.query(JsonVarchar.class, jsonInsert.getObjectId()));
        Assert.assertEquals(jsonInsert.getData(), jsonSelect.getData());
    }
}
