package org.objectstyle.cayenne.swing;

import java.util.Collections;

/**
 * An implementation of BindingDelegate that invokes a no-argument context action on every
 * model update.
 * 
 * @author Andrei Adamchik
 */
public class ActionDelegate implements BindingDelegate {

    protected BindingExpression expression;

    public ActionDelegate(String expression) {
        this.expression = new BindingExpression(expression);
    }

    public void modelUpdated(ObjectBinding binding, Object oldValue, Object newValue) {
        // TODO: might add new and old value as variables...
        expression.getValue(binding.getContext(), Collections.EMPTY_MAP);
    }
}