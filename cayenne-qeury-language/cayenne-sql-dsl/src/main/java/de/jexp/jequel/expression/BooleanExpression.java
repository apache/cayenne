package de.jexp.jequel.expression;

public interface BooleanExpression extends Expression {
    BooleanExpression and(BooleanExpression expression);

    BooleanExpression or(BooleanExpression expression);
}
