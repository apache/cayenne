package de.jexp.jequel.expression;

public interface NumericOperations {
    BooleanExpression eq(Object expression);

    BooleanExpression ge(Object expression);

    BooleanExpression gt(Object expression);

    BooleanExpression lt(Object expression);

    BooleanExpression le(Object expression);

    BooleanExpression ne(Object expression);

    BooleanExpression between(Object start, Object end);

    NumericBinaryExpression plus(Object expression);

    NumericBinaryExpression minus(Object expression);

    NumericBinaryExpression times(Object expression);

    NumericBinaryExpression by(Object expression);
}
