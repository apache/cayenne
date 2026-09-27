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
package org.apache.cayenne.ql;

import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.query.Ordering;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.query.SortOrder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class QLSelectPrinterTest {

    @Test
    public void printFromOnly() {
        assertEquals("from Artist", QLSelectPrinter.print(ObjectSelect.query(Object.class, "Artist")));
    }

    @Test
    public void printAllClauses() {
        ObjectSelect<?> query = ObjectSelect.query(Object.class, "Artist")
                .where(ExpressionFactory.exp("artistName like $name"))
                .orderBy(new Ordering("artistName", SortOrder.DESCENDING_INSENSITIVE))
                .orderBy(new Ordering("dateOfBirth", SortOrder.ASCENDING))
                .limit(10)
                .offset(20)
                .prefetch("paintings", PrefetchTreeNode.JOINT_PREFETCH_SEMANTICS)
                .prefetch("paintings.gallery", PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS)
                .prefetch("exhibits", PrefetchTreeNode.DISJOINT_BY_ID_PREFETCH_SEMANTICS)
                .prefetch("groups", PrefetchTreeNode.UNDEFINED_SEMANTICS)
                .distinct();

        assertEquals("select distinct self from Artist where artistName like $name "
                        + "order by artistName desc insensitive, dateOfBirth limit 10 offset 20 "
                        + "prefetch paintings joint, paintings.gallery disjoint, exhibits disjointById, groups",
                QLSelectPrinter.print(query));
    }

    @Test
    public void printOffsetWithoutLimit() {
        assertEquals("from Artist offset 20",
                QLSelectPrinter.print(ObjectSelect.query(Object.class, "Artist").offset(20)));
    }

    @Test
    public void printPrefetchParents() {
        // a prefetch of a nested path implies its parents, that are phantom nodes and are not printed
        ObjectSelect<?> query = ObjectSelect.query(Object.class, "Artist")
                .prefetch("paintings.gallery", PrefetchTreeNode.DISJOINT_PREFETCH_SEMANTICS);
        assertEquals("from Artist prefetch paintings.gallery disjoint", QLSelectPrinter.print(query));
    }
}
