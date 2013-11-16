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
package org.apache.cayenne.access;

import java.util.List;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.DataRow;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.query.SortOrder;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.embeddable.EmbedEntity1;
import org.apache.cayenne.testdo.embeddable.Embeddable1;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.apache.cayenne.unit.di.server.UseServerRuntime;

@UseServerRuntime(ServerCase.DEFAULT_PROJECT)
public class EmbeddingTest extends ServerCase {
    
    @Inject
    protected ObjectContext context;
    
    @Inject
    protected DBHelper dbHelper;
    
    protected TableHelper tEmbedEntity1;
    
    @Override
    protected void setUpAfterInjection() throws Exception {
        dbHelper.deleteAll("EMBED_ENTITY1");
        
        tEmbedEntity1 = new TableHelper(dbHelper, "EMBED_ENTITY1");
        tEmbedEntity1.setColumns("ID", "NAME", "EMBEDDED10", "EMBEDDED20", "EMBEDDED30", "EMBEDDED40");
    }
    
    protected void createSelectDataSet() throws Exception {
        tEmbedEntity1.delete().execute();
        tEmbedEntity1.insert(1, "n1", "e1", "e2", "e3", "e4");
        tEmbedEntity1.insert(2, "n2", "ex1", "ex2", "ex3", "ex4");
    }
    
    protected void createUpdateDataSet() throws Exception {
        tEmbedEntity1.delete().execute();
        tEmbedEntity1.insert(1, "n1", "e1", "e2", "e3", "e4");
    }

    public void testSelect() throws Exception {
        createSelectDataSet();

        SelectQuery query = new SelectQuery(EmbedEntity1.class);
        query.addOrdering(EmbedEntity1.NAME_PROPERTY, SortOrder.ASCENDING);

        List<?> results = context.performQuery(query);
        assertEquals(2, results.size());

        EmbedEntity1 o1 = (EmbedEntity1) results.get(0);

        assertEquals("n1", o1.getName());
        Embeddable1 e11 = o1.getEmbedded1();
        Embeddable1 e12 = o1.getEmbedded2();

        assertNotNull(e11);
        assertNotNull(e12);
        assertEquals("e1", e11.getEmbedded10());
        assertEquals("e2", e11.getEmbedded20());
        assertEquals("e3", e12.getEmbedded10());
        assertEquals("e4", e12.getEmbedded20());

        EmbedEntity1 o2 = (EmbedEntity1) results.get(1);

        assertEquals("n2", o2.getName());
        Embeddable1 e21 = o2.getEmbedded1();
        Embeddable1 e22 = o2.getEmbedded2();

        assertNotNull(e21);
        assertNotNull(e22);
        assertEquals("ex1", e21.getEmbedded10());
        assertEquals("ex2", e21.getEmbedded20());
        assertEquals("ex3", e22.getEmbedded10());
        assertEquals("ex4", e22.getEmbedded20());
    }

    public void testInsert() throws Exception {

        EmbedEntity1 o1 = context.newObject(EmbedEntity1.class);
        o1.setName("NAME");

        Embeddable1 e1 = new Embeddable1();

        // init before the embeddable was set on an owning object
        e1.setEmbedded10("E11");
        e1.setEmbedded20("E12");
        o1.setEmbedded1(e1);

        Embeddable1 e2 = new Embeddable1();
        o1.setEmbedded2(e2);

        // init after it was set on the owning object
        e2.setEmbedded10("E21");
        e2.setEmbedded20("E22");

        context.commitChanges();

        SelectQuery query = new SelectQuery(EmbedEntity1.class);
        query.setFetchingDataRows(true);
        DataRow row = (DataRow) Cayenne.objectForQuery(context, query);
        assertNotNull(row);
        assertEquals("E11", row.get("EMBEDDED10"));
        assertEquals("E12", row.get("EMBEDDED20"));
        assertEquals("E21", row.get("EMBEDDED30"));
        assertEquals("E22", row.get("EMBEDDED40"));
    }

    public void testUpdateEmbeddedProperties() throws Exception {
        createUpdateDataSet();

        SelectQuery query = new SelectQuery(EmbedEntity1.class);
        query.addOrdering(EmbedEntity1.NAME_PROPERTY, SortOrder.ASCENDING);

        List<?> results = context.performQuery(query);
        EmbedEntity1 o1 = (EmbedEntity1) results.get(0);

        Embeddable1 e11 = o1.getEmbedded1();
        e11.setEmbedded10("x1");

        assertEquals(PersistenceState.MODIFIED, o1.getPersistenceState());

        context.commitChanges();
        SelectQuery query1 = new SelectQuery(EmbedEntity1.class);
        query1.setFetchingDataRows(true);
        DataRow row = (DataRow) Cayenne.objectForQuery(context, query1);
        assertNotNull(row);
        assertEquals("x1", row.get("EMBEDDED10"));
    }

    public void testUpdateEmbedded() throws Exception {
        createUpdateDataSet();

        SelectQuery query = new SelectQuery(EmbedEntity1.class);
        query.addOrdering(EmbedEntity1.NAME_PROPERTY, SortOrder.ASCENDING);

        List<?> results = context.performQuery(query);
        EmbedEntity1 o1 = (EmbedEntity1) results.get(0);

        Embeddable1 e11 = new Embeddable1();
        e11.setEmbedded10("x1");
        e11.setEmbedded20("x2");
        o1.setEmbedded1(e11);

        assertEquals(PersistenceState.MODIFIED, o1.getPersistenceState());

        context.commitChanges();
        SelectQuery query1 = new SelectQuery(EmbedEntity1.class);
        query1.setFetchingDataRows(true);
        DataRow row = (DataRow) Cayenne.objectForQuery(context, query1);
        assertNotNull(row);
        assertEquals("x1", row.get("EMBEDDED10"));
    }
}
