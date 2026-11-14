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
import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Provider;
import org.apache.cayenne.testdo.sorting.SortChild;
import org.apache.cayenne.testdo.sorting.SortMaster;
import org.apache.cayenne.testdo.sorting.SortTree;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link GraphBasedDbRowOpSorter} reads the committed FKs of the updated and deleted rows, that are queried from the
 * DB when the snapshots are not cached. It skips the reads that can't produce a dependency. The tests make the
 * changes, then empty the snapshot cache, so that each read is a query.
 */
public class GraphBasedDbRowOpSorterIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv
            .forProject(CayenneProjects.SORTING_PROJECT)
            .withExtraModules(b -> b.bind(DbRowOpSorter.class).to(QueryCountingSorter.class));

    private ObjectContext context;
    private QueryCountingSorter sorter;

    @BeforeEach
    public void setUp() {
        context = env.context();
        sorter = (QueryCountingSorter) env.runtime().getInjector().getInstance(DbRowOpSorter.class);
    }

    @AfterEach
    public void cleanUp() throws SQLException {
        // break the reference cycle first, as the tables are cleaned one at a time
        env.table("SORT_TREE", "PARENT_ID").update().set("PARENT_ID", null, Types.INTEGER).execute();
        for (String table : List.of("SORT_CHILD", "SORT_MASTER", "SORT_TREE")) {
            env.table(table).deleteAll();
        }
    }

    @Test
    public void deleteWithoutDeletedParents() {
        SortMaster m = master("m");
        SortChild c1 = child("c1", m);
        SortChild c2 = child("c2", m);
        context.commitChanges();

        context.deleteObjects(c1, c2);

        // no deleted rows the children reference
        assertSortQueries(0);
    }

    @Test
    public void updateWithoutFkChange() {
        SortMaster m = master("m");
        SortChild c = child("c", m);
        SortMaster other = master("other");
        context.commitChanges();

        c.setName("c1");
        context.deleteObject(other);

        // the FK is not changed, so the parent it referenced doesn't matter, even with deletes in its table
        assertSortQueries(0);
    }

    @Test
    public void deleteWithDeletedParent() {
        SortTree root = tree("root", null);
        SortTree child = tree("child", root);
        context.commitChanges();

        context.deleteObjects(root, child);

        // the rows reference the table with deletes, each row read once
        assertSortQueries(2);
    }

    private void assertSortQueries(int expected) {
        env.runtime().getDataDomain().getSharedSnapshotCache().clear();
        context.commitChanges();
        assertEquals(expected, sorter.lastSortQueries);
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

    /**
     * Counts the queries run while sorting, i.e. the snapshots read from the DB.
     */
    public static class QueryCountingSorter extends GraphBasedDbRowOpSorter {

        private int lastSortQueries;

        public QueryCountingSorter(@Inject Provider<DataDomain> dataDomainProvider) {
            super(dataDomainProvider);
        }

        @Override
        public List<DbRowOp> sort(List<DbRowOp> dbRows) {
            List<DbRowOp> sorted = new ArrayList<>(dbRows.size());
            lastSortQueries = env.runWithQueryCounter(() -> sorted.addAll(super.sort(dbRows)));
            return sorted;
        }
    }
}
