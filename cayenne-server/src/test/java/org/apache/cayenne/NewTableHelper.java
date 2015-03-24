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
package org.apache.cayenne;

import de.jexp.jequel.expression.IColumn;
import de.jexp.jequel.expression.Table;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;

import java.util.Map;

/**
 * @since 4.0
 */
public class NewTableHelper extends TableHelper {
    public NewTableHelper(DBHelper dbHelper, Table<?> table) {
        super(dbHelper, table.getName());

        Map<String, IColumn> fields = table.getFields();
        String[] columns = new String[fields.size()];
        int[] types = new int[fields.size()];
        int i = 0;
        for (IColumn<?> entry : fields.values()) {
            columns[i] = entry.getName();
            types[i] = entry.getJdbcType();
            i++;
        }
        setColumns(columns);
        setColumnTypes(types);
    }

    public NewTableHelper(DBHelper dbHelper, String tableName, String... columns) {
        super(dbHelper, tableName, columns);
    }
}
