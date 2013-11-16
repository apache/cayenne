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

package org.apache.cayenne.exp;

public class TstUnaryExpSuite extends TstExpressionSuite {
    
    private static final TstExpressionCase negative1 = buildNegative1();
    private static final TstExpressionCase negative2 = buildNegative2();
    private static final TstExpressionCase negative3 = buildNegative3();
    
    /** Cayenne syntax: "-5" */
    private static TstExpressionCase buildNegative1() {           
        Expression e1 = ExpressionFactory.expressionOfType(Expression.NEGATIVE);
        e1.setOperand(0, new Integer(5));
        return new TstExpressionCase("Painting",
        e1, 
        "-?",
        1, 1
        );
    }
    
    /** Cayenne syntax: "-estimatedPrice" */
    private static TstExpressionCase buildNegative2() {           
        Expression e1 = ExpressionFactory.expressionOfType(Expression.NEGATIVE);
        Expression e10 = ExpressionFactory.expressionOfType(Expression.OBJ_PATH);
        e10.setOperand(0, "estimatedPrice");
        e1.setOperand(0, e10);
        return new TstExpressionCase("Painting",
        e1, 
        "-ta.ESTIMATED_PRICE",
        2, 1
        );
    }
    
    /** Cayenne syntax: "-toGallery.paintingArray.estimatedPrice" */
    private static TstExpressionCase buildNegative3() {           
        Expression e1 = ExpressionFactory.expressionOfType(Expression.NEGATIVE);
        Expression e10 = ExpressionFactory.expressionOfType(Expression.OBJ_PATH);
        e10.setOperand(0, "toGallery.paintingArray.estimatedPrice");
        e1.setOperand(0, e10);
        return new TstExpressionCase("Exhibit",
        e1, 
        "-ta.ESTIMATED_PRICE",
        2, 1
        );
    }
    
    
    public TstUnaryExpSuite() {
        addCase(negative1);
        addCase(negative2);
        addCase(negative3);
    }
}
