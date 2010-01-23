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
package org.apache.cayenne.gen;

import java.sql.SQLException;
import java.util.List;

import org.apache.art.Artist;
import org.apache.art.Testmap;
import org.apache.cayenne.test.DBHelper;
import org.apache.cayenne.test.TableHelper;
import org.apache.cayenne.unit.CayenneCase;

public class DataMapGeneratedQueryRunTest extends CayenneCase {

    protected TableHelper artistHelper;

    @Override
    protected void setUp() throws Exception {
        DBHelper dbHelper = getDbHelper();
        artistHelper = new TableHelper(dbHelper, "ARTIST", "ARTIST_ID", "ARTIST_NAME");
        artistHelper.deleteAll();
    }

    public void testPerformGeneratedQuery() throws SQLException {
        artistHelper.insert(1, "A2");
        artistHelper.insert(2, "A1");

        List<Artist> result = Testmap.getInstance().performQueryWithQualifier(
                createDataContext(),
                "A1");

        assertEquals(1, result.size());
    }
}
