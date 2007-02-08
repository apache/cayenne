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

package org.apache.cayenne.access.trans;

import java.util.HashMap;
import java.util.Map;

import org.apache.art.MeaningfulPKTest1;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.unit.CayenneTestCase;

/**
 * @deprecated Since 1.2 InsertQuery is gone.
 * @author Andrei Adamchik
 */
public class InsertTranslatorTst extends CayenneTestCase {

    public void testMeaningfulPrimaryKey() throws Exception {
        Map id = new HashMap();
        Map object = new HashMap();

        id.put("ARTIST_ID", new Integer(3000));

        object.putAll(id);
        object.put("ARTIST_NAME", "aaaaa");
        object.put("DATE_OF_BIRTH", new java.util.Date());

        org.apache.cayenne.query.InsertQuery q = new org.apache.cayenne.query.InsertQuery(
                MeaningfulPKTest1.class);
        q.setObjectSnapshot(object);
        q.setObjectId(new ObjectId("MeaningfulPKTest1", id));

        InsertTranslator transl = new InsertTranslator();
        transl.setEntityResolver(getNode().getEntityResolver());
        transl.setQuery(q);
        transl.prepareLists();

        assertNotNull(transl.columnList);
        assertEquals(3, transl.columnList.size());
    }
}
