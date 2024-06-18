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

package org.apache.cayenne.access.sqlbuilder.sqltree;


import org.apache.cayenne.access.sqlbuilder.QuotingAppendable;

/**
 * @since 4.2
 */
public class ExpressionNode extends Node {

    public ExpressionNode() {
    }

    public ExpressionNode(NodeType nodeType) {
        super(nodeType);
    }

    @Override
    public QuotingAppendable append(QuotingAppendable buffer) {
        return buffer;
    }

    @Override
    public void appendChildrenStart(QuotingAppendable buffer) {
        if(parent != null
                && parent.type != NodeType.WHERE
                && parent.type != NodeType.JOIN
                && parent.type != NodeType.UPDATE_SET) {
            buffer.append(" (");
        }
    }

    @Override
    public void appendChildrenEnd(QuotingAppendable buffer) {
        if(parent != null
                && parent.type != NodeType.WHERE
                && parent.type != NodeType.JOIN
                && parent.type != NodeType.UPDATE_SET) {
            buffer.append(" )");
        }
    }

    @Override
    public String toString() {
        return "{ExpressionNode}";
    }

    @Override
    public Node copy() {
        return new ExpressionNode();
    }
}
