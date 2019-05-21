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

/**
 * @since 4.2
 */
interface ExpressionTrait extends NodeBuilder {

    default ExpressionNodeBuilder lt(NodeBuilder operand) {
        return new ExpressionNodeBuilder(this).lt(operand);
    }

    default ExpressionNodeBuilder gt(NodeBuilder operand) {
        return new ExpressionNodeBuilder(this).gt(operand);
    }

    default ExpressionNodeBuilder lte(NodeBuilder operand) {
        return new ExpressionNodeBuilder(this).lte(operand);
    }

    default ExpressionNodeBuilder gte(NodeBuilder operand) {
        return new ExpressionNodeBuilder(this).gte(operand);
    }

    default ExpressionNodeBuilder eq(NodeBuilder nodeBuilder) {
        return new ExpressionNodeBuilder(this).eq(nodeBuilder);
    }

    default ExpressionNodeBuilder plus(NodeBuilder nodeBuilder) {
        return new ExpressionNodeBuilder(this).plus(nodeBuilder);
    }

    default ExpressionNodeBuilder minus(NodeBuilder nodeBuilder) {
        return new ExpressionNodeBuilder(this).minus(nodeBuilder);
    }

    default ExpressionNodeBuilder mul(NodeBuilder nodeBuilder) {
        return new ExpressionNodeBuilder(this).mul(nodeBuilder);
    }

    default ExpressionNodeBuilder div(NodeBuilder nodeBuilder) {
        return new ExpressionNodeBuilder(this).div(nodeBuilder);
    }
}
