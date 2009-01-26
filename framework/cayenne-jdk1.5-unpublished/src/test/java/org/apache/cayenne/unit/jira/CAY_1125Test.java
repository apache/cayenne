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
import org.apache.cayenne.query.CapsStrategy;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.MySQLStackAdapter;

public class CAY_1125Test extends CayenneCase {

    private boolean isMySQL() {
        return getAccessStackAdapter() instanceof MySQLStackAdapter;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        if (isMySQL()) {
            deleteTestData();

            createDataContext()
                    .performGenericQuery(
                            new SQLTemplate(
                                    Artist.class,
                                    "alter table ARTIST ADD COLUMN SMALLINT_UNSIGNED SMALLINT UNSIGNED NULL"));

            DbEntity artistDB = getDbEntity("ARTIST");
            artistDB.addAttribute(new DbAttribute(
                    "SMALLINT_UNSIGNED",
                    Types.SMALLINT,
                    artistDB));

            ObjEntity artistObj = getObjEntity("Artist");
            ObjAttribute artistObjAttr = new ObjAttribute(
                    "smallintUnsigned",
                    "java.lang.Integer",
                    artistObj);
            artistObjAttr.setDbAttributePath("SMALLINT_UNSIGNED");
            artistObj.addAttribute(artistObjAttr);
            getDomain().getEntityResolver().clearCache();
            getDomain().getEntityResolver().getClassDescriptorMap().clearDescriptors();
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        if (isMySQL()) {

            getObjEntity("Artist").removeAttribute("smallintUnsigned");
            getDbEntity("ARTIST").removeAttribute("SMALLINT_UNSIGNED");

            getDomain().getEntityResolver().clearCache();
            getDomain().getEntityResolver().getClassDescriptorMap().clearDescriptors();
        }
    }

    public void testSQLTemplate() {
        if (isMySQL()) {

            SQLTemplate insert = new SQLTemplate(
                    Artist.class,
                    "INSERT INTO ARTIST (ARTIST_ID, ARTIST_NAME, SMALLINT_UNSIGNED) VALUES (1, 'A', 33000)");

            ObjectContext context = createDataContext();
            context.performGenericQuery(insert);

            SQLTemplate select = new SQLTemplate(Artist.class, "SELECT * FROM ARTIST");
            select.setColumnNamesCapitalization(CapsStrategy.UPPER);

            List<Artist> results = context.performQuery(select);
            assertEquals(1, results.size());
            assertEquals(33000, results.get(0).readProperty("smallintUnsigned"));
        }
    }
}
