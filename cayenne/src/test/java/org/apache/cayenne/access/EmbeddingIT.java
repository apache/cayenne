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

import org.apache.cayenne.DataRow;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.ResultBatchIterator;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.embeddable.EmbedChild;
import org.apache.cayenne.testdo.embeddable.EmbedEntity1;
import org.apache.cayenne.testdo.embeddable.EmbedEntity2;
import org.apache.cayenne.testdo.embeddable.EmbedRoot;
import org.apache.cayenne.testdo.embeddable.Embeddable1;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EmbeddingIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.EMBEDDABLE_PROJECT);

    
    protected TableHelper tEmbedEntity1;
    protected TableHelper tEmbedEntity2;
    protected TableHelper tEmbedRoot;
    protected TableHelper tEmbedChild;

    
    @BeforeEach
    public void setUp() throws Exception {
        tEmbedEntity1 = env.table("EMBED_ENTITY1", "ID", "NAME", "EMBEDDED10", "EMBEDDED20", "EMBEDDED30", "EMBEDDED40");

        tEmbedEntity2 = env.table("EMBED_ENTITY2", "ID", "NAME", "ENTITY1_ID", "EMBEDDED10", "EMBEDDED20");

        tEmbedRoot = env.table("EMBED_ROOT", "ID", "NAME", "EMBEDDED10", "EMBEDDED20", "TYPE");

        tEmbedChild = env.table("EMBED_CHILD", "ID", "CHILD_ATTR");
    }
    
    protected void createSelectDataSet() throws Exception {
        tEmbedEntity1.insert(1, "n1", "e1", "e2", "e3", "e4");
        tEmbedEntity1.insert(2, "n2", "ex1", "ex2", "ex3", "ex4");
    }

    protected void createSelectDataSet2() throws Exception {
        createSelectDataSet();
        tEmbedEntity2.insert(1, "n2-1", 1, "e1", "e2");
        tEmbedEntity2.insert(2, "n2-1", 2, "e1", "e2");
    }

    protected void createSelectDataSetInheritance() throws Exception {
        tEmbedRoot.insert(1, "root1", "e1-1", "e2-1", 0);
        tEmbedRoot.insert(2, "root2", "e1-2", "e2-2", 1);
        tEmbedChild.insert(2, "child-attr1");
    }

    protected void createUpdateDataSet() throws Exception {
        tEmbedEntity1.insert(1, "n1", "e1", "e2", "e3", "e4");
    }

    @Test
    public void select() throws Exception {
        createSelectDataSet();

        List<EmbedEntity1> results = ObjectSelect.query(EmbedEntity1.class).orderBy(EmbedEntity1.NAME.asc()).select(env.context());
        assertEquals(2, results.size());

        EmbedEntity1 o1 = results.get(0);

        assertEquals("n1", o1.getName());
        Embeddable1 e11 = o1.getEmbedded1();
        Embeddable1 e12 = o1.getEmbedded2();

        assertNotNull(e11);
        assertNotNull(e12);
        assertEquals("e1", e11.getEmbedded10());
        assertEquals("e2", e11.getEmbedded20());
        assertEquals("e3", e12.getEmbedded10());
        assertEquals("e4", e12.getEmbedded20());

        EmbedEntity1 o2 = results.get(1);

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

    @Test
    public void embeddablePropertiesInWhere() throws Exception {
        createSelectDataSet();

        List<EmbedEntity1> result = ObjectSelect.query(EmbedEntity1.class)
                .where(EmbedEntity1.EMBEDDED1.dot(Embeddable1.EMBEDDED10).eq("e1"))
                .orderBy(EmbedEntity1.EMBEDDED1.dot(Embeddable1.EMBEDDED10).asc())
                .select(env.context());
        assertEquals(1, result.size());
        assertEquals("e1", result.get(0).getEmbedded1().getEmbedded10());
    }

    @Test
    public void insert() {

        EmbedEntity1 o1 = env.context().newObject(EmbedEntity1.class);
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

        env.context().commitChanges();

        DataRow row = ObjectSelect.dataRowQuery(EmbedEntity1.class).selectOne(env.context());
        assertNotNull(row);
        assertEquals("E11", row.get("EMBEDDED10"));
        assertEquals("E12", row.get("EMBEDDED20"));
        assertEquals("E21", row.get("EMBEDDED30"));
        assertEquals("E22", row.get("EMBEDDED40"));
    }

    @Test
    public void updateEmbeddedProperties() throws Exception {
        createUpdateDataSet();

        List<EmbedEntity1> results = ObjectSelect.query(EmbedEntity1.class).orderBy(EmbedEntity1.NAME.asc()).select(env.context());
        EmbedEntity1 o1 = results.get(0);

        Embeddable1 e11 = o1.getEmbedded1();
        e11.setEmbedded10("x1");

        assertEquals(PersistenceState.MODIFIED, o1.getPersistenceState());

        env.context().commitChanges();

        DataRow row = ObjectSelect.dataRowQuery(EmbedEntity1.class).selectOne(env.context());
        assertNotNull(row);
        assertEquals("x1", row.get("EMBEDDED10"));
    }

    @Test
    public void updateEmbedded() throws Exception {
        createUpdateDataSet();

        List<EmbedEntity1> results = ObjectSelect.query(EmbedEntity1.class).orderBy(EmbedEntity1.NAME.asc()).select(env.context());
        EmbedEntity1 o1 = results.get(0);

        Embeddable1 e11 = new Embeddable1();
        e11.setEmbedded10("x1");
        e11.setEmbedded20("x2");
        o1.setEmbedded1(e11);

        assertEquals(PersistenceState.MODIFIED, o1.getPersistenceState());

        env.context().commitChanges();

        DataRow row = ObjectSelect.dataRowQuery(EmbedEntity1.class).selectOne(env.context());
        assertNotNull(row);
        assertEquals("x1", row.get("EMBEDDED10"));
    }

    @Test
    public void propertyExpression() throws Exception {
        createSelectDataSet();

        List<EmbedEntity1> result = ObjectSelect.query(EmbedEntity1.class)
                .where(EmbedEntity1.EMBEDDED1.dot(Embeddable1.EMBEDDED10).eq("e1"))
                .orderBy(EmbedEntity1.EMBEDDED2.dot(Embeddable1.EMBEDDED20).desc())
                .select(env.context());

        assertEquals(1, result.size());
    }

    @Test
    public void relatedEmbedded() throws Exception {
        createSelectDataSet2();

        List<EmbedEntity2> result = ObjectSelect.query(EmbedEntity2.class)
                .where(EmbedEntity2.ENTITY1.dot(EmbedEntity1.EMBEDDED1).dot(Embeddable1.EMBEDDED10).eq("e1"))
                .orderBy(EmbedEntity2.ENTITY1.dot(EmbedEntity1.EMBEDDED2).dot(Embeddable1.EMBEDDED20).desc())
                .select(env.context());

        assertEquals(1, result.size());
    }

    @Test
    public void prefetchWithEmbedded() throws Exception {
        createSelectDataSet2();

        List<EmbedEntity2> result = ObjectSelect.query(EmbedEntity2.class)
                .prefetch(EmbedEntity2.ENTITY1.joint())
                .select(env.context());

        assertEquals(2, result.size());
        assertNotNull(result.get(0).getEntity1().getEmbedded1());
        assertNotNull(result.get(1).getEntity1().getEmbedded1());
    }

    @Test
    public void inMemoryFilteringByEmbeddable() throws Exception {
        createSelectDataSet();

        List<EmbedEntity1> result = ObjectSelect.query(EmbedEntity1.class).select(env.context());
        assertEquals(2, result.size());

        List<EmbedEntity1> filtered = EmbedEntity1.EMBEDDED1.dot(Embeddable1.EMBEDDED10).eq("e1").filterObjects(result);
        assertEquals(1, filtered.size());
        assertEquals("n1", filtered.get(0).getName());
    }

    @Test
    public void columnSelect() throws Exception {
        createSelectDataSet2();

        List<Embeddable1> result = ObjectSelect.columnQuery(EmbedEntity1.class, EmbedEntity1.EMBEDDED2)
                .orderBy(EmbedEntity1.EMBEDDED2.dot(Embeddable1.EMBEDDED10).asc())
                .select(env.context());
        assertEquals(2, result.size());
        assertEquals("e3", result.get(0).getEmbedded10());
        assertEquals("e4", result.get(0).getEmbedded20());
        assertEquals("ex3", result.get(1).getEmbedded10());
        assertEquals("ex4", result.get(1).getEmbedded20());

        result.get(0).setEmbedded10("test");
        env.context().commitChanges();
     }

    @Test
    public void columnSelectMultiple() throws Exception {
        createSelectDataSet2();

        List<Object[]> result = ObjectSelect.columnQuery(EmbedEntity1.class, EmbedEntity1.EMBEDDED1, EmbedEntity1.EMBEDDED2)
                .orderBy(EmbedEntity1.EMBEDDED2.dot(Embeddable1.EMBEDDED10).asc())
                .select(env.context());
        assertEquals(2, result.size());
        assertEquals("e3", ((Embeddable1)result.get(0)[1]).getEmbedded10());
        assertEquals("e4", ((Embeddable1)result.get(0)[1]).getEmbedded20());
        assertEquals("ex3", ((Embeddable1)result.get(1)[1]).getEmbedded10());
        assertEquals("ex4", ((Embeddable1)result.get(1)[1]).getEmbedded20());
    }

    @Test
    public void columnSelectMixed() throws Exception {
        createSelectDataSet2();

        List<Object[]> result = ObjectSelect.columnQuery(EmbedEntity1.class, EmbedEntity1.EMBEDDED1.dot(Embeddable1.EMBEDDED10), EmbedEntity1.EMBEDDED2)
                .orderBy(EmbedEntity1.EMBEDDED2.dot(Embeddable1.EMBEDDED10).asc())
                .select(env.context());
        assertEquals(2, result.size());
        assertEquals("e3", ((Embeddable1)result.get(0)[1]).getEmbedded10());
        assertEquals("e4", ((Embeddable1)result.get(0)[1]).getEmbedded20());
        assertEquals("ex3", ((Embeddable1)result.get(1)[1]).getEmbedded10());
        assertEquals("ex4", ((Embeddable1)result.get(1)[1]).getEmbedded20());
    }

    @Test
    public void where() throws Exception {
        createSelectDataSet2();

        Embeddable1 embeddable1 = new Embeddable1();
        embeddable1.setEmbedded10("e1");
        embeddable1.setEmbedded20("e2");

        List<EmbedEntity1> result = ObjectSelect.query(EmbedEntity1.class)
                .where(EmbedEntity1.EMBEDDED1.eq(embeddable1))
                .select(env.context());
        assertEquals(1, result.size());
    }

    @Test
    public void selectWithInheritance() throws Exception {
        createSelectDataSetInheritance();

        List<EmbedRoot> roots = ObjectSelect.query(EmbedRoot.class)
                .orderBy(EmbedRoot.NAME.asc())
                .select(env.context());

        assertEquals(2, roots.size());

        EmbedRoot root = roots.get(0);
        EmbedRoot child = roots.get(1);
        assertTrue(child instanceof EmbedChild);

        assertEquals("e1-1", root.getEmbedded().getEmbedded10());
        assertEquals("e2-1", root.getEmbedded().getEmbedded20());

        assertEquals("e1-2", child.getEmbedded().getEmbedded10());
        assertEquals("e2-2", child.getEmbedded().getEmbedded20());
        assertEquals("child-attr1", ((EmbedChild) child).getChildAttr());
    }

    @Test
    public void insertWithInheritance() {
        {
            EmbedRoot root = env.context().newObject(EmbedRoot.class);
            root.setName("root");
            Embeddable1 embeddable1 = new Embeddable1();
            embeddable1.setEmbedded10("root-10");
            embeddable1.setEmbedded20("root-20");
            root.setEmbedded(embeddable1);
        }

        {
            EmbedChild child = env.context().newObject(EmbedChild.class);
            child.setName("child");
            Embeddable1 embeddable1 = new Embeddable1();
            embeddable1.setEmbedded10("child-10");
            embeddable1.setEmbedded20("child-20");
            child.setEmbedded(embeddable1);
        }

        env.context().commitChanges();
    }

    @Test
    public void queryWithBatchIterator() throws Exception {
        createSelectDataSet2();
        try (ResultBatchIterator<EmbedEntity1> iterator = ObjectSelect.query(EmbedEntity1.class)
                .batchIterator(env.context(), 2)) {
            assertNotNull(iterator.next().get(0).getEmbedded2());
        }

    }
}
