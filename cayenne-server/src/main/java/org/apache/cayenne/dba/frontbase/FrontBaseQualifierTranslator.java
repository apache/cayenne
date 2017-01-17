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

package org.apache.cayenne.dba.frontbase;

import org.apache.cayenne.access.translator.select.QualifierTranslator;
import org.apache.cayenne.access.translator.select.QueryAssembler;
import org.apache.cayenne.exp.parser.ASTFunctionCall;

/**
 * @since 4.0
 */
public class FrontBaseQualifierTranslator extends QualifierTranslator {

    private int substringArg = 0;

    public FrontBaseQualifierTranslator(QueryAssembler queryAssembler) {
        super(queryAssembler);
    }

    @Override
    protected void appendFunction(ASTFunctionCall functionExpression) {
        switch (functionExpression.getFunctionName()) {
            case "CONCAT":
                // noop
                break;
            case "LOCATE":
                out.append("POSITION");
                break;
            case "LENGTH":
                out.append("CHAR_LENGTH");
                break;
            case "SUBSTRING":
                substringArg = 0;
            default:
                super.appendFunction(functionExpression);
        }
    }

    @Override
    protected void appendFunctionArgDivider(ASTFunctionCall functionExpression) {
        switch (functionExpression.getFunctionName()) {
            case "CONCAT":
                out.append(" || ");
                break;
            case "LOCATE":
                out.append(" IN ");
                break;
            case "SUBSTRING":
                // SUBSTRING (str FROM offset FOR length)
                switch (substringArg++) {
                    case 0:
                        out.append(" FROM ");
                        break;
                    case 1:
                        out.append(" FOR ");
                        break;
                }
                break;
            default:
                super.appendFunctionArgDivider(functionExpression);
        }
    }

    @Override
    protected void clearLastFunctionArgDivider(ASTFunctionCall functionExpression) {
        switch (functionExpression.getFunctionName()) {
            case "CONCAT":
                out.delete(out.length() - " || ".length(), out.length());
                break;
            case "LOCATE":
                out.delete(out.length() - " IN ".length(), out.length());
                break;
            case "SUBSTRING":
                // no offset arg
                if(substringArg == 2) {
                    out.delete(out.length() - " FOR ".length(), out.length());
                }
                break;
            default:
                super.clearLastFunctionArgDivider(functionExpression);
        }
    }
}
