package de.jexp.jequel.expression;

public interface StringExpressions extends Expression {

    BooleanExpression like(StringExpressions expression);

    BooleanExpression likeIgnoreCase(StringExpressions expression);

}
