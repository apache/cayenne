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

import ognl.Ognl;
import ognl.OgnlException;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.validation.ValidationException;

/**
 */
// TODO: extend BindingExpression, unless we decide to make it a composition...
public abstract class BindingBase implements ObjectBinding {

    private Object compiled;

    protected Object context;
    protected String expression;

    protected BindingDelegate delegate;
    protected boolean modelUpdateDisabled;

    protected boolean usingNullForEmptyStrings;
    protected boolean checkingForValueChange;

    static Throwable unwind(Throwable th) {
        if (th instanceof OgnlException) {
            Throwable reason = ((OgnlException) th).getReason();
            return (reason != null) ? unwind(reason) : th;
        }
        else {
            return Util.unwindException(th);
        }
    }

    public BindingBase(String propertyExpression) {

        try {
            this.compiled = Ognl.parseExpression(propertyExpression);
        }
        catch (OgnlException ex) {
            throw new CayenneRuntimeException("Invalid expression - "
                    + propertyExpression, BindingBase.unwind(ex));
        }

        this.expression = propertyExpression;
    }

    public Object getContext() {
        return context;
    }

    public void setContext(Object object) {
        this.context = object;
    }

    public boolean isCheckingForValueChange() {
        return checkingForValueChange;
    }

    public void setCheckingForValueChange(boolean checkingForValueChange) {
        this.checkingForValueChange = checkingForValueChange;
    }

    public boolean isUsingNullForEmptyStrings() {
        return usingNullForEmptyStrings;
    }

    public void setUsingNullForEmptyStrings(boolean b) {
        this.usingNullForEmptyStrings = b;
    }

    public String getExpression() {
        return expression;
    }

    public BindingDelegate getDelegate() {
        return delegate;
    }

    public void setDelegate(BindingDelegate delegate) {
        this.delegate = delegate;
    }

    /**
     * Pushes a new value to the bound context. If binding delegate is set, notifies it
     * delegate about the update.
     */
    public void setValue(Object value) {
        if (context == null) {
            throw new BindingException("No context");
        }

        try {
            // prevent the same thread from calling setValue recursively - something that
            // may happen due to event loops.

            if (modelUpdateDisabled) {
                return;
            }

            Object oldValue = null;

            modelUpdateDisabled = true;
            try {

                if (delegate != null) {
                    // find old value
                    oldValue = getValue();
                }

                if (isUsingNullForEmptyStrings() && "".equals(value)) {
                    value = null;
                }

                if (isCheckingForValueChange()) {
                    // avoid calling getValue() twice...

                    Object existingValue = (delegate != null) ? oldValue : getValue();
                    if (Util.nullSafeEquals(value, existingValue)) {
                        return;
                    }
                }

                Ognl.setValue(compiled, context, value);
            }
            finally {
                modelUpdateDisabled = false;
            }

            if (delegate != null) {
                delegate.modelUpdated(this, oldValue, value);
            }
        }
        catch (OgnlException ex) {
            processException(ex);
        }
    }

    /**
     * Pulls bound value from the context.
     */
    public Object getValue() {
        if (context == null) {
            throw new BindingException("No context");
        }

        try {
            return Ognl.getValue(compiled, context);
        }
        catch (OgnlException ex) {
            processException(ex);
            return null;
        }
    }

    protected void processException(Throwable th) throws ValidationException,
            BindingException {
        Throwable root = BindingBase.unwind(th);
        if (root instanceof ValidationException) {
            throw (ValidationException) root;
        }
        else if (root instanceof NumberFormatException) {
            throw new ValidationException("Invalid numeric string");
        }

        throw new BindingException("Evaluation failed in context: " + context, root);
    }

}
