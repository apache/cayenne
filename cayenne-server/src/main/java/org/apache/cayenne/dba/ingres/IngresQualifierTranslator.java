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

package org.apache.cayenne.dba.ingres;

import org.apache.cayenne.access.translator.select.QueryAssembler;
import org.apache.cayenne.access.translator.select.TrimmingQualifierTranslator;
import org.apache.cayenne.exp.parser.ASTFunctionCall;

/**
 * @since 4.0
 */
class IngresQualifierTranslator extends TrimmingQualifierTranslator {

    IngresQualifierTranslator(QueryAssembler queryAssembler) {
        super(queryAssembler, IngresAdapter.TRIM_FUNCTION);
    }

    @Override
    protected void appendFunction(ASTFunctionCall functionExpression) {
        if(!"CONCAT".equals(functionExpression.getFunctionName())) {
            super.appendFunction(functionExpression);
        }
    }

    @Override
    protected void appendFunctionArgDivider(ASTFunctionCall functionExpression) {
        if("CONCAT".equals(functionExpression.getFunctionName())) {
            out.append(" + ");
        } else {
            super.appendFunctionArgDivider(functionExpression);
        }
    }

    @Override
    protected void clearLastFunctionArgDivider(ASTFunctionCall functionExpression) {
        if("CONCAT".equals(functionExpression.getFunctionName())) {
            out.delete(out.length() - " + ".length(), out.length());
        } else {
            super.clearLastFunctionArgDivider(functionExpression);
        }
    }
}
