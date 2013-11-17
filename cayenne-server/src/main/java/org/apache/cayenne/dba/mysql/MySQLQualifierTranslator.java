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
package org.apache.cayenne.dba.mysql;

import java.io.IOException;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.trans.QualifierTranslator;
import org.apache.cayenne.access.trans.QueryAssembler;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.parser.PatternMatchNode;

class MySQLQualifierTranslator extends QualifierTranslator {

    public MySQLQualifierTranslator(QueryAssembler queryAssembler) {
        super(queryAssembler);
    }

    @Override
    protected void appendLikeEscapeCharacter(PatternMatchNode patternMatchNode)
            throws IOException {

        char escapeChar = patternMatchNode.getEscapeChar();

        if ('?' == escapeChar) {
            throw new CayenneRuntimeException(
                    "the escape character of '?' is illegal for LIKE clauses.");
        }

        if (0 != escapeChar) {
            // this is a difference with super implementation - MySQL driver does not
            // support JDBC escape syntax, so creating an explicit SQL escape:
            out.append(" ESCAPE '");
            out.append(escapeChar);
            out.append("'");
        }
    }

    @Override
    public void finishedChild(Expression node, int childIndex, boolean hasMoreChildren) {
        
        if (!caseInsensitive) {
            super.finishedChild(node, childIndex, hasMoreChildren);
        }
        else {
            
            if (!hasMoreChildren) {
                return;
            }
            
            // if we have something except LIKE or NOT LIKE then no need in specific handling
            if (node.getType() != Expression.LIKE
                    && node.getType() != Expression.NOT_LIKE) {
                super.finishedChild(node, childIndex, hasMoreChildren);
                return;
            }
            
            try {
                Appendable out = (matchingObject) ? new StringBuilder() : this.out;
                
                switch (node.getType()) {
                    case Expression.LIKE:
                        out.append(" LIKE BINARY ");
                        break;
                    case Expression.NOT_LIKE:
                        out.append(" NOT LIKE BINARY ");
                        break;
                }
            }
            catch (IOException ioex) {
                throw new CayenneRuntimeException("Error appending content", ioex);
            }
        }
    }
}
