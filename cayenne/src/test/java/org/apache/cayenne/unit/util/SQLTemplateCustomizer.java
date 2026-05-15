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

package org.apache.cayenne.unit.util;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.query.SQLTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to customize SQLTemplate queries used in test cases per adapter.
 */
public class SQLTemplateCustomizer {

    private static final Map<String, Map<String, String>> map = new HashMap<>();

    static {
        Map<String, String> q1 = new HashMap<>();
        q1.put("org.apache.cayenne.dba.postgres.PostgresAdapter",
                "SELECT #result('ARTIST_ID'), RTRIM(#result('ARTIST_NAME')), "
                        + "#result('DATE_OF_BIRTH') FROM ARTIST ORDER BY ARTIST_ID");
        q1.put("org.apache.cayenne.dba.ingres.IngresAdapter",
                "SELECT #result('ARTIST_ID'), TRIM(#result('ARTIST_NAME')), "
                        + "#result('DATE_OF_BIRTH') FROM ARTIST ORDER BY ARTIST_ID");
        q1.put("org.apache.cayenne.dba.openbase.OpenBaseAdapter",
                "SELECT #result('ARTIST_ID'), #result('ARTIST_NAME'), "
                        + "#result('DATE_OF_BIRTH') FROM ARTIST ORDER BY ARTIST_ID");

        Map<String, String> q2 = new HashMap<>();
        q2.put("org.apache.cayenne.dba.postgres.PostgresAdapter",
                "SELECT #result('ARTIST_ID'), RTRIM(#result('ARTIST_NAME')), #result('DATE_OF_BIRTH') "
                        + "FROM ARTIST WHERE ARTIST_ID = #bind($id)");
        q2.put("org.apache.cayenne.dba.ingres.IngresAdapter",
                "SELECT #result('ARTIST_ID'), TRIM(#result('ARTIST_NAME')), #result('DATE_OF_BIRTH') "
                        + "FROM ARTIST WHERE ARTIST_ID = #bind($id)");
        q2.put("org.apache.cayenne.dba.openbase.OpenBaseAdapter",
                "SELECT #result('ARTIST_ID'), #result('ARTIST_NAME'), #result('DATE_OF_BIRTH') "
                        + "FROM ARTIST WHERE ARTIST_ID = #bind($id)");

        Map<String, String> q3 = new HashMap<>();
        q3.put(
                "org.apache.cayenne.dba.oracle.OracleAdapter",
                "UPDATE ARTIST SET ARTIST_NAME = #bind($newName) WHERE RTRIM(ARTIST_NAME) = #bind($oldName)");
        q3.put(
                "org.apache.cayenne.dba.oracle.Oracle8Adapter",
                "UPDATE ARTIST SET ARTIST_NAME = #bind($newName) WHERE RTRIM(ARTIST_NAME) = #bind($oldName)");

        map.put("SELECT * FROM ARTIST ORDER BY ARTIST_ID", q1);
        map.put("SELECT * FROM ARTIST WHERE ARTIST_ID = #bind($id)", q2);
        map.put("UPDATE ARTIST SET ARTIST_NAME = #bind($newName) "
                + "WHERE ARTIST_NAME = #bind($oldName)", q3);
    }

    public static SQLTemplateCustomizer of(DbAdapter dbAdapter) {
        return new SQLTemplateCustomizer(map, dbAdapter);
    }

    private final DbAdapter adapter;
    private final Map<String, Map<String, String>> sqlMap;

    private SQLTemplateCustomizer(Map<String, Map<String, String>> sqlMap, DbAdapter adapter) {
        this.sqlMap = sqlMap;
        this.adapter = adapter;
    }

    public void updateSQLTemplate(SQLTemplate query) {
        Map<String, String> customSQL = sqlMap.get(query.getDefaultTemplate());
        if (customSQL != null) {
            String key = adapter.unwrap().getClass().getName();
            String template = customSQL.get(key);
            if (template != null) {
                query.setTemplate(key, template);
            }
        }
    }

    public SQLTemplate createSQLTemplate(Class<?> root, String defaultTemplate) {
        SQLTemplate template = new SQLTemplate(root, defaultTemplate);
        updateSQLTemplate(template);
        return template;
    }
}
