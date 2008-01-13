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

import java.util.Iterator;
import java.util.List;

import org.apache.art.Painting;
import org.apache.art.PaintingInfo;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SelectQueryBase;

public class CAY_788Test extends SelectQueryBase {

    public void testPrefetchToOne() {
        DataContext ctxt = createDataContext();
        SelectQuery query = new SelectQuery(Painting.class);
        query.addPrefetch(Painting.TO_PAINTING_INFO_PROPERTY);
        List daos = ctxt.performQuery(query);
        assertTrue(!daos.isEmpty());
        for (Iterator it = daos.iterator(); it.hasNext();) {
            Painting p = (Painting) it.next();
            PaintingInfo pi = p.getToPaintingInfo();
            assertEquals("persistence state", PersistenceState.COMMITTED, p
                    .getPersistenceState());
            assertEquals("persistence state", PersistenceState.COMMITTED, pi
                    .getPersistenceState());
        }
    }

    @Override
    protected void populateTables() throws Exception {
        DataContext ctxt = createDataContext();
        for (int i = 0; i < 10; i++) {
            Painting p = ctxt.newObject(Painting.class);
            p.setPaintingTitle("Painting title #" + i);
            p.setPaintingDescription("Painting desc #" + i);
            PaintingInfo pi = ctxt.newObject(PaintingInfo.class);
            pi.setTextReview("Review #" + i);
            p.setToPaintingInfo(pi);
        }
        ctxt.commitChanges();
    }

}
