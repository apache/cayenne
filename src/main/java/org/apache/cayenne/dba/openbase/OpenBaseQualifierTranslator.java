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

package org.apache.cayenne.dba.openbase;

import org.apache.cayenne.access.trans.QualifierTranslator;
import org.apache.cayenne.access.trans.QueryAssembler;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.map.DbAttribute;

/** 
 * Translates query qualifier to SQL. Used as a helper class by query translators.
 * 
 * @author <a href="mailto:mkienenb@alaska.net">Mike Kienenberger</a>
 * @author Andrus Adamchik
 * 
 * @since 1.1
 */
public class OpenBaseQualifierTranslator extends QualifierTranslator {

    public OpenBaseQualifierTranslator() {
        this(null);
    }

    public OpenBaseQualifierTranslator(QueryAssembler queryAssembler) {
        super(queryAssembler);
    }

    @Override
    public void startNode(Expression node, Expression parentNode) {

        if (node.getOperandCount() == 2) {
            // binary nodes are the only ones that currently require this
            detectObjectMatch(node);

            if (parenthesisNeeded(node, parentNode)) {
                qualBuf.append('(');
            }

            // super implementation has special handling 
            // of LIKE_IGNORE_CASE and NOT_LIKE_IGNORE_CASE
            // OpenBase is case-insensitive by default
            // ...
        }
        else {
            super.startNode(node, parentNode);
        }
    }

    @Override
    public void endNode(Expression node, Expression parentNode) {
        if (node.getOperandCount() == 2) {
            // check if we need to use objectMatchTranslator to finish building the expression
            if (matchingObject) {
                appendObjectMatch();
            }

            if (parenthesisNeeded(node, parentNode))
                qualBuf.append(')');

            // super implementation has special handling 
            // of LIKE_IGNORE_CASE and NOT_LIKE_IGNORE_CASE
            // OpenBase is case-insensitive by default
            // ...
        }
        else {
            super.endNode(node, parentNode);
        }
    }

    @Override
    protected void appendLiteralDirect(
        StringBuffer buf,
        Object val,
        DbAttribute attr,
        Expression parentExpression) {

        // Special handling of string matching is needed:
        // Case-sensitive LIKE must be converted to [x][Y][z] format
        if (val instanceof String
            && (parentExpression.getType() == Expression.LIKE
                || parentExpression.getType() == Expression.NOT_LIKE)) {

            val = caseSensitiveLikePattern((String) val);
        }

        super.appendLiteralDirect(buf, val, attr, parentExpression);
    }

    private String caseSensitiveLikePattern(String pattern) {
        int len = pattern.length();
        StringBuffer buffer = new StringBuffer(len * 3);

        for (int i = 0; i < len; i++) {
            char c = pattern.charAt(i);
            if (c == '%' || c == '?') {
                buffer.append(c);
            }
            else {
                buffer.append("[").append(c).append("]");
            }
        }

        return buffer.toString();
    }

    @Override
    public void finishedChild(Expression node, int childIndex, boolean hasMoreChildren) {
        if (!hasMoreChildren) {
            return;
        }

        // super implementation has special handling 
        // of LIKE_IGNORE_CASE and NOT_LIKE_IGNORE_CASE
        // OpenBase is case-insensitive by default
        // ...

        switch (node.getType()) {

            case Expression.LIKE_IGNORE_CASE :
                finishedChildNodeAppendExpression(node, " LIKE ");
                break;
            case Expression.NOT_LIKE_IGNORE_CASE :
                finishedChildNodeAppendExpression(node, " NOT LIKE ");
                break;
            default :
                super.finishedChild(node, childIndex, hasMoreChildren);
        }
    }

    private void finishedChildNodeAppendExpression(Expression node, String operation) {
        StringBuffer buf = (matchingObject) ? new StringBuffer() : qualBuf;
        buf.append(operation);
        if (matchingObject) {
            objectMatchTranslator.setOperation(buf.toString());
            objectMatchTranslator.setExpression(node);
        }
    }
}
