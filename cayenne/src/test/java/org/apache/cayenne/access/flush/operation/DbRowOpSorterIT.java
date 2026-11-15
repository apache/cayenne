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

package org.apache.cayenne.access.flush.operation;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.testdo.sorting.SortA;
import org.apache.cayenne.testdo.sorting.SortB;
import org.apache.cayenne.testdo.sorting.SortChild;
import org.apache.cayenne.testdo.sorting.SortFlatChild;
import org.apache.cayenne.testdo.sorting.SortMaster;
import org.apache.cayenne.testdo.sorting.SortPk;
import org.apache.cayenne.testdo.sorting.SortPkChild;
import org.apache.cayenne.testdo.sorting.SortTree;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The cases of ordering the operations of a commit by {@link GraphDbRowOpSorter}. Each case states the order of the
 * operations it requires, and passes if the DB accepts the commit, and the order holds. To catch the orders that are
 * right by chance, the sorter sorts several permutations of its input, and the order is checked in each of them. The
 * cases not supported are disabled.
 *
 * <pre>
 * FK to another table: insert, delete, move, nullify
 * FK to the same table: insert, delete, nullify, re-parent, mixed with the rows of other tables
 * Tables referencing each other: insert, delete
 * Rows referencing each other (not supported)
 * Meaningful PK: delete and insert, change
 * Flattened relationship join rows: insert, delete
 * Flattened attribute of a related row: insert
 * </pre>
 */
public class DbRowOpSorterIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv
            .forProject(CayenneProjects.SORTING_PROJECT)
            .withExtraModules(b -> b.bind(DbRowOpSorter.class).to(PermutingSorter.class));

    private ObjectContext context;
    private PermutingSorter sorter;

    @BeforeEach
    public void setUp() {
        context = env.context();
        sorter = (PermutingSorter) env.runtime().getInjector().getInstance(DbRowOpSorter.class);
    }

    @AfterEach
    public void cleanUp() throws SQLException {
        // break the reference cycles first, as the tables are cleaned one at a time
        env.table("SORT_TREE", "PARENT_ID").update().set("PARENT_ID", null, Types.INTEGER).execute();
        env.table("SORT_A", "B_ID").update().set("B_ID", null, Types.INTEGER).execute();
        env.table("SORT_B", "A_ID").update().set("A_ID", null, Types.INTEGER).execute();
        for (String table : List.of("SORT_LINK", "SORT_TAG", "SORT_CHILD", "SORT_MASTER", "SORT_TREE", "SORT_A",
                "SORT_B", "SORT_PK_CHILD", "SORT_PK")) {
            env.table(table).deleteAll();
        }
    }

    @Nested
    public class ForeignKey {

        @Test
        public void insert() {
            SortMaster m = master("m");
            SortChild c = child("c", m);

            assertOrder(insertOf(m), insertOf(c));
        }

        @Test
        public void delete() {
            SortMaster m = master("m");
            SortChild c = child("c", m);
            context.commitChanges();

            context.deleteObjects(m, c);

            assertOrder(deleteOf(c), deleteOf(m));
        }

        @Test
        public void moveToNewMaster() {
            SortMaster m1 = master("m1");
            SortChild c = child("c", m1);
            context.commitChanges();

            SortMaster m2 = master("m2");
            c.setMaster(m2);

            assertOrder(insertOf(m2), updateOf(c));
        }

        @Test
        public void moveFromDeletedMaster() {
            SortMaster m1 = master("m1");
            SortMaster m2 = master("m2");
            SortChild c = child("c", m1);
            context.commitChanges();

            c.setMaster(m2);
            context.deleteObject(m1);

            assertOrder(updateOf(c), deleteOf(m1));
        }

        @Test
        public void nullifyOnDelete() {
            SortMaster m = master("m");
            SortChild c = child("c", m);
            context.commitChanges();

            // the "nullify" delete rule sets the child FK to null
            context.deleteObject(m);

            assertOrder(updateOf(c), deleteOf(m));
        }
    }

    @Nested
    public class Reflexive {

        @Test
        public void insert() {
            SortTree root = tree("root", null);
            SortTree child = tree("child", root);
            SortTree grandChild = tree("grandChild", child);

            assertOrder(insertOf(root), insertOf(child), insertOf(grandChild));
        }

        @Test
        public void insertWithOtherTables() {
            // SORT_MASTER is at the same dependency level as SORT_TREE, and its rows mix with the rows of SORT_TREE
            // in the commit
            master("m1");
            SortTree root = tree("root", null);
            master("m2");
            SortTree child = tree("child", root);
            master("m3");
            SortTree grandChild = tree("grandChild", child);

            assertOrder(insertOf(root), insertOf(child), insertOf(grandChild));
        }

        @Test
        public void delete() {
            SortTree root = tree("root", null);
            SortTree child = tree("child", root);
            SortTree grandChild = tree("grandChild", child);
            context.commitChanges();

            context.deleteObjects(root, child, grandChild);

            assertOrder(deleteOf(grandChild), deleteOf(child), deleteOf(root));
        }

        @Test
        public void nullifyOnDelete() {
            SortTree parent = tree("parent", null);
            SortTree child = tree("child", parent);
            context.commitChanges();

            // the "nullify" delete rule sets the child FK to null, an update and a delete in the same table
            context.deleteObject(parent);

            assertOrder(updateOf(child), deleteOf(parent));
        }

        @Test
        public void reparent() {
            SortTree p1 = tree("p1", null);
            SortTree child = tree("child", p1);
            context.commitChanges();

            SortTree p2 = tree("p2", null);
            child.setParent(p2);
            context.deleteObject(p1);

            assertOrder(insertOf(p2), updateOf(child), deleteOf(p1));
        }
    }

    /**
     * SORT_A and SORT_B reference each other, but the rows don't. One pair of rows references in each direction, so no
     * order of the tables works, only an order of the rows.
     */
    @Nested
    public class TableCycle {

        @Test
        public void insert() {
            SortA a1 = a("a1", null);
            SortB b1 = b("b1", a1);
            SortB b2 = b("b2", null);
            SortA a2 = a("a2", b2);

            assertOrder(chain(insertOf(a1), insertOf(b1)), chain(insertOf(b2), insertOf(a2)));
        }

        @Test
        public void delete() {
            // inserted in two commits, as the first commit has no references, and the second only references
            // committed rows, any sorter can insert them
            SortA a1 = a("a1", null);
            SortB b2 = b("b2", null);
            context.commitChanges();
            SortB b1 = b("b1", a1);
            SortA a2 = a("a2", b2);
            context.commitChanges();

            context.deleteObjects(a1, b1, a2, b2);

            assertOrder(chain(deleteOf(b1), deleteOf(a1)), chain(deleteOf(a2), deleteOf(b2)));
        }

        @Test
        @Disabled("""
                Not supported: no order of the inserts works, the cycle could only be broken by an \
                insert with a null FK and an update""")
        public void rowCycle() {
            SortA a = a("a", null);
            SortB b = b("b", a);
            a.setB(b);

            context.commitChanges();
        }
    }

    @Nested
    public class MeaningfulPk {

        @Test
        public void deleteAndInsertSamePk() {
            SortPk p1 = pk(1, "p1");
            context.commitChanges();

            context.deleteObject(p1);
            SortPk p2 = pk(1, "p2");

            assertOrder(deleteOf(p1), insertOf(p2));
        }

        @Test
        public void deleteAndInsertSamePkWithChild() {
            SortPk p1 = pk(1, "p1");
            SortPkChild c1 = pkChild("c1", p1);
            context.commitChanges();

            context.deleteObjects(p1, c1);
            SortPk p2 = pk(1, "p2");
            SortPkChild c2 = pkChild("c2", p2);

            assertOrder(deleteOf(c1), deleteOf(p1), insertOf(p2), insertOf(c2));
        }

        @Test
        public void changePkAndNullifyFk() {
            SortPk p = pk(1, "p");
            SortPkChild c = pkChild("c", p);
            context.commitChanges();

            c.setParent(null);
            p.setPk(2);

            assertOrder(updateOf(c), updateOf(p));
        }
    }

    @Nested
    public class Flattened {

        @Test
        public void insert() {
            SortMaster m = master("m");
            SortChild c = child("c", null);
            m.addToTaggedChildren(c);

            assertOrder(chain(insertOf(m), insertOf("SORT_TAG")), chain(insertOf(c), insertOf("SORT_TAG")));
        }

        @Test
        public void delete() {
            SortMaster m = master("m");
            SortChild c = child("c", null);
            m.addToTaggedChildren(c);
            context.commitChanges();

            m.removeFromTaggedChildren(c);
            context.deleteObject(c);

            assertOrder(deleteOf("SORT_TAG"), deleteOf(c));
        }

        @Test
        public void attributeInsert() {
            // writing an attribute of the related row inserts the row
            SortFlatChild c = context.newObject(SortFlatChild.class);
            c.setName("c");
            c.setMasterName("m");

            assertOrder(insertOf("SORT_MASTER"), insertOf(c));
        }
    }

    private SortMaster master(String name) {
        SortMaster m = context.newObject(SortMaster.class);
        m.setName(name);
        return m;
    }

    private SortChild child(String name, SortMaster master) {
        SortChild c = context.newObject(SortChild.class);
        c.setName(name);
        c.setMaster(master);
        return c;
    }

    private SortTree tree(String name, SortTree parent) {
        SortTree t = context.newObject(SortTree.class);
        t.setName(name);
        t.setParent(parent);
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

    private static List<Op> chain(Op... ops) {
        return List.of(ops);
    }

    private void assertOrder(Op... ops) {
        assertOrder(chain(ops));
    }

    /**
     * Commits the changes, and checks that the operations of each chain come in the chain order in every permutation
     * the sorter sorted.
     */
    @SafeVarargs
    private void assertOrder(List<Op>... chains) {
        context.commitChanges();

        for (List<DbRowOp> sorted : sortedPermutations()) {
            for (List<Op> chain : chains) {
                assertTrue(inOrder(chain, sorted), () -> chain + " expected, got " + sorted);
            }
        }
    }

    private List<List<DbRowOp>> sortedPermutations() {
        List<List<DbRowOp>> sorted = sorter.lastSorted;
        assertTrue(sorted.size() > 1, "Nothing was sorted");
        return sorted;
    }

    private static boolean inOrder(List<Op> chain, List<DbRowOp> sorted) {
        int previous = -1;
        for (Op op : chain) {
            int i = op.indexIn(sorted);
            if (i < previous) {
                return false;
            }
            previous = i;
        }
        return true;
    }

    private static Op insertOf(Persistent object) {
        return Op.of("insert", InsertDbRowOp.class, object);
    }

    private static Op updateOf(Persistent object) {
        return Op.of("update", UpdateDbRowOp.class, object);
    }

    private static Op deleteOf(Persistent object) {
        return Op.of("delete", DeleteDbRowOp.class, object);
    }

    private static Op insertOf(String table) {
        return Op.of("insert", InsertDbRowOp.class, table);
    }

    private static Op deleteOf(String table) {
        return Op.of("delete", DeleteDbRowOp.class, table);
    }

    /**
     * An operation expected among the sorted ones, on the table of an object, or on a join table.
     */
    private record Op(String label, Predicate<DbRowOp> matcher) {

        static Op of(String type, Class<? extends DbRowOp> opType, Persistent object) {
            String entity = object.getObjectId().getEntityName();
            String table = env.mapping().entity(entity).table().name();
            return new Op(type + " " + entity, op -> opType.isInstance(op)
                    && op.getObject() == object
                    && table.equals(op.getEntity().name()));
        }

        static Op of(String type, Class<? extends DbRowOp> opType, String table) {
            return new Op(type + " " + table, op -> opType.isInstance(op) && table.equals(op.getEntity().name()));
        }

        int indexIn(List<DbRowOp> sorted) {
            int found = -1;
            for (int i = 0; i < sorted.size(); i++) {
                if (matcher.test(sorted.get(i))) {
                    assertEquals(-1, found, () -> "More than one " + label + " in " + sorted);
                    found = i;
                }
            }
            assertTrue(found >= 0, () -> "No " + label + " in " + sorted);
            return found;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * Sorts several permutations of the operations with {@link GraphDbRowOpSorter}, recording each result for the test
     * to check, and returns the result for the operations in the order they came in.
     */
    public static class PermutingSorter implements DbRowOpSorter {

        private static final int SHUFFLES = 4;

        private final DbRowOpSorter delegate;
        private List<List<DbRowOp>> lastSorted = List.of();

        public PermutingSorter(@Inject Provider<DataDomain> dataDomainProvider) {
            this.delegate = new GraphDbRowOpSorter(dataDomainProvider);
        }

        @Override
        public List<DbRowOp> sort(List<DbRowOp> ops) {
            List<List<DbRowOp>> permutations = new ArrayList<>();
            permutations.add(new ArrayList<>(ops));

            List<DbRowOp> reversed = new ArrayList<>(ops);
            Collections.reverse(reversed);
            permutations.add(reversed);

            // a fixed seed, so that a failure can be reproduced
            Random random = new Random(1);
            for (int i = 0; i < SHUFFLES; i++) {
                List<DbRowOp> shuffled = new ArrayList<>(ops);
                Collections.shuffle(shuffled, random);
                permutations.add(shuffled);
            }

            List<List<DbRowOp>> sorted = new ArrayList<>(permutations.size());
            for (List<DbRowOp> permutation : permutations) {
                sorted.add(delegate.sort(permutation));
            }

            this.lastSorted = sorted;
            return sorted.getFirst();
        }
    }
}
