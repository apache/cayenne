/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cayenne.query.sql.generator;

import de.jexp.jequel.table.BaseTable;
import de.jexp.jequel.table.Field;
import org.apache.cayenne.map.DataMap;
import org.junit.Test;

import java.util.Map;

import static org.apache.cayenne.merge.builders.ObjectMother.*;

public class DbModelGeneratorTest {

    @Test
    public void testGenerate() throws Exception {
        DbModelGenerator generator = new DbModelGenerator();

        StringBuilder res = new StringBuilder();

        DataMap dataMap = new DataMap();
        dataMap.addDbEntity(dbEntity("Table1").attributes(
                dbAttr("id").primaryKey(),
                dbAttr("name").typeVarchar(25),
                dbAttr("age").typeInt()
        ).build());

        generator.generate(res, dataMap, "org.apache.cayenne", "MyWorld");

        System.out.println("res = " + res);
    }

    interface DbModel {
        Table1 Table1 = new Table1();
        final class Table1 extends BaseTable<Table1> {
            public final Field age = integer();
            public final Field id = timestamp().primaryKey();
            public final Field name = string();

            {initFields();}
        }
    }

    @Test
    public void testFieldInitialization() {
        for (Map.Entry<String, Field<?>> o : DbModel.Table1.getFields().entrySet()) {
            System.out.println("o = " + o);
        }
    }
}