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
package org.apache.cayenne.query;

import org.apache.art.Artist;
import org.apache.cayenne.unit.CayenneCase;

public class QueryChainTest extends CayenneCase {

    public void testSelectQuery() {

        QueryChain chain = new QueryChain();
        chain.addQuery(new SelectQuery(Artist.class));
        chain.addQuery(new SelectQuery(Artist.class));

        QueryMetadata md = chain.getMetaData(getDomain().getEntityResolver());

        assertNotNull(md);
        assertTrue(md.isFetchingDataRows());
        assertNull(md.getObjEntity());
    }

    public void testSelectQueryDataRows() {

        QueryChain chain = new QueryChain();
        SelectQuery q1 = new SelectQuery(Artist.class);
        q1.setFetchingDataRows(true);
        chain.addQuery(q1);
        
        SelectQuery q2 = new SelectQuery(Artist.class);
        q2.setFetchingDataRows(true);
        chain.addQuery(q2);

        QueryMetadata md = chain.getMetaData(getDomain().getEntityResolver());

        assertNotNull(md);
        assertTrue(md.isFetchingDataRows());
        assertNull(md.getObjEntity());
    }
}
