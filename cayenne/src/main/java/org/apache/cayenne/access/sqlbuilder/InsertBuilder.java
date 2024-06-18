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

package org.apache.cayenne.access.sqlbuilder;

import org.apache.cayenne.access.sqlbuilder.sqltree.InsertColumnsNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.InsertNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.TableNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.InsertValuesNode;
import org.apache.cayenne.map.DbEntity;

/**
 * @since 4.2
 */
public class InsertBuilder extends BaseBuilder {

    /*
    INSERT INTO AUTO_PK_SUPPORT (TABLE_NAME, NEXT_ID) VALUES ('X_AUTHOR', 200)
    */

    private static final int TABLE_NODE   = 0;
    private static final int COLUMNS_NODE = 1;
    private static final int VALUES_NODE  = 2;

    public InsertBuilder(String table) {
        super(new InsertNode(), VALUES_NODE + 1);
        node(TABLE_NODE, () -> new TableNode(table, null));
    }

    public InsertBuilder(DbEntity table) {
        super(new InsertNode(), VALUES_NODE + 1);
        node(TABLE_NODE, () -> new TableNode(table, null));
    }

    public InsertBuilder column(ColumnNodeBuilder columnNode) {
        node(COLUMNS_NODE, InsertColumnsNode::new).addChild(columnNode.build());
        return this;
    }

    public InsertBuilder value(ValueNodeBuilder valueNode) {
        node(VALUES_NODE, InsertValuesNode::new).addChild(valueNode.build());
        return this;
    }
}
