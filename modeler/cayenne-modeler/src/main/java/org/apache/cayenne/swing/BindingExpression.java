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


package org.apache.cayenne.swing;

import java.util.Map;

import ognl.Ognl;
import ognl.OgnlException;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.util.Util;

/**
 * A class for expression evaluation using a designated scripting language (now - OGNL).
 * 
 */
public class BindingExpression {

    private Object compiled;
    protected String expression;

    static Throwable unwind(Throwable th) {
        if (th instanceof OgnlException) {
            Throwable reason = ((OgnlException) th).getReason();
            return (reason != null) ? unwind(reason) : th;
        }
        else {
            return Util.unwindException(th);
        }
    }

    public BindingExpression(String expression) {
        try {
            this.compiled = Ognl.parseExpression(expression);
        }
        catch (OgnlException ex) {
            throw new CayenneRuntimeException(
                    "Invalid expression - " + expression,
                    BindingBase.unwind(ex));
        }

        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }

    public void setValue(Object context, Map contextVariables, Object value) {
        if (context == null) {
            throw new BindingException("No context");
        }

        try {
            Ognl.setValue(compiled, contextVariables, context, value);
        }
        catch (OgnlException ex) {
            throw new BindingException(
                    "Evaluation failed in context: " + context,
                    unwind(ex));
        }
    }

    public Object getValue(Object context, Map contextVariables) {
        if (context == null) {
            throw new BindingException("No context");
        }

        try {
            return Ognl.getValue(compiled, contextVariables, context);
        }
        catch (OgnlException ex) {
            throw new BindingException(
                    "Evaluation failed in context: " + context,
                    unwind(ex));
        }
    }
}
