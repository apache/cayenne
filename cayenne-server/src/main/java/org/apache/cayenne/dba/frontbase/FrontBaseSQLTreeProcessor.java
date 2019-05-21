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

package org.apache.cayenne.dba.frontbase;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.sqlbuilder.QuotingAppendable;
import org.apache.cayenne.access.sqlbuilder.sqltree.FunctionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.sqlbuilder.sqltree.OpExpressionNode;
import org.apache.cayenne.access.sqlbuilder.sqltree.TextNode;
import org.apache.cayenne.access.translator.select.BaseSQLTreeProcessor;

/**
 * @since 4.2
 */
public class FrontBaseSQLTreeProcessor extends BaseSQLTreeProcessor {

    @Override
    protected void onFunctionNode(Node parent, FunctionNode child, int index) {
        switch (child.getFunctionName()) {
            case "CONCAT":
                replaceChild(parent, index, new OpExpressionNode("||"));
                break;
            case "LOCATE":
                // POSITION (substr IN str)
                replaceChild(parent, index, new FunctionNode("POSITION", child.getAlias()) {
                    @Override
                    public void appendChildrenSeparator(QuotingAppendable buffer, int childIdx) {
                        buffer.append(" IN ");
                    }
                });
                break;
            case "LENGTH":
                replaceChild(parent, index, new FunctionNode("CHAR_LENGTH", child.getAlias()));
                break;
            case "SUBSTRING":
                // SUBSTRING (str FROM offset FOR length)
                replaceChild(parent, index, new FunctionNode("SUBSTRING", child.getAlias()){
                    @Override
                    public void appendChildrenSeparator(QuotingAppendable buffer, int childIdx) {
                        if(childIdx == 0) {
                            buffer.append(" FROM ");
                        } else if(childIdx == 1) {
                            buffer.append(" FOR ");
                        }
                    }
                });
                break;
            case "YEAR":
            case "MONTH":
            case "DAY":
            case "DAY_OF_MONTH":
            case "HOUR":
            case "MINUTE":
            case "SECOND":
                Node functionReplacement = new ExtractFunctionNode(child.getAlias());
                String functionName = child.getFunctionName();
                if("DAY_OF_MONTH".equals(functionName)) {
                    functionName = "DAY";
                }
                functionReplacement.addChild(new TextNode(functionName));
                replaceChild(parent, index, functionReplacement);
                break;

            case "DAY_OF_WEEK":
            case "DAY_OF_YEAR":
            case "WEEK":
                throw new CayenneRuntimeException("Function %s() is unsupported in FrontBase.", child.getFunctionName());
        }
    }

    private static class ExtractFunctionNode extends FunctionNode {
        public ExtractFunctionNode(String alias) {
            super("EXTRACT", alias, true);
        }

        @Override
        public void appendChildrenSeparator(QuotingAppendable buffer, int childIdx) {
            buffer.append(" FROM ");
        }

        @Override
        public Node copy() {
            return new ExtractFunctionNode(getAlias());
        }
    }
}
