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

public class TstTernaryExpSuite extends TstExpressionSuite {
    
    private static final TstExpressionCase between1 = buildBetween1();
    
    
    /** Cayenne syntax: "toGallery.paintingArray.estimatedPrice between 3000 and 15000" */
    private static TstExpressionCase buildBetween1() {           
        Expression e1 = ExpressionFactory.expressionOfType(Expression.BETWEEN);
        Expression e10 = ExpressionFactory.expressionOfType(Expression.OBJ_PATH);
        e10.setOperand(0, "toGallery.paintingArray.estimatedPrice");
        e1.setOperand(0, e10);
        e1.setOperand(1, new Integer(3000));
        e1.setOperand(2, new Integer(15000));
        return new TstExpressionCase("Exhibit",
        e1, 
        "ta.ESTIMATED_PRICE BETWEEN ? AND ?",
        2, 3);
    }
    
    
    public TstTernaryExpSuite() {
        addCase(between1);
    }
}
