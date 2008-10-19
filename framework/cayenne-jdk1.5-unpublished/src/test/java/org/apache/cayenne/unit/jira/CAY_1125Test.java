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
import java.util.List;

import org.apache.art.Artist;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.MySQLStackAdapter;

public class CAY_1125Test extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        deleteTestData();
        DbEntity artistDB = getDbEntity("ARTIST");
        artistDB.addAttribute(new DbAttribute(
                "SMALLINT_UNSIGNED",
                Types.SMALLINT,
                artistDB));

        ObjEntity artistObj = getObjEntity("Artist");
        artistObj.addAttribute(new ObjAttribute(
                "smallintUnsigned",
                "java.lang.Integer",
                artistObj));
        getDomain().getEntityResolver().clearCache();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        getObjEntity("Artist").removeAttribute("smallintUnsigned");
        getDbEntity("ARTIST").removeAttribute("SMALLINT_UNSIGNED");
    }

    public void testSQLTemplate() {
        if (getAccessStackAdapter() instanceof MySQLStackAdapter) {

            SQLTemplate insert = new SQLTemplate(
                    Artist.class,
                    "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, SMALLINT_UNSIGNED) VALUES (1, 'A', 32768)");

            ObjectContext context = createDataContext();
            context.performGenericQuery(insert);
            
            SQLTemplate select = new SQLTemplate(Artist.class, "SELECT * FROM ARTIST");
            select.setColumnNamesCapitalization(SQLTemplate.UPPERCASE_COLUMN_NAMES);
            
            
            List<Artist> results = context.performQuery(select);
            assertEquals(1, results.size());
            assertEquals(32768, results.get(0).readProperty("smallintUnsigned"));
        }
    }
}
