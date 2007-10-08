/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.swing;

import ognl.Ognl;
import ognl.OgnlException;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.util.Util;
import org.objectstyle.cayenne.validation.ValidationException;

/**
 * @author Andrei Adamchik
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