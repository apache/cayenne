package org.objectstyle.cayenne.swing;

import java.util.Map;

import ognl.Ognl;
import ognl.OgnlException;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.util.Util;

/**
 * A class for expression evaluation using a designated scripting language (now - OGNL).
 * 
 * @author Andrei Adamchik
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