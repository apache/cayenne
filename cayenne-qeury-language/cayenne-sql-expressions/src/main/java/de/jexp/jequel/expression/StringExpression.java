package de.jexp.jequel.expression;

public interface StringExpression extends Expression {

    BooleanExpression like(StringExpression expression);

    BooleanExpression likeIgnoreCase(StringExpression expression);

}
