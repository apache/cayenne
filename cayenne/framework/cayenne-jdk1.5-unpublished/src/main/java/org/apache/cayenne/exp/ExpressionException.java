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

import org.apache.cayenne.CayenneRuntimeException;

/** 
 * RuntimeException subclass thrown in cases of errors during 
 * expressions creation/parsing.
 * 
 */
public class ExpressionException extends CayenneRuntimeException {
    protected String expressionString;

    /**
     * Constructor for ExpressionException.
     */
    public ExpressionException() {
        super();
    }

    /**
     * Constructor for ExpressionException.
     * @param msg
     */
    public ExpressionException(String msg) {
        super(msg);
    }

    /**
     * Constructor for ExpressionException.
     * @param th
     */
    public ExpressionException(Throwable th) {
        super(th);
    }

    /**
     * Constructor for ExpressionException.
     * @param msg
     * @param th
     */
    public ExpressionException(String msg, Throwable th) {
        super(msg, th);
    }

    /**
     * Constructor for ExpressionException.
     * 
     * @since 1.1
     */
    public ExpressionException(String msg, String expressionString, Throwable th) {
        super(msg, th);
        this.expressionString = expressionString;
    }

    public String getExpressionString() {
        return expressionString;
    }
}
