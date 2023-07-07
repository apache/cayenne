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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.ThenNode;

/**
 * @since 5.0
 */
class WhenBuilder implements NodeBuilder {

    NodeBuilder result;
    CaseWhenBuilder builder;

    public WhenBuilder(CaseWhenBuilder caseWhenBuilder) {
        this.builder = caseWhenBuilder;
    }

    CaseWhenBuilder then(NodeBuilder result) {
        this.result = result;
        builder.getNodeBuilders().add(this);
        return builder;
    }
    @Override
    public Node build() {
        if(result == null) {
            throw new CayenneRuntimeException("\"Then\" result must be defined after the \"When\" condition ");
        }
        ThenNode thenNode = new ThenNode();
        thenNode.addChild(result.build());
        return thenNode;
    }
}