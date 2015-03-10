package de.jexp.jequel.expression;

import de.jexp.jequel.expression.logical.BooleanExpression;

public interface StringOperations {
    BooleanExpression like(Object expression);
}
