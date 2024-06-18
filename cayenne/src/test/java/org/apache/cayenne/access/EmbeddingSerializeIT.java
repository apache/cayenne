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

package org.apache.cayenne.access;

import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.embeddable.EmbedEntity1;
import org.apache.cayenne.testdo.embeddable.Embeddable1;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.apache.cayenne.util.Util;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@UseCayenneRuntime(CayenneProjects.EMBEDDABLE_PROJECT)
public class EmbeddingSerializeIT extends RuntimeCase {

    @Inject
    protected ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    protected TableHelper tEmbedEntity1;

    @Before
    public void setUp() throws Exception {
        tEmbedEntity1 = new TableHelper(dbHelper, "EMBED_ENTITY1");
        tEmbedEntity1.setColumns("ID", "NAME", "EMBEDDED10", "EMBEDDED20", "EMBEDDED30", "EMBEDDED40");
    }

    protected void createSelectDataSet() throws Exception {
        tEmbedEntity1.delete().execute();
        tEmbedEntity1.insert(1, "n1", "e1", "e2", "e3", "e4");
    }

    @Test
    public void testSerializationEntityWithEmbeddableCommited() throws Exception {
        createSelectDataSet();

        ObjectSelect<EmbedEntity1> query = ObjectSelect.query(EmbedEntity1.class);
        List<EmbedEntity1> results = query.select(context);
        assertEquals(1, results.size());

        EmbedEntity1 o1 = results.get(0);
        assertNotNull(o1);

        EmbedEntity1 o2 = Util.cloneViaSerialization(o1);
        assertNotNull(o2);

        assertEquals(o1.getPersistenceState(), PersistenceState.COMMITTED);
        assertEquals(o2.getPersistenceState(), PersistenceState.HOLLOW);

        assertEquals("e1", o1.getEmbedded1().getEmbedded10());
        assertEquals("e3", o1.getEmbedded2().getEmbedded10());

        assertNull(o2.getEmbedded1());
        assertNull(o2.getEmbedded2());
    }

    @Test
    public void testSerializationEntityWithEmbeddableModified() throws Exception {
        createSelectDataSet();

        ObjectSelect<EmbedEntity1> query = ObjectSelect.query(EmbedEntity1.class);
        List<EmbedEntity1> results = query.select(context);
        assertEquals(1, results.size());

        EmbedEntity1 o1 = results.get(0);
        assertNotNull(o1);
        o1.setPersistenceState(PersistenceState.MODIFIED);

        EmbedEntity1 o2 = Util.cloneViaSerialization(o1);
        assertNotNull(o2);

        assertEquals(o1.getPersistenceState(), PersistenceState.MODIFIED);
        assertEquals(o2.getPersistenceState(), PersistenceState.MODIFIED);

        assertEquals("e1", o1.getEmbedded1().getEmbedded10());
        assertEquals("e1", o2.getEmbedded1().getEmbedded10());

        assertEquals("e3", o1.getEmbedded2().getEmbedded10());
        assertEquals("e3", o2.getEmbedded2().getEmbedded10());
    }

    @Test
    public void testSerializationEmbeddable() throws Exception {
        createSelectDataSet();

        ObjectSelect<EmbedEntity1> query = ObjectSelect.query(EmbedEntity1.class);
        List<EmbedEntity1> results = query.select(context);
        assertEquals(1, results.size());

        EmbedEntity1 o1 = results.get(0);
        assertNotNull(o1);

        Embeddable1 e1 = o1.getEmbedded1();

        Embeddable1 e2 = Util.cloneViaSerialization(e1);

        assertEquals(e1.getEmbedded10(), e2.getEmbedded10());
    }
}
