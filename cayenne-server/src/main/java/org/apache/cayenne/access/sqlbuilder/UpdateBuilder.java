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

import org.apache.cayenne.access.sqlbuilder.sqltree.UpdateSetNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.TableNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.UpdateNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.WhereNode;
import org.apache.cayenne.map.DbEntity;

/**
 * @since 4.2
 */
public class UpdateBuilder extends BaseBuilder {

    private static final int TABLE_NODE = 0;
    private static final int SET_NODE   = 1;
    private static final int WHERE_NODE = 2;

    public UpdateBuilder(String table) {
        super(new UpdateNode(), WHERE_NODE + 1);
        node(TABLE_NODE, () -> new TableNode(table, null));
    }

    public UpdateBuilder(DbEntity table) {
        super(new UpdateNode(), WHERE_NODE + 1);
        node(TABLE_NODE, () -> new TableNode(table, null));
    }

    public UpdateBuilder set(NodeBuilder setExpression) {
        node(SET_NODE, UpdateSetNode::new).addChild(setExpression.build());
        return this;
    }

    public UpdateBuilder where(NodeBuilder expression) {
        if(expression != null) {
            node(WHERE_NODE, WhereNode::new).addChild(expression.build());
        }
        return this;
    }

}
