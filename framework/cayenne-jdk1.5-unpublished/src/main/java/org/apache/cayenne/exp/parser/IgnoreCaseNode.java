/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.exp.parser;

import java.io.PrintWriter;

import org.apache.cayenne.exp.ExpressionException;

/**
 * Common node for likeIgnoreCase and notLikeIgnoreCase
 */
abstract class IgnoreCaseNode extends PatternMatchNode {

    IgnoreCaseNode(int i, boolean ignoringCase) {
        super(i, ignoringCase);
    }

    IgnoreCaseNode(int i, boolean ignoringCase, char escapeChar) {
        super(i, ignoringCase, escapeChar);
    }

    @Override
    protected void encodeChildrenAsEJBQL(PrintWriter pw, String rootId) {
        // with like, first expression is always path, second is a literal, which must be
        // uppercased
        pw.print("upper(");
        ((SimpleNode) children[0]).encodeAsEJBQL(pw, rootId);
        pw.print(") ");
        pw.print(getEJBQLExpressionOperator(0));
        pw.print(" ");

        Object literal = ((ASTScalar) children[1]).getValue();
        if (!(literal instanceof String)) {
            throw new ExpressionException("Literal value should be a string");
        }
        SimpleNode.encodeScalarAsString(pw, ((String) literal).toUpperCase(), '\'');
    }
}
