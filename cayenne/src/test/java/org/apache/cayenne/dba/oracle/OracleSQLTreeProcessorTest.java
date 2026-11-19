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

package org.apache.cayenne.dba.oracle;

import org.apache.cayenne.access.sqlbuilder.sqltree.InNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.ValueNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.column;
import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.select;
import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.table;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class OracleSQLTreeProcessorTest {

    @Test
    public void inListIsChunked() {
        Node processed = new OracleSQLTreeProcessor().process(selectIn(new Object[2500], true));

        List<Integer> sizes = inValues(processed).stream().map(v -> ((Object[]) v).length).toList();
        assertEquals(List.of(1000, 1000, 500), sizes);
    }

    @Test
    public void byteArrayParameterIsNotChunked() {
        byte[] key = new byte[2500];
        Node processed = new OracleSQLTreeProcessor().process(selectIn(key, false));

        List<Object> values = inValues(processed);
        assertEquals(1, values.size());
        assertSame(key, values.get(0));
    }

    private static Node selectIn(Object value, boolean isArray) {
        return select(column("*"))
                .from(table("t"))
                .where(() -> {
                    Node in = new InNode(false);
                    in.addChild(column("c").build());
                    in.addChild(new ValueNode(value, isArray, null, true));
                    return in;
                })
                .build();
    }

    private static List<Object> inValues(Node node) {
        List<Object> values = new ArrayList<>();
        collectInValues(node, values);
        return values;
    }

    private static void collectInValues(Node node, List<Object> values) {
        if (node instanceof InNode) {
            values.add(((ValueNode) node.getChild(1)).getValue());
        }
        for (int i = 0; i < node.getChildrenCount(); i++) {
            collectInValues(node.getChild(i), values);
        }
    }
}
