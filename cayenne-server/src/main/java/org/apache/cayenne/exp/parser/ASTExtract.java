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

package org.apache.cayenne.exp.parser;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.exp.Expression;

public class ASTExtract extends ASTFunctionCall {

    /**
     * Available components of date/time.
     * Names must be in sync with tokens used in dateTimeExtractingFunction() rule in ExpressionParser.jjt
     */
    public enum DateTimePart {
        YEAR, MONTH, WEEK,
        // day options, day is synonym for dayOfMonth
        DAY_OF_YEAR, DAY, DAY_OF_MONTH, DAY_OF_WEEK,
        HOUR, MINUTE, SECOND
    }

    /**
     * Map from camelCase name to enum elements.
     * @see ASTFunctionCall#nameToCamelCase(String)
     */
    private static final Map<String, DateTimePart> NAME_TO_PART = new HashMap<>();
    static {
        for(DateTimePart part : DateTimePart.values()) {
            NAME_TO_PART.put(nameToCamelCase(part.name()), part);
        }
    }

    /**
     * camelCase name, found in ExpressionParser.jjt tokens
     */
    private String partName;

    private DateTimePart part;

    ASTExtract(int id) {
        super(id, "EXTRACT");
    }

    public ASTExtract(Expression expression) {
        super(ExpressionParserTreeConstants.JJTEXTRACT, "EXTRACT", expression);
    }

    @Override
    public String getFunctionName() {
        return part.name();
    }

    @Override
    protected void appendFunctionNameAsString(Appendable out) throws IOException {
        out.append(partName);
    }

    /**
     * This method is used by {@link ExpressionParser}
     * @param partToken {@link Token#image} from {@link ExpressionParser}
     */
    void setPartToken(String partToken) {
        part = NAME_TO_PART.get(partToken);
        if(part == null) {
            throw new CayenneRuntimeException("Unknown timestamp part: %s", partToken);
        }
        this.partName = partToken;
    }

    /**
     * This method is used by FunctionExpressionFactory
     * @param part date/time part to extract
     */
    public void setPart(DateTimePart part) {
        this.part = part;
        this.partName = nameToCamelCase(part.name());
    }

    public DateTimePart getPart() {
        return part;
    }

    public String getPartCamelCaseName() {
        return partName;
    }

    @Override
    public Expression shallowCopy() {
        ASTExtract copy = new ASTExtract(id);
        copy.partName = partName;
        copy.part = part;
        return copy;
    }

    @Override
    protected int getRequiredChildrenCount() {
        return 1;
    }

    @Override
    protected Object evaluateSubNode(Object o, Object[] evaluatedChildren) throws Exception {
        return null;
    }

}
