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

package org.apache.cayenne.access.flush;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.DeferredValue;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.resolved.BatchRow;
import org.apache.cayenne.query.resolved.DeleteBatch;
import org.apache.cayenne.query.resolved.InsertBatch;
import org.apache.cayenne.query.resolved.ResolvedBatch;
import org.apache.cayenne.testdo.sorting.SortA;
import org.apache.cayenne.testdo.sorting.SortB;
import org.apache.cayenne.testdo.sorting.SortC;
import org.apache.cayenne.testdo.sorting.SortD;
import org.apache.cayenne.testdo.sorting.SortGenTree;
import org.apache.cayenne.testdo.sorting.SortMaster;
import org.apache.cayenne.testdo.sorting.SortMasterInfo;
import org.apache.cayenne.testdo.sorting.SortPk;
import org.apache.cayenne.testdo.sorting.SortPkChild;
import org.apache.cayenne.testdo.sorting.SortTree;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The DML statements a commit sends to the DB: which rows are inserted, updated and deleted, the columns written, and
 * the order. Unlike the ordering cases of {@link org.apache.cayenne.access.flush.operation.DefaultDbRowOpPlannerIT},
 * these need the right set of statements, not just the right order of the ones produced from the object changes, e.g.
 * an insert with a null FK followed by an update setting it.
 *
 * <p>The statements are recorded as the DB node receives them, one per row. A case checks all of them, and the order
 * of those that depend on each other, while the independent ones may come in any order.
 *
 * <p>The cases not supported yet, marked with TODO, check the incorrect statements produced now with
 * {@link #assertIncorrectDml(List[])}, so that any change of them is noticed.
 *
 * <pre>
 * Rows referencing each other: insert, with Cayenne-generated and DB-generated PKs, with no nullable FK (sent as
 *   is); delete
 * A row referencing itself, with a Cayenne-generated and a DB-generated PK
 * Meaningful PK: change of a referenced row, a child moved to it, swap (not supported)
 * Phantom changes: no statements
 * </pre>
 */
public class FlushDmlIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.SORTING_PROJECT);

    private ObjectContext context;

    @BeforeEach
    public void setUp() {
        context = env.context();
    }

    @AfterEach
    public void cleanUp() throws SQLException {
        // break the reference cycles first, as the tables are cleaned one at a time
        env.table("SORT_TREE", "PARENT_ID").update().set("PARENT_ID", null, Types.INTEGER).execute();
        env.table("SORT_GEN_TREE", "PARENT_ID").update().set("PARENT_ID", null, Types.INTEGER).execute();
        env.table("SORT_A", "B_ID").update().set("B_ID", null, Types.INTEGER).execute();
        env.table("SORT_B", "A_ID").update().set("A_ID", null, Types.INTEGER).execute();
        for (String table : List.of("SORT_MASTER_INFO", "SORT_MASTER", "SORT_TREE", "SORT_GEN_TREE", "SORT_A",
                "SORT_B", "SORT_PK_CHILD", "SORT_PK")) {
            env.table(table).deleteAll();
        }
    }

    /**
     * Rows referencing each other, so that no order of the inserts or the deletes works.
     */
    @Nested
    public class RowCycle {

        @Test
        public void insert() {
            SortA a = a("a", null);
            SortB b = b("b", a);
            a.setB(b);

            // the row of the table first in the batch order is inserted without the FK
            assertDml(List.of(
                    Dml.insert(a).setNull("B_ID"),
                    Dml.insert(b).set("A_ID"),
                    Dml.update(a).set("B_ID")));
        }

        @Test
        public void insertSameTable() {
            SortTree t1 = tree("t1", null);
            SortTree t2 = tree("t2", t1);
            t1.setParent(t2);

            // the rows are alike, so either can be inserted without the FK
            assertDmlAnyOf(
                    List.of(
                            Dml.insert(t1).setNull("PARENT_ID"),
                            Dml.insert(t2).set("PARENT_ID"),
                            Dml.update(t1).set("PARENT_ID")),
                    List.of(
                            Dml.insert(t2).setNull("PARENT_ID"),
                            Dml.insert(t1).set("PARENT_ID"),
                            Dml.update(t2).set("PARENT_ID")));
        }

        @Test
        public void insertGeneratedPks() {
            SortGenTree t1 = genTree("t1");
            SortGenTree t2 = genTree("t2");
            t1.setParent(t2);
            t2.setParent(t1);

            // the update is qualified by the PK generated by the insert of its row
            assertDmlAnyOf(
                    List.of(
                            Dml.insert(t1).setNull("PARENT_ID"),
                            Dml.insert(t2).set("PARENT_ID"),
                            Dml.update(t1).set("PARENT_ID")),
                    List.of(
                            Dml.insert(t2).setNull("PARENT_ID"),
                            Dml.insert(t1).set("PARENT_ID"),
                            Dml.update(t2).set("PARENT_ID")));

            ObjectContext fresh = env.runtime().newContext();
            SortGenTree t1Fetched = ObjectSelect.query(SortGenTree.class)
                    .where(SortGenTree.NAME.eq("t1"))
                    .selectOne(fresh);
            assertEquals("t2", t1Fetched.getParent().getName());
            assertEquals("t1", t1Fetched.getParent().getParent().getName());
        }

        @Test
        public void insertMandatoryFks() {
            SortC c = context.newObject(SortC.class);
            c.setName("c");
            SortD d = context.newObject(SortD.class);
            d.setName("d");
            c.setD(d);
            d.setC(c);

            // neither FK can be NULL, so no insert can be split, and the inserts are sent in the table order, for the
            // DB to accept them (e.g. with deferred FK constraints) or reject them
            assertDmlSent(List.of(Dml.insert(c).set("D_ID"), Dml.insert(d).set("C_ID")));
        }

        @Test
        public void delete() {
            SortA a = a("a", null);
            SortB b = b("b", a);
            a.setB(b);
            context.commitChanges();

            context.deleteObjects(a, b);

            // the row of the table first in the table order gets its FK set to NULL, and the deletes keep their order
            assertDml(List.of(
                    Dml.update(a).setNull("B_ID"),
                    Dml.delete(b),
                    Dml.delete(a)));
        }

        @Test
        public void deleteSameTable() {
            SortTree t1 = tree("t1", null);
            SortTree t2 = tree("t2", t1);
            t1.setParent(t2);
            context.commitChanges();

            context.deleteObjects(t1, t2);

            // the rows are alike, so either can get its FK set to NULL
            assertDmlAnyOf(
                    List.of(Dml.update(t1).setNull("PARENT_ID"), Dml.delete(t2), Dml.delete(t1)),
                    List.of(Dml.update(t2).setNull("PARENT_ID"), Dml.delete(t1), Dml.delete(t2)));
        }

        @Test
        public void selfReference() {
            SortTree t = tree("t", null);
            t.setParent(t);

            // the PK is known before the insert, so the insert sets the FK to it
            assertDml(List.of(Dml.insert(t).set("PARENT_ID")));
        }

        @Test
        public void selfReferenceGeneratedPk() {
            // e.g. the root message of a thread, referencing itself as the thread root
            SortGenTree t = genTree("t");
            t.setParent(t);

            // the PK is generated by the insert, so the insert can't set the FK to it, and an update sets it after
            assertDml(List.of(Dml.insert(t).setNull("PARENT_ID"), Dml.update(t).set("PARENT_ID")));

            SortGenTree fetched = ObjectSelect.query(SortGenTree.class)
                    .where(SortGenTree.NAME.eq("t"))
                    .selectOne(env.runtime().newContext());
            assertEquals("t", fetched.getParent().getName());
            assertEquals(Cayenne.intPKForObject(fetched), Cayenne.intPKForObject(fetched.getParent()));
        }
    }

    /**
     * A change of the meaningful PK of a row referenced by the other rows. Changing the PK alone works only with the
     * FKs cascading the update, which the model doesn't know of. The referencing rows need to follow the PK.
     */
    @Nested
    public class MeaningfulPk {

        @Test
        public void changeReferencedPk() {
            SortPk p = pk(1, "p");
            SortPkChild c = pkChild("c", p);
            context.commitChanges();

            p.setPk(2);

            // TODO: the child still references the old PK. Should be an insert of the row with the new PK, an update
            //  of the child to reference it, and a delete of the row with the old PK
            assertIncorrectDml(List.of(Dml.update(p).set("PK", 2)));
        }

        @Test
        public void moveChildToChangedPk() {
            SortPk p = pk(1, "p");
            SortPkChild c = pkChild("c", null);
            context.commitChanges();

            c.setParent(p);
            p.setPk(2);

            // TODO: the child FK is set to the old PK of the parent, taken from its ObjectId when the change is
            //  recorded. Should be the new PK, and with the parent referenced, the PK change done as in
            //  "changeReferencedPk"
            assertIncorrectDml(List.of(Dml.update(c).set("PARENT_PK", 1), Dml.update(p).set("PK", 2)));
        }

        @Test
        public void swapPks() {
            SortPk p1 = pk(1, "p1");
            SortPk p2 = pk(2, "p2");
            context.commitChanges();

            p1.setPk(2);
            p2.setPk(1);

            // TODO: neither PK can be changed first. With no rows referencing them, should be the deletes of both,
            //  then the inserts of both
            assertIncorrectDml(List.of(Dml.update(p1).set("PK", 2)), List.of(Dml.update(p2).set("PK", 1)));
        }
    }

    @Nested
    public class Phantom {

        /**
         * CAY-2723: a dependent object related, unrelated and deleted before the commit leaves no update of its PK.
         */
        @Test
        public void dependentPkUpdate() {
            SortMaster m = context.newObject(SortMaster.class);
            m.setName("m");

            SortMasterInfo info = context.newObject(SortMasterInfo.class);
            info.setNote("info");

            m.setInfo(info);
            m.setInfo(null);
            context.deleteObject(info);

            assertDml(List.of(Dml.insert(m)));
        }
    }

    private SortTree tree(String name, SortTree parent) {
        SortTree t = context.newObject(SortTree.class);
        t.setName(name);
        t.setParent(parent);
        return t;
    }

    private SortGenTree genTree(String name) {
        SortGenTree t = context.newObject(SortGenTree.class);
        t.setName(name);
        return t;
    }

    private SortA a(String name, SortB b) {
        SortA a = context.newObject(SortA.class);
        a.setName(name);
        a.setB(b);
        return a;
    }

    private SortB b(String name, SortA a) {
        SortB b = context.newObject(SortB.class);
        b.setName(name);
        b.setA(a);
        return b;
    }

    private SortPk pk(int pk, String name) {
        SortPk p = context.newObject(SortPk.class);
        p.setPk(pk);
        p.setName(name);
        return p;
    }

    private SortPkChild pkChild(String name, SortPk parent) {
        SortPkChild c = context.newObject(SortPkChild.class);
        c.setName(name);
        c.setParent(parent);
        return c;
    }

    /**
     * Commits the changes, and checks that the statements of the lists are all the statements sent to the DB, and
     * that the statements of each list come in the list order, while the lists may interleave.
     */
    @SafeVarargs
    private void assertDml(List<Dml>... lists) {
        assertStatements(commit(), lists);
    }

    /**
     * Commits the changes, and checks the statements as {@link #assertDml(List[])} does with a single list, for the
     * cases with more than one correct list of statements: passes if any of the lists matches.
     */
    @SafeVarargs
    private void assertDmlAnyOf(List<Dml>... alternatives) {
        List<Statement> statements = commit();
        List<String> mismatches = new ArrayList<>();
        for (List<Dml> alternative : alternatives) {
            try {
                assertStatements(statements, alternative);
                return;
            } catch (AssertionError e) {
                mismatches.add(e.getMessage());
            }
        }
        fail("None of the expected statements matched: " + mismatches);
    }

    private List<Statement> commit() {
        return env.runWithBatchRecorder(() -> context.commitChanges())
                .stream()
                .flatMap(b -> Statement.of(b).stream())
                .toList();
    }

    /**
     * Commits the changes, and checks the statements sent to the DB as {@link #assertDml(List[])} does, for the cases
     * whose commit result depends on the DB: with the FK constraints checked immediately, the DB rejects the commit,
     * while without them, or with the deferred or cascading ones, the DB may accept it. The statements sent after a
     * rejected one are recorded too.
     */
    @SafeVarargs
    private void assertDmlSent(List<Dml>... lists) {
        List<Statement> statements = env.runWithBatchRecorder(() -> {
                    try {
                        context.commitChanges();
                    } catch (CayenneRuntimeException e) {
                        // the DB rejected the statements
                    }
                })
                .stream()
                .flatMap(b -> Statement.of(b).stream())
                .toList();
        assertStatements(statements, lists);
    }

    /**
     * Commits the changes of a case not supported yet, and checks the statements produced now, as
     * {@link #assertDmlSent(List[])} does. Not the correct statements, but the ones to track: a change of them fails
     * the case, that can then check the correct ones with {@link #assertDml(List[])}.
     */
    @SafeVarargs
    private void assertIncorrectDml(List<Dml>... lists) {
        assertDmlSent(lists);
    }

    @SafeVarargs
    private static void assertStatements(List<Statement> statements, List<Dml>... lists) {
        int expectedCount = Arrays.stream(lists).mapToInt(List::size).sum();
        assertEquals(expectedCount, statements.size(), () -> "Unexpected statements: " + statements);

        for (List<Dml> list : lists) {
            int previous = -1;
            for (Dml dml : list) {
                int i = dml.indexIn(statements);
                if (i < previous) {
                    fail(list + " expected, got " + statements);
                }
                previous = i;
            }
        }
    }

    private enum Kind {
        INSERT, UPDATE, DELETE
    }

    /**
     * A statement sent to the DB for one row, with the columns it writes, or for a delete, the columns of its
     * qualifier.
     */
    private record Statement(Kind kind, String table, ObjectId id, Map<String, Object> columns) {

        static List<Statement> of(ResolvedBatch batch) {
            Kind kind = switch (batch) {
                case InsertBatch ignored -> Kind.INSERT;
                case DeleteBatch ignored -> Kind.DELETE;
                default -> Kind.UPDATE;
            };

            List<Statement> statements = new ArrayList<>(batch.rows().size());
            for (BatchRow row : batch.rows()) {
                Map<String, Object> columns = kind == Kind.DELETE ? row.qualifier() : row.values();
                statements.add(new Statement(kind, batch.table().name(), row.objectId(), columns));
            }
            return statements;
        }

        @Override
        public String toString() {
            Map<String, Object> resolved = new HashMap<>();
            columns.forEach((k, v) -> resolved.put(k, DeferredValue.resolve(v)));
            return kind + " " + table + " " + resolved;
        }
    }

    /**
     * An expected statement for the row of an object, and the columns it writes, if checked.
     */
    private record Dml(Kind kind, String table, ObjectId id, Map<String, Object> idSnapshot,
                       Map<String, Object> values) {

        // the value of a column written with any non-null value
        private static final Object ANY = new Object();

        // the value of a column written as null, or not written by an insert
        private static final Object NULL = new Object();

        static Dml insert(Persistent object) {
            return of(Kind.INSERT, object);
        }

        static Dml update(Persistent object) {
            return of(Kind.UPDATE, object);
        }

        static Dml delete(Persistent object) {
            return of(Kind.DELETE, object);
        }

        // the id is captured before the commit, as a new object gets another id after it
        private static Dml of(Kind kind, Persistent object) {
            ObjectId id = object.getObjectId();
            String table = env.mapping().entity(id.getEntityName()).table().name();
            return new Dml(kind, table, id, new HashMap<>(id.getIdSnapshot()), new HashMap<>());
        }

        /**
         * The statement writes the column with a non-null value.
         */
        Dml set(String column) {
            values.put(column, ANY);
            return this;
        }

        /**
         * The statement writes the column as null, or for an insert, doesn't write it.
         */
        Dml setNull(String column) {
            values.put(column, NULL);
            return this;
        }

        /**
         * The statement writes the column with the value.
         */
        Dml set(String column, Object value) {
            values.put(column, value);
            return this;
        }

        int indexIn(List<Statement> statements) {
            int found = -1;
            for (int i = 0; i < statements.size(); i++) {
                if (isRowOf(statements.get(i))) {
                    assertEquals(-1, found, () -> "More than one " + this + " in " + statements);
                    found = i;
                }
            }
            assertTrue(found >= 0, () -> "No " + this + " in " + statements);
            assertValues(statements.get(found));
            return found;
        }

        // a delete has no id, only the PK values in its qualifier
        private boolean isRowOf(Statement statement) {
            if (statement.kind() != kind || !statement.table().equals(table)) {
                return false;
            }

            return kind == Kind.DELETE
                    ? statement.columns().entrySet().containsAll(idSnapshot.entrySet())
                    : id.equals(statement.id());
        }

        private void assertValues(Statement statement) {
            values.forEach((column, expected) -> {
                if (expected == NULL) {
                    assertTrue(kind == Kind.INSERT || statement.columns().containsKey(column),
                            () -> "No " + column + " written by " + statement);
                    assertTrue(statement.columns().get(column) == null,
                            () -> "Not null " + column + " written by " + statement);
                    return;
                }

                assertTrue(statement.columns().containsKey(column),
                        () -> "No " + column + " written by " + statement);
                Object actual = DeferredValue.resolve(statement.columns().get(column));
                if (expected == ANY) {
                    assertTrue(actual != null || statement.columns().get(column) != null,
                            () -> "Null " + column + " written by " + statement);
                } else {
                    assertTrue(Objects.equals(expected, actual),
                            () -> column + "=" + expected + " expected, got " + statement);
                }
            });
        }

        @Override
        public String toString() {
            return kind + " " + table + " " + idSnapshot + (values.isEmpty() ? "" : " " + values.keySet());
        }
    }
}
