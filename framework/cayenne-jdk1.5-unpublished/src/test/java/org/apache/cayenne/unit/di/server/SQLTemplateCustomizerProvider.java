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
package org.apache.cayenne.unit.di.server;

import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.ConfigurationException;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.unit.util.SQLTemplateCustomizer;

public class SQLTemplateCustomizerProvider implements Provider<SQLTemplateCustomizer> {

    @Inject
    private DbAdapter dbAdapter;

    public SQLTemplateCustomizer get() throws ConfigurationException {
        Map<String, Map<String, String>> map = new HashMap<String, Map<String, String>>();

        Map<String, String> q1 = new HashMap<String, String>();
        q1.put(
                "org.apache.cayenne.dba.postgres.PostgresAdapter",
                "SELECT #result('ARTIST_ID'), RTRIM(#result('ARTIST_NAME')), "
                        + "#result('DATE_OF_BIRTH') FROM ARTIST ORDER BY ARTIST_ID");
        q1.put(
                "org.apache.cayenne.dba.ingres.IngresAdapter",
                "SELECT #result('ARTIST_ID'), TRIM(#result('ARTIST_NAME')), "
                        + "#result('DATE_OF_BIRTH') FROM ARTIST ORDER BY ARTIST_ID");
        q1.put(
                "org.apache.cayenne.dba.openbase.OpenBaseAdapter",
                "SELECT #result('ARTIST_ID'), #result('ARTIST_NAME'), "
                        + "#result('DATE_OF_BIRTH') FROM ARTIST ORDER BY ARTIST_ID");

        Map<String, String> q2 = new HashMap<String, String>();
        q2.put(
                "org.apache.cayenne.dba.postgres.PostgresAdapter",
                "SELECT #result('ARTIST_ID'), RTRIM(#result('ARTIST_NAME')), #result('DATE_OF_BIRTH') "
                        + "FROM ARTIST WHERE ARTIST_ID = #bind($id)");
        q2.put(
                "org.apache.cayenne.dba.ingres.IngresAdapter",
                "SELECT #result('ARTIST_ID'), TRIM(#result('ARTIST_NAME')), #result('DATE_OF_BIRTH') "
                        + "FROM ARTIST WHERE ARTIST_ID = #bind($id)");
        q2.put(
                "org.apache.cayenne.dba.openbase.OpenBaseAdapter",
                "SELECT #result('ARTIST_ID'), #result('ARTIST_NAME'), #result('DATE_OF_BIRTH') "
                        + "FROM ARTIST WHERE ARTIST_ID = #bind($id)");

        Map<String, String> q3 = new HashMap<String, String>();
        q3
                .put(
                        "org.apache.cayenne.dba.oracle.OracleAdapter",
                        "UPDATE ARTIST SET ARTIST_NAME = #bind($newName) WHERE RTRIM(ARTIST_NAME) = #bind($oldName)");
        q3
                .put(
                        "org.apache.cayenne.dba.oracle.Oracle8Adapter",
                        "UPDATE ARTIST SET ARTIST_NAME = #bind($newName) WHERE RTRIM(ARTIST_NAME) = #bind($oldName)");

        map.put("SELECT * FROM ARTIST ORDER BY ARTIST_ID", q1);
        map.put("SELECT * FROM ARTIST WHERE ARTIST_ID = #bind($id)", q2);
        map.put("UPDATE ARTIST SET ARTIST_NAME = #bind($newName) "
                + "WHERE ARTIST_NAME = #bind($oldName)", q3);

        return new SQLTemplateCustomizer(map, dbAdapter);
    }
}
