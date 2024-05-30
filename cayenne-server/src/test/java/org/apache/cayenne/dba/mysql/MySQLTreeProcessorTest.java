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

package org.apache.cayenne.dba.mysql;

import org.apache.cayenne.access.sqlbuilder.BaseSqlBuilderTest;
import org.apache.cayenne.access.sqlbuilder.sqltree.LikeNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.junit.Before;
import org.junit.Test;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.*;

public class MySQLTreeProcessorTest extends BaseSqlBuilderTest {

    private Node sqlNode;

    @Before
    public void generateSql() {
        sqlNode = select(column("*"))
                .from(table("test"))
                .where(() -> {
                    Node node = new LikeNode(false, false, (char) 0);
                    node.addChild(column("column").build());
                    node.addChild(value("abc").build());
                    return node;
                })
                .build();
        assertSQL("SELECT * FROM test WHERE column LIKE 'abc'", sqlNode);
    }

    @Test
    public void testLikeCI() {
        MySQLTreeProcessor instance = MySQLTreeProcessor.getInstance(true);
        Node processed = instance.process(sqlNode);
        assertSQL("SELECT * FROM test WHERE column LIKE BINARY 'abc'", processed);
    }

    @Test
    public void testLikeCS() {
        MySQLTreeProcessor instance = MySQLTreeProcessor.getInstance(false);
        Node processed = instance.process(sqlNode);
        assertSQL("SELECT * FROM test WHERE column LIKE 'abc'", processed);
    }
}