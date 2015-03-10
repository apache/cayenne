package de.jexp.jequel.expression;


import de.jexp.jequel.expression.logical.BooleanExpression;

public interface ListOperations {
    BooleanExpression in(Object... expressions);
}
